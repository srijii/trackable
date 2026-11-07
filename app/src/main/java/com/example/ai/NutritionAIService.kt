package com.example.ai

import com.example.data.database.FoodEntity

/**
 * Abstraction layer for AI-powered natural language nutrition parsing.
 * Allows swapping the provider from Gemini to NVIDIA NIM or local models seamlessly.
 */
interface NutritionAIService {
    val providerName: String

    suspend fun parseFoodInput(
        userInput: String,
        inventory: List<FoodEntity>
    ): NutritionAiParseResult
}
