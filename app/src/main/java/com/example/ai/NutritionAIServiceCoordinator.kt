package com.example.ai

import com.example.data.database.FoodEntity
import com.example.data.repository.NutritionRepository

class NutritionAIServiceCoordinator(
    private val repository: NutritionRepository
) : NutritionAIService {

    private val localService = LocalRuleBasedNutritionAIService()

    private val geminiService = GeminiNutritionAIService(
        apiKeyProvider = {
            kotlinx.coroutines.runBlocking {
                repository.getSettings().geminiApiKey
            }
        },
        modelProvider = {
            kotlinx.coroutines.runBlocking {
                repository.getSettings().geminiModel
            }
        },
        localFallbackService = localService
    )

    private val nvidiaService = NvidiaNimNutritionAIService(
        apiKeyProvider = {
            kotlinx.coroutines.runBlocking {
                repository.getSettings().nvidiaApiKey
            }
        },
        modelProvider = {
            kotlinx.coroutines.runBlocking {
                repository.getSettings().nvidiaModel
            }
        },
        baseUrlProvider = {
            kotlinx.coroutines.runBlocking {
                repository.getSettings().nvidiaBaseUrl
            }
        },
        localFallbackService = localService
    )

    private val openAiService = NvidiaNimNutritionAIService(
        apiKeyProvider = {
            kotlinx.coroutines.runBlocking {
                repository.getSettings().openaiApiKey
            }
        },
        modelProvider = {
            kotlinx.coroutines.runBlocking {
                repository.getSettings().openaiModel
            }
        },
        baseUrlProvider = {
            "https://api.openai.com/v1"
        },
        localFallbackService = localService
    )

    override val providerName: String
        get() {
            val settings = kotlinx.coroutines.runBlocking { repository.getSettings() }
            return when (settings.aiProvider) {
                "NVIDIA_NIM" -> "NVIDIA NIM (${settings.nvidiaModel})"
                "OPENAI" -> "OpenAI (${settings.openaiModel})"
                "LOCAL" -> "Local Smart Engine"
                else -> "Gemini (${settings.geminiModel})"
            }
        }

    override suspend fun parseFoodInput(
        userInput: String,
        inventory: List<FoodEntity>
    ): NutritionAiParseResult {
        val settings = repository.getSettings()
        return when (settings.aiProvider) {
            "NVIDIA_NIM" -> nvidiaService.parseFoodInput(userInput, inventory)
            "OPENAI" -> openAiService.parseFoodInput(userInput, inventory)
            "LOCAL" -> localService.parseFoodInput(userInput, inventory)
            else -> geminiService.parseFoodInput(userInput, inventory)
        }
    }
}
