package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_logs")
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val foodId: Long? = null,
    val foodNameSnapshot: String,
    val brandSnapshot: String = "",
    val quantity: Double,
    val servingUnit: String,
    val calories: Double,
    val protein: Double,
    val carbohydrates: Double,
    val fat: Double,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String, // "YYYY-MM-DD"
    val mealType: String = "Snack", // "Breakfast", "Lunch", "Dinner", "Snack"
    val originalPrompt: String = ""
)
