package com.example.ai

import com.example.data.database.FoodEntity
import com.example.domain.NutritionCalculationEngine
import com.example.util.ApiKeyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Groq chat-completions provider.
 *
 * Uses Qwen 3.8 27B (qwen/qwen3.8-27b) which is multimodal on Groq, so the same
 * model handles both natural-language food logging and nutrition-label photos.
 *
 * Important: errors are surfaced to the caller instead of silently falling back
 * to the local engine. The UI lets the user retry or switch to Local AI.
 */
class GroqNutritionAIService(
    private val apiKeyProvider: () -> String = { "" },
    private val modelProvider: () -> String = { "qwen/qwen3.8-27b" }
) : NutritionAIService {

    override val providerName: String = "Groq Qwen"
    override val supportsImageInput: Boolean = true

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val endpoint = "https://api.groq.com/openai/v1/chat/completions"

    // ---------------------------------------------------------------------
    // Natural-language food logging
    // ---------------------------------------------------------------------
    override suspend fun parseFoodInput(
        userInput: String,
        inventory: List<FoodEntity>
    ): NutritionAiParseResult = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyUtils.normalize(apiKeyProvider())
        if (apiKey.isBlank() || apiKey == "MY_GROQ_API_KEY") {
            return@withContext NutritionAiParseResult(
                isSuccess = false,
                providerUsed = "Groq",
                errorMessage = "No Groq API key configured. Add your Groq API key in Settings or switch to Local AI."
            )
        }
        ApiKeyUtils.groqKeyIssue(apiKey)?.let { issue ->
            return@withContext NutritionAiParseResult(
                isSuccess = false,
                providerUsed = "Groq",
                errorMessage = issue
            )
        }

        try {
            val inventorySummary = inventorySummaryJson(inventory)

            val prompt = """
                You are a nutrition logging parser for an Android nutrition app.
                The user has the following foods in their personal food inventory (JSON):
                $inventorySummary

                User sentence: "$userInput"

                The user's input may contain typos, missing spaces or broken formatting
                (e.g. "50g paner", "milkk 50ml"). Fix obvious mistakes and join split
                quantities/units (e.g. "50 g" -> "50g").

                TASK:
                1. Identify all foods/drinks and their quantities/units.
                2. If water is consumed, extract the total amount in ml in "waterMl".
                3. Match each food to an inventory item.
                   - Provide the serving "multiplier" = consumed quantity / reference serving size.
                     Example: reference serving 100g, user ate 46g -> 0.46.
                   - If ambiguous, set "ambiguous": true and list candidate ids.
                   - If not found in the inventory, set "matchedFoodId": null and "unmatched": true.
                4. Output STRICT JSON ONLY (no markdown, no commentary):
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

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }

            val content = requestCompletion(apiKey, messages)
            parseFoodItemsResponse(content, inventory, parsedProviderUsed())
        } catch (e: Exception) {
            NutritionAiParseResult(
                isSuccess = false,
                providerUsed = "Groq",
                errorMessage = "${e.message ?: "Groq request failed"}. You can retry or switch to Local AI."
            )
        }
    }

    // ---------------------------------------------------------------------
    // Nutrition label / product photo extraction
    // ---------------------------------------------------------------------
    override suspend fun extractNutritionLabel(imageBase64: String): NutritionAiParseResult =
        withContext(Dispatchers.IO) {
            val apiKey = ApiKeyUtils.normalize(apiKeyProvider())
            if (apiKey.isBlank() || apiKey == "MY_GROQ_API_KEY") {
                return@withContext NutritionAiParseResult(
                    isSuccess = false,
                    providerUsed = "Groq",
                    errorMessage = "No Groq API key configured. Add your Groq API key in Settings or switch to Local AI."
                )
            }
            ApiKeyUtils.groqKeyIssue(apiKey)?.let { issue ->
                return@withContext NutritionAiParseResult(
                    isSuccess = false,
                    providerUsed = "Groq",
                    errorMessage = issue
                )
            }

            try {
                val prompt = """
                    You are a nutrition-label and food-package extraction system.

                    Look at the uploaded photo. It may be a nutrition facts table, a product
                    package with the product name, or a photo of the food itself.

                    Extract the product information and return STRICT JSON ONLY:
                    {
                      "product_name": "Oats",
                      "brand": "Example",
                      "category": "Grains",
                      "basis": "100g",
                      "serving_size": 100.0,
                      "serving_unit": "g",
                      "calories_kcal": 389,
                      "protein_g": 16.9,
                      "carbs_g": 66.3,
                      "fat_g": 6.9,
                      "fiber_g": 10.6,
                      "sugar_g": 0,
                      "sodium_mg": 5,
                      "confidence": "high",
                      "notes": ""
                    }

                    Rules:
                    1. Preserve the exact values printed on the label.
                    2. Identify whether values are per 100g, per 100ml, or per serving and set
                       "serving_size"/"serving_unit"/"basis" accordingly. Convert values so they
                       match that serving size.
                    3. Never invent missing values. Use null for unreadable or missing fields.
                    4. Do not confuse serving size with package size.
                    5. If the photo is a food (not a label), estimate per-100g nutrition and mark
                       "confidence" as "estimated".
                    6. Return valid JSON only.
                """.trimIndent()

                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("type", "image_url")
                                put("image_url", JSONObject().apply {
                                    put("url", "data:image/jpeg;base64,$imageBase64")
                                })
                            })
                        })
                    })
                }

                val content = requestCompletion(apiKey, messages)
                val json = JSONObject(cleanJson(content))

                val name = json.optCleanString("product_name").ifBlank { "Scanned Product" }
                val brand = json.optCleanString("brand")
                val category = json.optCleanString("category").ifBlank { "Scanned" }
                val servingSize = json.optDouble("serving_size", 100.0)
                    .takeIf { it.isFinite() && it > 0 } ?: 100.0
                val servingUnit = json.optCleanString("serving_unit").ifBlank { "g" }
                val calories = json.optDouble("calories_kcal", -1.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0
                val protein = json.optDouble("protein_g", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0
                val carbs = json.optDouble("carbs_g", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0
                val fat = json.optDouble("fat_g", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0
                val fiber = json.optDouble("fiber_g", -1.0).takeIf { it.isFinite() && it >= 0 }
                val sugar = json.optDouble("sugar_g", -1.0).takeIf { it.isFinite() && it >= 0 }
                val sodium = json.optDouble("sodium_mg", -1.0).takeIf { it.isFinite() && it >= 0 }
                val confidence = json.optCleanString("confidence").ifBlank { "unknown" }
                val notes = json.optCleanString("notes")

                val food = FoodEntity(
                    name = name,
                    brand = brand,
                    category = category,
                    servingSize = servingSize,
                    servingUnit = servingUnit,
                    calories = calories,
                    protein = protein,
                    carbohydrates = carbs,
                    fat = fat,
                    fiber = fiber,
                    sugar = sugar
                )

                val candidate = ParsedItemCandidate(
                    rawName = name,
                    matchedFood = food,
                    quantity = servingSize,
                    servingUnit = servingUnit,
                    mealType = guessMealType(),
                    isUnmatched = false,
                    calculatedCalories = calories,
                    calculatedProtein = protein,
                    calculatedCarbs = carbs,
                    calculatedFat = fat,
                    note = buildString {
                        append("Extracted from photo (confidence: $confidence)")
                        if (sodium != null) append(" • Sodium: ${sodium.toInt()} mg")
                        if (notes.isNotBlank()) append(" • $notes")
                    }
                )

                NutritionAiParseResult(
                    isSuccess = true,
                    items = listOf(candidate),
                    providerUsed = "Groq Qwen (vision)",
                    summaryText = "Extracted '$name' from photo"
                )
            } catch (e: Exception) {
                NutritionAiParseResult(
                    isSuccess = false,
                    providerUsed = "Groq",
                    errorMessage = "Could not read the image: ${e.message ?: "Unknown error"}. Try again with a clearer photo, or add the food manually."
                )
            }
        }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------
    private fun parsedProviderUsed(): String = "Groq (${modelProvider().ifBlank { "qwen/qwen3.8-27b" }})"

    private fun inventorySummaryJson(inventory: List<FoodEntity>): String {
        return JSONArray().apply {
            inventory.forEach { f ->
                put(JSONObject().apply {
                    put("id", f.id)
                    put("name", f.name)
                    put("brand", f.brand)
                    put("servingSize", f.servingSize)
                    put("servingUnit", f.servingUnit)
                    put("calories", f.calories)
                    put("protein", f.protein)
                    put("carbs", f.carbohydrates)
                    put("fat", f.fat)
                })
            }
        }.toString()
    }

    private fun requestCompletion(apiKey: String, messages: JSONArray): String {
        val model = modelProvider().ifBlank { "qwen/qwen3.8-27b" }
        val requestJson = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", 0.1)
            put("max_tokens", 1200)
            put("response_format", JSONObject().apply { put("type", "json_object") })
        }

        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IllegalStateException(
                    ApiKeyUtils.groqHttpErrorMessage(response.code, extractApiError(body))
                )
            }
            val content = JSONObject(body)
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?: ""
            if (content.isBlank()) throw IllegalStateException("Groq returned an empty response")
            return content
        }
    }

    private fun extractApiError(body: String): String? {
        return try {
            val error = JSONObject(body).optJSONObject("error")
            val message = error?.optString("message")?.takeIf { it.isNotBlank() }
            val code = error?.optString("code")?.takeIf { it.isNotBlank() }
            when {
                message != null && code != null -> "$message ($code)"
                message != null -> message
                code != null -> code
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseFoodItemsResponse(
        content: String,
        inventory: List<FoodEntity>,
        providerLabel: String
    ): NutritionAiParseResult {
        val parsedJson = JSONObject(cleanJson(content))
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
                inventoryMap[candidateIds.optLong(c)]?.let { candidateFoods.add(it) }
            }

            val matchedFood = matchedFoodId?.let { inventoryMap[it] } ?: candidateFoods.firstOrNull()

            if (matchedFood != null && !isUnmatched) {
                val calc = if (aiMultiplier > 0.0) {
                    NutritionCalculationEngine.calculateWithMultiplier(matchedFood, aiMultiplier)
                } else {
                    NutritionCalculationEngine.calculate(matchedFood, quantity, unit)
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
                        note = "Not matched in your inventory. Tap to match or add."
                    )
                )
            }
        }

        return NutritionAiParseResult(
            isSuccess = true,
            waterMl = waterMl,
            items = parsedItems,
            providerUsed = providerLabel,
            summaryText = "Parsed with Groq Qwen"
        )
    }

    /** Treats missing and JSON-null string values as empty instead of "null". */
    private fun JSONObject.optCleanString(key: String): String {
        if (!has(key) || isNull(key)) return ""
        return optString(key).trim().takeIf { it != "null" }.orEmpty()
    }

    private fun cleanJson(content: String): String {
        return content
            .replace("```json", "")
            .replace("```", "")
            .trim()
    }

    private fun guessMealType(): String {
        val hour = java.time.LocalTime.now().hour
        return when (hour) {
            in 5..10 -> "Breakfast"
            in 11..14 -> "Lunch"
            in 15..17 -> "Snack"
            in 18..22 -> "Dinner"
            else -> "Snack"
        }
    }
}
