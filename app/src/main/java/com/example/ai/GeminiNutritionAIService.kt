package com.example.ai

import com.example.BuildConfig
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

class GeminiNutritionAIService(
    private val apiKeyProvider: () -> String = { "" },
    private val modelProvider: () -> String = { "gemini-3.6-flash" },
    private val localFallbackService: LocalRuleBasedNutritionAIService = LocalRuleBasedNutritionAIService()
) : NutritionAIService {

    override val providerName: String = "Gemini AI"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    override suspend fun parseFoodInput(
        userInput: String,
        inventory: List<FoodEntity>
    ): NutritionAiParseResult = withContext(Dispatchers.IO) {
        val customKey = apiKeyProvider().trim()
        val apiKey = if (customKey.isNotBlank()) {
            customKey
        } else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Throwable) {
                ""
            }
        }

        // If no API key is provided or offline, use local engine directly
        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext localFallbackService.parseFoodInput(userInput, inventory)
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

            val prompt = """
                You are a nutrition logging parser. The user is logging what they ate or drank.
                User inventory of foods available at home (JSON):
                $inventorySummary

                User sentence: "$userInput"

                IMPORTANT: The user's input may contain typos, missing spaces, wrong punctuation,
                or broken formatting (e.g. "50g paner", "milkk 50ml", "paneer 50 g", "milk, paneer 50,60").
                You must be smart and tolerant:
                - Fix obvious spelling mistakes (e.g. "paner" -> "paneer", "milkk" -> "milk")
                - Join split quantities and units (e.g. "50 g" -> "50g", "200 m l" -> "200ml")
                - Parse comma-separated or punctuation-separated items correctly
                - If unsure, match to the closest inventory item by name similarity

                TASK:
                1. Identify all foods and beverages mentioned, their quantities and units.
                2. If water is consumed, extract total ml in "waterMl".
                3. Match each food item against the inventory.
                   - Calculate the multiplier for nutrition based on consumed quantity divided by reference serving size.
                     Example: If reference serving is 100g and user ate 46g, multiplier is 0.46 (46.0 / 100.0).
                   - If matched to one inventory food, set "matchedFoodId" to its id and provide "multiplier".
                   - If ambiguous between multiple foods (e.g. user says 'whey protein' and multiple exist), set "ambiguous": true and list matching ids in "candidateFoodIds".
                   - If not found in the inventory, set "matchedFoodId": null and "unmatched": true.
                4. Output ONLY valid JSON matching this schema with no markdown formatting:
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

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            val model = modelProvider().ifBlank { "gemini-3.6-flash" }
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                // Graceful fallback to local parser if API returns error
                val fallbackResult = localFallbackService.parseFoodInput(userInput, inventory)
                return@withContext fallbackResult.copy(
                    providerUsed = "Local (API error ${response.code})"
                )
            }

            val responseBody = response.body?.string() ?: ""
            val jsonRoot = JSONObject(responseBody)
            val candidatesArray = jsonRoot.optJSONArray("candidates")
            val firstCandidate = candidatesArray?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val partsArray = contentObj?.optJSONArray("parts")
            val text = partsArray?.optJSONObject(0)?.optString("text") ?: ""

            val cleanedJsonText = text.replace("```json", "").replace("```", "").trim()
            val parsedJson = JSONObject(cleanedJsonText)

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
                            note = "Not matched in your inventory. Tap to match or add."
                        )
                    )
                }
            }

            NutritionAiParseResult(
                isSuccess = true,
                waterMl = waterMl,
                items = parsedItems,
                providerUsed = "Gemini ($model)",
                summaryText = "Parsed with Gemini AI"
            )
        } catch (e: Exception) {
            // Automatic fallback to local parser
            val localResult = localFallbackService.parseFoodInput(userInput, inventory)
            localResult.copy(providerUsed = "Local (Offline/Fallback)")
        }
    }
}
