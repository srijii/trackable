package com.example.data.backup

import com.example.data.database.AppSettingsEntity
import com.example.data.database.FoodEntity
import com.example.data.database.FoodLogEntity
import com.example.data.database.NutritionGoalEntity
import com.example.data.database.WaterLogEntity
import com.example.data.database.WeightLogEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON encoding/decoding for backups. The format is deliberately explicit
 * (rather than reflective) so old backup files keep working as the app evolves.
 */
object BackupJson {

    private const val FORMAT = "trackable_backup"
    private const val VERSION = 1

    fun encode(payload: BackupPayload): String {
        val data = JSONObject()
        payload.foods?.let { foods ->
            data.put("foods", JSONArray().apply { foods.forEach { put(foodToJson(it)) } })
        }
        payload.foodLogs?.let { logs ->
            data.put("foodLogs", JSONArray().apply { logs.forEach { put(foodLogToJson(it)) } })
        }
        payload.waterLogs?.let { logs ->
            data.put("waterLogs", JSONArray().apply { logs.forEach { put(waterLogToJson(it)) } })
        }
        payload.weightLogs?.let { logs ->
            data.put("weightLogs", JSONArray().apply { logs.forEach { put(weightLogToJson(it)) } })
        }
        payload.goals?.let { data.put("goals", goalsToJson(it)) }
        payload.settings?.let { data.put("settings", settingsToJson(it)) }

        val root = JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("scope", payload.scope.name)
            put("exportedAt", payload.exportedAt)
            put("data", data)
        }
        return root.toString(2)
    }

    fun decode(json: String): BackupPayload {
        val root = JSONObject(json)
        require(root.optString("format") == FORMAT) {
            "This file isn't a Trackable backup."
        }
        val version = root.optInt("version", 0)
        require(version in 1..VERSION) {
            "This backup version ($version) isn't supported."
        }
        val data = root.optJSONObject("data")
            ?: throw IllegalArgumentException("The backup file has no data section.")
        val scope = runCatching { BackupScope.valueOf(root.optString("scope")) }
            .getOrDefault(BackupScope.ALL_DATA)

        val payload = BackupPayload(
            scope = scope,
            exportedAt = root.optLong("exportedAt", 0L),
            foods = data.optJSONArray("foods")?.mapArray { foodFromJson(it) },
            foodLogs = data.optJSONArray("foodLogs")?.mapArray { foodLogFromJson(it) },
            waterLogs = data.optJSONArray("waterLogs")?.mapArray { waterLogFromJson(it) },
            weightLogs = data.optJSONArray("weightLogs")?.mapArray { weightLogFromJson(it) },
            goals = data.optJSONObject("goals")?.let { goalsFromJson(it) },
            settings = data.optJSONObject("settings")?.let { settingsFromJson(it) }
        )
        require(!payload.isEmpty()) { "The backup file doesn't contain any data." }
        return payload
    }

    // ------------------------------------------------------------------
    // Foods
    // ------------------------------------------------------------------

    private fun foodToJson(food: FoodEntity): JSONObject = JSONObject().apply {
        put("id", food.id)
        put("name", food.name)
        put("brand", food.brand)
        put("category", food.category)
        put("servingSize", food.servingSize)
        put("servingUnit", food.servingUnit)
        put("calories", food.calories)
        put("protein", food.protein)
        put("carbohydrates", food.carbohydrates)
        put("fat", food.fat)
        put("fiber", food.fiber ?: JSONObject.NULL)
        put("sugar", food.sugar ?: JSONObject.NULL)
        put("barcode", food.barcode ?: JSONObject.NULL)
        put("createdAt", food.createdAt)
    }

    private fun foodFromJson(json: JSONObject): FoodEntity = FoodEntity(
        id = json.optLong("id", 0L),
        name = json.getString("name"),
        brand = json.optString("brand", ""),
        category = json.optString("category", "General"),
        servingSize = json.optDouble("servingSize", 100.0),
        servingUnit = json.optString("servingUnit", "g"),
        calories = json.optDouble("calories", 0.0),
        protein = json.optDouble("protein", 0.0),
        carbohydrates = json.optDouble("carbohydrates", 0.0),
        fat = json.optDouble("fat", 0.0),
        fiber = json.nullableDouble("fiber"),
        sugar = json.nullableDouble("sugar"),
        barcode = json.nullableString("barcode"),
        createdAt = json.optLong("createdAt", System.currentTimeMillis())
    )

    // ------------------------------------------------------------------
    // Food logs
    // ------------------------------------------------------------------

    private fun foodLogToJson(log: FoodLogEntity): JSONObject = JSONObject().apply {
        put("id", log.id)
        put("foodId", log.foodId ?: JSONObject.NULL)
        put("foodNameSnapshot", log.foodNameSnapshot)
        put("brandSnapshot", log.brandSnapshot)
        put("quantity", log.quantity)
        put("servingUnit", log.servingUnit)
        put("calories", log.calories)
        put("protein", log.protein)
        put("carbohydrates", log.carbohydrates)
        put("fat", log.fat)
        put("fiber", log.fiber ?: JSONObject.NULL)
        put("sugar", log.sugar ?: JSONObject.NULL)
        put("timestamp", log.timestamp)
        put("dateString", log.dateString)
        put("mealType", log.mealType)
        put("originalPrompt", log.originalPrompt)
    }

    private fun foodLogFromJson(json: JSONObject): FoodLogEntity = FoodLogEntity(
        id = json.optLong("id", 0L),
        foodId = json.nullableLong("foodId"),
        foodNameSnapshot = json.getString("foodNameSnapshot"),
        brandSnapshot = json.optString("brandSnapshot", ""),
        quantity = json.optDouble("quantity", 1.0),
        servingUnit = json.optString("servingUnit", "g"),
        calories = json.optDouble("calories", 0.0),
        protein = json.optDouble("protein", 0.0),
        carbohydrates = json.optDouble("carbohydrates", 0.0),
        fat = json.optDouble("fat", 0.0),
        fiber = json.nullableDouble("fiber"),
        sugar = json.nullableDouble("sugar"),
        timestamp = json.optLong("timestamp", System.currentTimeMillis()),
        dateString = json.getString("dateString"),
        mealType = json.optString("mealType", "Snack"),
        originalPrompt = json.optString("originalPrompt", "")
    )

    // ------------------------------------------------------------------
    // Water logs
    // ------------------------------------------------------------------

    private fun waterLogToJson(log: WaterLogEntity): JSONObject = JSONObject().apply {
        put("id", log.id)
        put("amountMl", log.amountMl)
        put("timestamp", log.timestamp)
        put("dateString", log.dateString)
    }

    private fun waterLogFromJson(json: JSONObject): WaterLogEntity = WaterLogEntity(
        id = json.optLong("id", 0L),
        amountMl = json.optInt("amountMl", 0),
        timestamp = json.optLong("timestamp", System.currentTimeMillis()),
        dateString = json.getString("dateString")
    )

    // ------------------------------------------------------------------
    // Weight logs
    // ------------------------------------------------------------------

    private fun weightLogToJson(log: WeightLogEntity): JSONObject = JSONObject().apply {
        put("id", log.id)
        put("dateEpochDay", log.dateEpochDay)
        put("weightKg", log.weightKg)
        put("note", log.note)
        put("loggedAtTimestamp", log.loggedAtTimestamp)
    }

    private fun weightLogFromJson(json: JSONObject): WeightLogEntity = WeightLogEntity(
        id = json.optLong("id", 0L),
        dateEpochDay = json.optLong("dateEpochDay", 0L),
        weightKg = json.optDouble("weightKg", 0.0),
        note = json.optString("note", ""),
        loggedAtTimestamp = json.optLong("loggedAtTimestamp", System.currentTimeMillis())
    )

    // ------------------------------------------------------------------
    // Goals / profile
    // ------------------------------------------------------------------

    private fun goalsToJson(goals: NutritionGoalEntity): JSONObject = JSONObject().apply {
        put("id", goals.id)
        put("name", goals.name)
        put("sex", goals.sex)
        put("caloriesGoal", goals.caloriesGoal)
        put("proteinGoal", goals.proteinGoal)
        put("carbsGoal", goals.carbsGoal)
        put("fatGoal", goals.fatGoal)
        put("waterGoalMl", goals.waterGoalMl)
        put("currentWeightKg", goals.currentWeightKg ?: JSONObject.NULL)
        put("targetWeightKg", goals.targetWeightKg ?: JSONObject.NULL)
        put("heightCm", goals.heightCm ?: JSONObject.NULL)
        put("age", goals.age ?: JSONObject.NULL)
        put("activityLevel", goals.activityLevel)
    }

    private fun goalsFromJson(json: JSONObject): NutritionGoalEntity = NutritionGoalEntity(
        id = json.optInt("id", 1),
        name = json.optString("name", ""),
        sex = json.optString("sex", ""),
        caloriesGoal = json.optDouble("caloriesGoal", 2200.0),
        proteinGoal = json.optDouble("proteinGoal", 140.0),
        carbsGoal = json.optDouble("carbsGoal", 220.0),
        fatGoal = json.optDouble("fatGoal", 65.0),
        waterGoalMl = json.optInt("waterGoalMl", 3000),
        currentWeightKg = json.nullableDouble("currentWeightKg"),
        targetWeightKg = json.nullableDouble("targetWeightKg"),
        heightCm = json.nullableDouble("heightCm"),
        age = json.nullableInt("age"),
        activityLevel = json.optString("activityLevel", "")
    )

    // ------------------------------------------------------------------
    // App settings
    // ------------------------------------------------------------------

    private fun settingsToJson(settings: AppSettingsEntity): JSONObject = JSONObject().apply {
        put("id", settings.id)
        put("aiProvider", settings.aiProvider)
        put("geminiApiKey", settings.geminiApiKey)
        put("geminiModel", settings.geminiModel)
        put("nvidiaApiKey", settings.nvidiaApiKey)
        put("nvidiaModel", settings.nvidiaModel)
        put("nvidiaBaseUrl", settings.nvidiaBaseUrl)
        put("openaiApiKey", settings.openaiApiKey)
        put("openaiModel", settings.openaiModel)
        put("groqApiKey", settings.groqApiKey)
        put("groqModel", settings.groqModel)
        put("dismissedGoalWarning", settings.dismissedGoalWarning)
        put("startDate", settings.startDate ?: JSONObject.NULL)
        put("useStartDate", settings.useStartDate)
        put("blockBeforeStartDate", settings.blockBeforeStartDate)
        put("blockAfterToday", settings.blockAfterToday)
        put("cupSize1Ml", settings.cupSize1Ml)
        put("cupSize2Ml", settings.cupSize2Ml)
        put("hasCompletedOnboarding", settings.hasCompletedOnboarding)
    }

    private fun settingsFromJson(json: JSONObject): AppSettingsEntity = AppSettingsEntity(
        id = json.optInt("id", 1),
        aiProvider = json.optString("aiProvider", "LOCAL"),
        geminiApiKey = json.optString("geminiApiKey", ""),
        geminiModel = json.optString("geminiModel", "gemini-3.6-flash"),
        nvidiaApiKey = json.optString("nvidiaApiKey", ""),
        nvidiaModel = json.optString("nvidiaModel", "minimaxai/minimax-m3.1-flash"),
        nvidiaBaseUrl = json.optString("nvidiaBaseUrl", "https://integrate.api.nvidia.com/v1"),
        openaiApiKey = json.optString("openaiApiKey", ""),
        openaiModel = json.optString("openaiModel", "gpt-4o-mini"),
        groqApiKey = json.optString("groqApiKey", ""),
        groqModel = json.optString("groqModel", "qwen/qwen3.8-27b"),
        dismissedGoalWarning = json.optBoolean("dismissedGoalWarning", false),
        startDate = json.nullableString("startDate"),
        useStartDate = json.optBoolean("useStartDate", true),
        blockBeforeStartDate = json.optBoolean("blockBeforeStartDate", true),
        blockAfterToday = json.optBoolean("blockAfterToday", true),
        cupSize1Ml = json.optInt("cupSize1Ml", 250),
        cupSize2Ml = json.optInt("cupSize2Ml", 500),
        hasCompletedOnboarding = json.optBoolean("hasCompletedOnboarding", true)
    )

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun JSONObject.nullableString(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private fun JSONObject.nullableDouble(key: String): Double? =
        if (has(key) && !isNull(key)) getDouble(key) else null

    private fun JSONObject.nullableLong(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null

    private fun JSONObject.nullableInt(key: String): Int? =
        if (has(key) && !isNull(key)) getInt(key) else null

    private inline fun <T> JSONArray.mapArray(transform: (JSONObject) -> T): List<T> {
        val result = ArrayList<T>(length())
        for (i in 0 until length()) {
            result.add(transform(getJSONObject(i)))
        }
        return result
    }
}
