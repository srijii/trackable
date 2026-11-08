package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nutrition_goals")
data class NutritionGoalEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "",
    val sex: String = "", // "Male" or "Female"
    val caloriesGoal: Double = 2200.0,
    val proteinGoal: Double = 140.0,
    val carbsGoal: Double = 220.0,
    val fatGoal: Double = 65.0,
    val waterGoalMl: Int = 3000,
    val currentWeightKg: Double? = null,
    val targetWeightKg: Double? = null,
    val heightCm: Double? = null,
    val age: Int? = null,
    val activityLevel: String = ""
)
