package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseInitializer
import com.example.data.database.FoodEntity
import com.example.data.database.FoodLogEntity
import com.example.data.database.NutritionGoalEntity
import com.example.data.database.WaterLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Result of testing an API key against a provider's endpoint.
 */
sealed class ApiKeyTestResult {
    object Success : ApiKeyTestResult()
    data class Failure(val error: String) : ApiKeyTestResult()
}

data class DailyNutritionSummary(
    val dateString: String,
    val totalCalories: Double = 0.0,
    val caloriesGoal: Double = 2200.0,
    val totalProtein: Double = 0.0,
    val proteinGoal: Double = 140.0,
    val totalCarbs: Double = 0.0,
    val carbsGoal: Double = 220.0,
    val totalFat: Double = 0.0,
    val fatGoal: Double = 65.0,
    val totalWaterMl: Int = 0,
    val waterGoalMl: Int = 3000,
    val foodLogs: List<FoodLogEntity> = emptyList(),
    val waterLogs: List<WaterLogEntity> = emptyList()
) {
    val caloriesRemaining: Double
        get() = (caloriesGoal - totalCalories).coerceAtLeast(0.0)

    val proteinRemaining: Double
        get() = (proteinGoal - totalProtein).coerceAtLeast(0.0)

    val carbsRemaining: Double
        get() = (carbsGoal - totalCarbs).coerceAtLeast(0.0)

    val fatRemaining: Double
        get() = (fatGoal - totalFat).coerceAtLeast(0.0)

    val waterRemainingMl: Int
        get() = (waterGoalMl - totalWaterMl).coerceAtLeast(0)

    val caloriesProgress: Float
        get() = if (caloriesGoal > 0) (totalCalories / caloriesGoal).toFloat().coerceIn(0f, 1f) else 0f

    val proteinProgress: Float
        get() = if (proteinGoal > 0) (totalProtein / proteinGoal).toFloat().coerceIn(0f, 1f) else 0f

    val carbsProgress: Float
        get() = if (carbsGoal > 0) (totalCarbs / carbsGoal).toFloat().coerceIn(0f, 1f) else 0f

    val fatProgress: Float
        get() = if (fatGoal > 0) (totalFat / fatGoal).toFloat().coerceIn(0f, 1f) else 0f

    val waterProgress: Float
        get() = if (waterGoalMl > 0) (totalWaterMl.toFloat() / waterGoalMl.toFloat()).coerceIn(0f, 1f) else 0f
}

data class DayTrendPoint(
    val dateString: String,
    val displayLabel: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val waterMl: Int
)

class NutritionRepository(private val database: AppDatabase) {

    private val foodDao = database.foodDao()
    private val foodLogDao = database.foodLogDao()
    private val waterLogDao = database.waterLogDao()
    private val goalDao = database.nutritionGoalDao()
    private val appSettingsDao = database.appSettingsDao()
    private val weightLogDao = database.weightLogDao()

    val weightLogsAscFlow: Flow<List<com.example.data.database.WeightLogEntity>> = weightLogDao.getAllWeightLogsAscFlow()
    val weightLogsDescFlow: Flow<List<com.example.data.database.WeightLogEntity>> = weightLogDao.getAllWeightLogsDescFlow()

    suspend fun logWeight(date: LocalDate, weightKg: Double, note: String = ""): Long {
        val entry = com.example.data.database.WeightLogEntity(
            dateEpochDay = date.toEpochDay(),
            weightKg = Math.round(weightKg * 100.0) / 100.0,
            note = note,
            loggedAtTimestamp = System.currentTimeMillis()
        )
        weightLogDao.insertOrUpdate(entry)
        // After inserting, find the most recent weight by date (not by entry time)
        // so that saving an older-dated weight doesn't overwrite today's current weight
        val latestByDate = weightLogDao.getLatestWeightLog()
        val currentWeight = if (latestByDate != null) latestByDate.weightKg else weightKg
        val currentGoals = getGoals()
        if (currentGoals != null) {
            updateGoals(currentGoals.copy(currentWeightKg = currentWeight))
        }
        return entry.id
    }

