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
        }
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
        }
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
        }
    )

    private val groqService = GroqNutritionAIService(
        apiKeyProvider = {
            kotlinx.coroutines.runBlocking {
                repository.getSettings().groqApiKey
            }
        },
        modelProvider = {
            kotlinx.coroutines.runBlocking {
                repository.getSettings().groqModel
            }
        }
    )

    override val providerName: String
        get() {
            val settings = kotlinx.coroutines.runBlocking { repository.getSettings() }
            return when (settings.aiProvider) {
                "NVIDIA_NIM" -> "NVIDIA NIM (${settings.nvidiaModel})"
                "OPENAI" -> "OpenAI (${settings.openaiModel})"
                "GROQ" -> "Groq (${settings.groqModel})"
                "LOCAL" -> "Local Smart Engine"
                else -> "Gemini (${settings.geminiModel})"
            }
        }

    override val supportsImageInput: Boolean
        get() {
            val provider = kotlinx.coroutines.runBlocking { repository.getSettings().aiProvider }
            return provider == "GROQ"
        }

    override suspend fun parseFoodInput(
        userInput: String,
        inventory: List<FoodEntity>
    ): NutritionAiParseResult {
        val settings = repository.getSettings()
        return when (settings.aiProvider) {
            "NVIDIA_NIM" -> nvidiaService.parseFoodInput(userInput, inventory)
            "OPENAI" -> openAiService.parseFoodInput(userInput, inventory)
            "GROQ" -> groqService.parseFoodInput(userInput, inventory)
            "LOCAL" -> localService.parseFoodInput(userInput, inventory)
            else -> geminiService.parseFoodInput(userInput, inventory)
        }
    }

    override suspend fun extractNutritionLabel(imageBase64: String): NutritionAiParseResult {
        val settings = repository.getSettings()
        // Groq Qwen is the vision-capable provider. If the user configured a
        // Groq key, use it for photos even while text logging uses another
        // provider -- otherwise the camera would fail for a purely unrelated
        // text-model choice.
        val hasGroqKey = com.example.util.ApiKeyUtils.normalize(settings.groqApiKey)
            .let { it.isNotBlank() && !it.startsWith("MY_") }
        return when {
            settings.aiProvider == "GROQ" -> groqService.extractNutritionLabel(imageBase64)
            hasGroqKey -> groqService.extractNutritionLabel(imageBase64)
            else -> NutritionAiParseResult(
                isSuccess = false,
                providerUsed = providerName,
                errorMessage = "Photo scanning needs a vision-capable AI model. Add a Groq API key in Settings (Groq Qwen reads nutrition labels), or add the food manually."
            )
        }
    }
}
