package com.example.ai

import com.example.data.database.FoodEntity
import com.example.domain.NutritionCalculationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NvidiaNimNutritionAIService(
    private val apiKeyProvider: () -> String,
    private val modelProvider: () -> String = { "meta/llama-3.3-70b-instruct" },
    private val baseUrlProvider: () -> String = { "https://integrate.api.nvidia.com/v1" },
    private val localFallbackService: LocalRuleBasedNutritionAIService = LocalRuleBasedNutritionAIService()
) : NutritionAIService {

    override val providerName: String = "NVIDIA NIM"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun parseFoodInput(
        userInput: String,
        inventory: List<FoodEntity>
    ): NutritionAiParseResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank()) {
            val fallback = localFallbackService.parseFoodInput(userInput, inventory)
            return@withContext fallback.copy(
                providerUsed = "Local (No NVIDIA API Key configured)"
            )
        }

        try {
            val inventorySummary = JSONArray().apply {
                inventory.forEach { f ->
                    put(
                        JSONObject().apply {
                            put("id", f.id)
                            put("name", f.name)
                            put("brand", f.brand)
                            put("servingSize", f.servingSize)
                            put("servingUnit", f.servingUnit)
                            put("calories", f.calories)
                            put("protein", f.protein)
                            put("carbs", f.carbohydrates)
                            put("fat", f.fat)
                        }
                    )
                }
            }

            val systemPrompt = """
                You are a nutrition logging parser for an Android nutrition app.
                The user has the following foods in their personal food inventory (JSON):
                $inventorySummary

                Task:
                1. Identify all foods/drinks and their quantities/units from the user's sentence.
                2. If water is consumed, extract total amount in ml in "waterMl".
                3. Match each food to an inventory item.
                   - For each food, calculate the serving multiplier based on consumed quantity vs reference serving size.
                     Example: If reference serving is 100g and user ate 46g, multiplier is 0.46 (46.0 / 100.0).
                   - If ambiguous (e.g. multiple whey products exist and user says 'whey protein'), set "ambiguous": true and list candidate ids in "candidateFoodIds".
                   - If not found in inventory, set "matchedFoodId": null and "unmatched": true.
                4. Output STRICT JSON ONLY matching this schema, no markdown or extra commentary:
                {
                  "waterMl": 0,
                  "items": [
                    {
                      "rawName": "paneer",
                      "matchedFoodId": 3,
                      "quantity": 46.0,
                      "unit": "g",
                      "multiplier": 0.46,
                      "ambiguous": false,
                      "candidateFoodIds": [],
                      "unmatched": false,
                      "mealType": "Lunch"
                    }
                  ]
                }
            """.trimIndent()

            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userInput)
                })
            }

            val modelName = modelProvider().ifBlank { "meta/llama-3.3-70b-instruct" }
            val requestJson = JSONObject().apply {
                put("model", modelName)
                put("messages", messagesArray)
                put("temperature", 0.1)
                put("max_tokens", 800)
            }

            var baseUrl = baseUrlProvider().trim()
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length - 1)
            }
            val endpoint = if (baseUrl.endsWith("/chat/completions")) baseUrl else "$baseUrl/chat/completions"

            val request = Request.Builder()
                .url(endpoint)
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "application/json")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val fallback = localFallbackService.parseFoodInput(userInput, inventory)
                return@withContext fallback.copy(
                    providerUsed = "Local (NVIDIA error HTTP ${response.code})"
                )
            }

            val body = response.body?.string() ?: ""
            val jsonRoot = JSONObject(body)
            val choices = jsonRoot.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val messageObj = firstChoice?.optJSONObject("message")
            val content = messageObj?.optString("content") ?: ""

            val cleanedJson = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val parsedJson = JSONObject(cleanedJson)
            val waterMl = parsedJson.optInt("waterMl", 0)
            val itemsJson = parsedJson.optJSONArray("items") ?: JSONArray()
            val parsedItems = mutableListOf<ParsedItemCandidate>()

            val inventoryMap = inventory.associateBy { it.id }

            for (i in 0 until itemsJson.length()) {
                val itemObj = itemsJson.getJSONObject(i)
                val rawName = itemObj.optString("rawName", "Food")
                val matchedFoodId = if (itemObj.isNull("matchedFoodId")) null else itemObj.optLong("matchedFoodId")
                val quantity = itemObj.optDouble("quantity", 1.0)
                val unit = itemObj.optString("unit", "piece")
                val aiMultiplier = itemObj.optDouble("multiplier", -1.0)
                val isAmbiguous = itemObj.optBoolean("ambiguous", false)
                val isUnmatched = itemObj.optBoolean("unmatched", false) || (matchedFoodId == null && !isAmbiguous)
                val mealType = itemObj.optString("mealType", "Snack")

                val candidateIds = itemObj.optJSONArray("candidateFoodIds") ?: JSONArray()
                val candidateFoods = mutableListOf<FoodEntity>()
                for (c in 0 until candidateIds.length()) {
                    val cId = candidateIds.optLong(c)
                    inventoryMap[cId]?.let { candidateFoods.add(it) }
                }

                val matchedFood = matchedFoodId?.let { inventoryMap[it] } ?: candidateFoods.firstOrNull()

                if (matchedFood != null && !isUnmatched) {
                    val calc = if (aiMultiplier > 0.0) {
                        NutritionCalculationEngine.calculateWithMultiplier(matchedFood, aiMultiplier)
                    } else {
                        NutritionCalculationEngine.calculate(
                            food = matchedFood,
                            consumedQuantity = quantity,
                            consumedUnit = unit
                        )
                    }
                    parsedItems.add(
                        ParsedItemCandidate(
                            rawName = rawName,
                            matchedFood = matchedFood,
                            quantity = quantity,
                            servingUnit = unit,
                            mealType = mealType,
                            isAmbiguous = isAmbiguous,
                            candidateFoods = candidateFoods,
                            isUnmatched = false,
                            calculatedCalories = calc.calories,
                            calculatedProtein = calc.protein,
                            calculatedCarbs = calc.carbohydrates,
                            calculatedFat = calc.fat
                        )
                    )
                } else {
                    parsedItems.add(
                        ParsedItemCandidate(
                            rawName = rawName,
                            matchedFood = null,
                            quantity = quantity,
                            servingUnit = unit,
                            mealType = mealType,
                            isAmbiguous = isAmbiguous,
                            candidateFoods = candidateFoods,
                            isUnmatched = true,
                            calculatedCalories = 0.0,
                            calculatedProtein = 0.0,
                            calculatedCarbs = 0.0,
                            calculatedFat = 0.0,
                            note = "Not found in your inventory. Tap to match or add."
                        )
                    )
                }
            }

            NutritionAiParseResult(
                isSuccess = true,
                waterMl = waterMl,
                items = parsedItems,
                providerUsed = "NVIDIA NIM ($modelName)",
                summaryText = "Parsed with NVIDIA NIM"
            )
        } catch (e: Exception) {
            val localResult = localFallbackService.parseFoodInput(userInput, inventory)
            localResult.copy(providerUsed = "Local (Fallback: ${e.message ?: "Network error"})")
        }
    }
}