    suspend fun deleteWeightLog(log: com.example.data.database.WeightLogEntity) {
        weightLogDao.deleteWeightLog(log)
    }

    suspend fun getWeightLogForDate(date: LocalDate): com.example.data.database.WeightLogEntity? {
        return weightLogDao.getWeightLogForDate(date.toEpochDay())
    }

    suspend fun getLatestWeight(): com.example.data.database.WeightLogEntity? {
        return weightLogDao.getLatestWeightLog()
    }

    val settingsFlow: Flow<com.example.data.database.AppSettingsEntity?> = appSettingsDao.getSettingsFlow()

    suspend fun getSettings(): com.example.data.database.AppSettingsEntity {
        return appSettingsDao.getSettings() ?: com.example.data.database.AppSettingsEntity()
    }

    suspend fun saveSettings(settings: com.example.data.database.AppSettingsEntity) {
        appSettingsDao.insertOrUpdate(settings)
    }

    // -----------------------------------------------------------------------
    // API Key Validation — test a key against the provider before saving it
    // -----------------------------------------------------------------------
    private val testClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun testApiKey(
        provider: String,
        apiKey: String,
        model: String,
        baseUrl: String = ""
    ): ApiKeyTestResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            ApiKeyTestResult.Failure("API key is empty or is a placeholder")
        } else {
            try {
                when (provider) {
                    "GEMINI" -> testGeminiKey(apiKey, model)
                    "NVIDIA_NIM", "OPENAI" -> testOpenAICompatibleKey(apiKey, baseUrl, model)
                    "LOCAL" -> ApiKeyTestResult.Success
                    else -> ApiKeyTestResult.Failure("Unknown provider: $provider")
                }
            } catch (e: UnknownHostException) {
                ApiKeyTestResult.Failure("No internet connection — cannot reach the API server.")
            } catch (e: SocketTimeoutException) {
                ApiKeyTestResult.Failure("Request timed out — check your internet connection.")
            } catch (e: IOException) {
                ApiKeyTestResult.Failure("Network error: ${e.message ?: "Unable to connect to the API server."}")
            } catch (e: Exception) {
                ApiKeyTestResult.Failure("${e.message ?: "Unexpected error while testing the API key."}")
            }
        }
    }

    private suspend fun testGeminiKey(apiKey: String, model: String): ApiKeyTestResult {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${model.ifBlank { "gemini-3.6-flash" }}:generateContent?key=$apiKey"
        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "test") })
                    })
                })
            })
        }
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val response = testClient.newCall(request).execute()
        return if (response.isSuccessful) {
            ApiKeyTestResult.Success
        } else {
            val bodyText = response.body?.string() ?: ""
            ApiKeyTestResult.Failure("Gemini API error (HTTP ${response.code}): ${response.message}. $bodyText")
        }
    }

    private suspend fun testOpenAICompatibleKey(apiKey: String, baseUrl: String, model: String): ApiKeyTestResult {
        var url = baseUrl.trim()
        if (url.endsWith("/")) {
            url = url.substring(0, url.length - 1)
        }
        val endpoint = if (url.endsWith("/chat/completions")) url else "$url/chat/completions"
        val body = JSONObject().apply {
            put("model", model.ifBlank { "gpt-4o-mini" })
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "test")
                })
            })
            put("max_tokens", 10)
        }
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val response = testClient.newCall(request).execute()
        return if (response.isSuccessful) {
            ApiKeyTestResult.Success
        } else {
            val bodyText = response.body?.string() ?: ""
            ApiKeyTestResult.Failure("API error (HTTP ${response.code}): ${response.message}. $bodyText")
        }
    }

    suspend fun initialize() {
        DatabaseInitializer.ensureInitialized(database)
        if (appSettingsDao.getSettings() == null) {
            appSettingsDao.insertOrUpdate(com.example.data.database.AppSettingsEntity())
        }
    }

    // --- Foods / Inventory ---
    val allFoodsFlow: Flow<List<FoodEntity>> = foodDao.getAllFoodsFlow()

    suspend fun getAllFoods(): List<FoodEntity> = foodDao.getAllFoods()

    fun searchFoods(query: String): Flow<List<FoodEntity>> {
        return if (query.isBlank()) {
            foodDao.getAllFoodsFlow()
        } else {
            foodDao.searchFoods(query)
        }
    }

    suspend fun getFoodById(id: Long): FoodEntity? = foodDao.getFoodById(id)

    suspend fun insertFood(food: FoodEntity): Long = foodDao.insertFood(food)

    suspend fun updateFood(food: FoodEntity) = foodDao.updateFood(food)

    suspend fun deleteFood(food: FoodEntity) = foodDao.deleteFood(food)

    suspend fun deleteFoodById(id: Long) = foodDao.deleteFoodById(id)

    // --- Food Logs ---
    fun getFoodLogsForDate(dateString: String): Flow<List<FoodLogEntity>> =
        foodLogDao.getLogsForDateFlow(dateString)

    suspend fun insertFoodLog(log: FoodLogEntity): Long = foodLogDao.insertLog(log)

    suspend fun insertFoodLogs(logs: List<FoodLogEntity>) = foodLogDao.insertAllLogs(logs)

    suspend fun updateFoodLog(log: FoodLogEntity) = foodLogDao.updateLog(log)

    suspend fun deleteFoodLog(log: FoodLogEntity) = foodLogDao.deleteLog(log)

    suspend fun deleteFoodLogById(id: Long) = foodLogDao.deleteLogById(id)

    // --- Water Logs ---
    fun getWaterLogsForDate(dateString: String): Flow<List<WaterLogEntity>> =
        waterLogDao.getWaterLogsForDateFlow(dateString)

    fun getTotalWaterForDate(dateString: String): Flow<Int?> =
        waterLogDao.getTotalWaterForDateFlow(dateString)

    suspend fun addWater(amountMl: Int, dateString: String): Long {
        return waterLogDao.insertWaterLog(
            WaterLogEntity(
                amountMl = amountMl,
                dateString = dateString
            )
        )
    }

    suspend fun deleteWaterLog(waterLog: WaterLogEntity) = waterLogDao.deleteWaterLog(waterLog)

    suspend fun deleteWaterLogById(id: Long) = waterLogDao.deleteWaterLogById(id)

    // --- Goals ---
    val goalsFlow: Flow<NutritionGoalEntity?> = goalDao.getGoalsFlow()

    suspend fun getGoals(): NutritionGoalEntity {
        return goalDao.getGoals() ?: NutritionGoalEntity()
    }

    suspend fun updateGoals(goals: NutritionGoalEntity) {
        goalDao.insertOrUpdateGoals(goals)
    }

    // --- Combined Daily Summary ---
    fun getDailySummary(dateString: String): Flow<DailyNutritionSummary> {
        return combine(
            foodLogDao.getLogsForDateFlow(dateString),
            waterLogDao.getWaterLogsForDateFlow(dateString),
            goalDao.getGoalsFlow()
        ) { foodLogs, waterLogs, goal ->
            val effectiveGoal = goal ?: NutritionGoalEntity()
            var calSum = 0.0
            var pSum = 0.0
            var cSum = 0.0
            var fSum = 0.0

            for (log in foodLogs) {
                calSum += log.calories
                pSum += log.protein
                cSum += log.carbohydrates
                fSum += log.fat
            }

            val waterSum = waterLogs.sumOf { it.amountMl }

            DailyNutritionSummary(
                dateString = dateString,
                totalCalories = Math.round(calSum * 10.0) / 10.0,
                caloriesGoal = effectiveGoal.caloriesGoal,
                totalProtein = Math.round(pSum * 10.0) / 10.0,
                proteinGoal = effectiveGoal.proteinGoal,
                totalCarbs = Math.round(cSum * 10.0) / 10.0,
                carbsGoal = effectiveGoal.carbsGoal,
                totalFat = Math.round(fSum * 10.0) / 10.0,
                fatGoal = effectiveGoal.fatGoal,
                totalWaterMl = waterSum,
                waterGoalMl = effectiveGoal.waterGoalMl,
                foodLogs = foodLogs,
                waterLogs = waterLogs
            )
        }
    }

    // --- Weekly Summaries (for the week calendar strip) ---
    fun getWeekSummaries(startDate: LocalDate): Flow<List<DailyNutritionSummary>> {
        val start = startDate.toString()
        val end = startDate.plusDays(6).toString()
        return combine(
            foodLogDao.getLogsBetweenDatesFlow(start, end),
            waterLogDao.getWaterLogsBetweenDatesFlow(start, end),
            goalDao.getGoalsFlow()
        ) { foodLogs, waterLogs, goal ->
            val effectiveGoal = goal ?: NutritionGoalEntity()
            val foodLogsByDate = foodLogs.groupBy { it.dateString }
            val waterLogsByDate = waterLogs.groupBy { it.dateString }

            (0 until 7).map { offset ->
                val date = startDate.plusDays(offset.toLong())
                val dateString = date.toString()
                val dayFood = foodLogsByDate[dateString] ?: emptyList()
                val dayWater = waterLogsByDate[dateString] ?: emptyList()

                var calSum = 0.0
                var pSum = 0.0
                var cSum = 0.0
                var fSum = 0.0
                for (log in dayFood) {
                    calSum += log.calories
                    pSum += log.protein
                    cSum += log.carbohydrates
                    fSum += log.fat
                }
                val waterSum = dayWater.sumOf { it.amountMl }

                DailyNutritionSummary(
                    dateString = dateString,
                    totalCalories = Math.round(calSum * 10.0) / 10.0,
                    caloriesGoal = effectiveGoal.caloriesGoal,
                    totalProtein = Math.round(pSum * 10.0) / 10.0,
                    proteinGoal = effectiveGoal.proteinGoal,
                    totalCarbs = Math.round(cSum * 10.0) / 10.0,
                    carbsGoal = effectiveGoal.carbsGoal,
                    totalFat = Math.round(fSum * 10.0) / 10.0,
                    fatGoal = effectiveGoal.fatGoal,
                    totalWaterMl = waterSum,
                    waterGoalMl = effectiveGoal.waterGoalMl,
                    foodLogs = dayFood,
                    waterLogs = dayWater
                )
            }
        }
    }

    // --- Analytics Aggregation ---
    suspend fun getTrendData(startDate: String, endDate: String, daysCount: Int): List<DayTrendPoint> {
        val foodLogs = foodLogDao.getLogsBetweenDates(startDate, endDate)
        val waterLogs = waterLogDao.getWaterLogsBetweenDatesFlow(startDate, endDate)

        val foodLogsByDate = foodLogs.groupBy { it.dateString }

        // Generate day list
        val result = mutableListOf<DayTrendPoint>()
        var curDate = LocalDate.parse(startDate)
        val lastDate = LocalDate.parse(endDate)

        while (!curDate.isAfter(lastDate)) {
            val dStr = curDate.toString()
            val dayFood = foodLogsByDate[dStr] ?: emptyList()
            val c = dayFood.sumOf { it.calories }
            val p = dayFood.sumOf { it.protein }
            val cb = dayFood.sumOf { it.carbohydrates }
            val f = dayFood.sumOf { it.fat }

            val label = "${curDate.monthValue}/${curDate.dayOfMonth}"
            result.add(
                DayTrendPoint(
                    dateString = dStr,
                    displayLabel = label,
                    calories = Math.round(c * 10.0) / 10.0,
                    protein = Math.round(p * 10.0) / 10.0,
                    carbs = Math.round(cb * 10.0) / 10.0,
                    fat = Math.round(f * 10.0) / 10.0,
                    waterMl = 0 // updated below if needed
                )
            )
            curDate = curDate.plusDays(1)
        }
        return result
    }
}
