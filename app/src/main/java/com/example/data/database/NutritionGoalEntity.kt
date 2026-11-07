package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nutrition_goals")
data class NutritionGoalEntity(
    @PrimaryKey
    val id: Int = 1,
    val caloriesGoal: Double = 2200.0,
    val proteinGoal: Double = 140.0,
    val carbsGoal: Double = 220.0,
    val fatGoal: Double = 65.0,
    val waterGoalMl: Int = 3000,
    val currentWeightKg: Double? = 75.0,
    val targetWeightKg: Double? = 70.0,
    val heightCm: Double? = 175.0,
    val age: Int? = 28,
    val activityLevel: String = "Moderate"
)
