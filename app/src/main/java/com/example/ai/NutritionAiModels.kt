package com.example.ai

import com.example.data.database.FoodEntity

data class ParsedItemCandidate(
    val rawName: String,
    val matchedFood: FoodEntity? = null,
    val quantity: Double = 1.0,
    val servingUnit: String = "serving",
    val mealType: String = "Snack",
    val isAmbiguous: Boolean = false,
    val candidateFoods: List<FoodEntity> = emptyList(),
    val isUnmatched: Boolean = false,
    val calculatedCalories: Double = 0.0,
    val calculatedProtein: Double = 0.0,
    val calculatedCarbs: Double = 0.0,
    val calculatedFat: Double = 0.0,
    val note: String? = null
)

data class NutritionAiParseResult(
    val isSuccess: Boolean = true,
    val waterMl: Int = 0,
    val items: List<ParsedItemCandidate> = emptyList(),
    val summaryText: String = "",
    val providerUsed: String = "Gemini",
    val errorMessage: String? = null
)
