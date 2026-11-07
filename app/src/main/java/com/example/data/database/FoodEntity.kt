package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val brand: String = "",
    val category: String = "General",
    val servingSize: Double = 100.0,
    val servingUnit: String = "g", // "g", "ml", "scoop", "piece", "cup", "slice"
    val calories: Double, // kcal per serving
    val protein: Double,  // grams per serving
    val carbohydrates: Double, // grams per serving
    val fat: Double, // grams per serving
    val fiber: Double? = null, // grams per serving
    val sugar: Double? = null, // grams per serving
    val barcode: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
