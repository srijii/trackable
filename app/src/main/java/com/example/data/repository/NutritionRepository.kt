package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseInitializer
import com.example.data.database.FoodEntity
import com.example.data.database.FoodLogEntity
import com.example.data.database.NutritionGoalEntity
import com.example.data.database.WaterLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

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
