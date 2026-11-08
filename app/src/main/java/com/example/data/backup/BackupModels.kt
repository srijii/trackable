package com.example.data.backup

import com.example.data.database.AppSettingsEntity
import com.example.data.database.FoodEntity
import com.example.data.database.FoodLogEntity
import com.example.data.database.NutritionGoalEntity
import com.example.data.database.WaterLogEntity
import com.example.data.database.WeightLogEntity

/** The kinds of backups the user can create and restore. */
enum class BackupScope(val label: String, val fileNamePart: String) {
    INVENTORY_DIET("Inventory & Diet", "inventory_diet"),
    WEIGHT_HISTORY("Weight History", "weight_history"),
    ALL_DATA("All App Data", "full_backup")
}

/**
 * A snapshot of app data. Each section is `null` when it is not part of the
 * backup, which lets a single model represent partial and full backups.
 */
data class BackupPayload(
    val scope: BackupScope,
    val exportedAt: Long = System.currentTimeMillis(),
    val foods: List<FoodEntity>? = null,
    val foodLogs: List<FoodLogEntity>? = null,
    val waterLogs: List<WaterLogEntity>? = null,
    val weightLogs: List<WeightLogEntity>? = null,
    val goals: NutritionGoalEntity? = null,
    val settings: AppSettingsEntity? = null
) {
    /** Human-readable list of what this backup contains, for the confirm dialog. */
    fun contentSummary(): List<String> = buildList {
        foods?.let { add("Inventory — ${it.size} food${if (it.size == 1) "" else "s"}") }
        foodLogs?.let { add("Diet history — ${it.size} meal entr${if (it.size == 1) "y" else "ies"}") }
        waterLogs?.let { add("Water intake — ${it.size} entr${if (it.size == 1) "y" else "ies"}") }
        weightLogs?.let { add("Weight history — ${it.size} entr${if (it.size == 1) "y" else "ies"}") }
        if (goals != null) add("Profile, goals & personal details")
        if (settings != null) add("App settings (including saved API keys)")
    }

    /** Whether the payload actually holds anything that can be restored. */
    fun isEmpty(): Boolean =
        foods == null && foodLogs == null && waterLogs == null &&
            weightLogs == null && goals == null && settings == null
}
