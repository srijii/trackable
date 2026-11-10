package com.example.ai

import com.example.data.database.FoodEntity

/**
 * Abstraction layer for AI-powered natural language nutrition parsing.
 * Allows swapping the provider from Gemini to NVIDIA NIM, Groq, or local models seamlessly.
 */
interface NutritionAIService {
    val providerName: String

    /** Whether this provider can analyze nutrition-label / product photos. */
    val supportsImageInput: Boolean get() = false

    suspend fun parseFoodInput(
        userInput: String,
        inventory: List<FoodEntity>
    ): NutritionAiParseResult

    /**
     * Extracts a food product from a base64-encoded JPEG/PNG image of a nutrition
     * label or product packaging. Providers that support vision override this.
     */
    suspend fun extractNutritionLabel(imageBase64: String): NutritionAiParseResult {
        return NutritionAiParseResult(
            isSuccess = false,
            providerUsed = providerName,
            errorMessage = "Image scanning is not supported by $providerName. Switch to a vision-capable AI model (for example Groq Qwen) in Settings, or add the food manually."
        )
    }
}
