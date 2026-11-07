package com.example.ai

import com.example.data.database.FoodEntity
import com.example.domain.NutritionCalculationEngine
import java.time.LocalTime
import java.util.Locale
import java.util.regex.Pattern

class LocalRuleBasedNutritionAIService : NutritionAIService {

    override val providerName: String = "Local Engine"

    override suspend fun parseFoodInput(
        userInput: String,
        inventory: List<FoodEntity>
    ): NutritionAiParseResult {
        val trimmed = userInput.trim()
        if (trimmed.isEmpty()) {
            return NutritionAiParseResult(
                isSuccess = false,
                errorMessage = "Input is empty"
            )
        }

        var detectedWaterMl = 0
        val items = mutableListOf<ParsedItemCandidate>()

        // 1. Detect and extract water consumption
        val waterPattern = Pattern.compile(
            """(?i)(\d+(?:\.\d+)?)\s*(ml|milliliters?|l|liters?|glass(?:es)?|cups?)\s*(?:of\s*)?water|water\s*(\d+(?:\.\d+)?)\s*(ml|milliliters?|l|liters?)"""
        )
        val waterMatcher = waterPattern.matcher(trimmed)
        var textWithoutWater = trimmed
        while (waterMatcher.find()) {
            val amountStr = waterMatcher.group(1) ?: waterMatcher.group(3)
            val unitStr = (waterMatcher.group(2) ?: waterMatcher.group(4) ?: "ml").lowercase(Locale.ROOT)
            val amount = amountStr?.toDoubleOrNull() ?: 1.0

            val ml = when {
                unitStr.startsWith("l") && !unitStr.startsWith("glass") -> (amount * 1000).toInt()
                unitStr.startsWith("glass") -> (amount * 250).toInt()
                unitStr.startsWith("cup") -> (amount * 240).toInt()
                else -> amount.toInt()
            }
            detectedWaterMl += ml
            textWithoutWater = textWithoutWater.replace(waterMatcher.group(0) ?: "", " ")
        }

        // Also check simple "drank water" or "glass of water"
        if (detectedWaterMl == 0 && textWithoutWater.contains("water", ignoreCase = true)) {
            val glassMatch = Pattern.compile("""(?i)(a|one|two|three|\d+)?\s*(?:glass(?:es)?|bottle(?:s)?)\s*(?:of\s*)?water""").matcher(textWithoutWater)
            if (glassMatch.find()) {
                val countWord = glassMatch.group(1)?.lowercase(Locale.ROOT) ?: "1"
                val count = wordToNumber(countWord)
                detectedWaterMl = (count * 250).toInt()
                textWithoutWater = textWithoutWater.replace(glassMatch.group(0) ?: "", " ")
            }
        }

        // 2. Determine meal type from context or time of day
        val defaultMealType = guessMealType(trimmed)

        // 3. Split remaining input into clauses (comma, 'and', 'with', 'plus')
        val clauses = textWithoutWater
            .split(Regex("(?i)\\b(?:and|with|plus|also)\\b|,"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (clause in clauses) {
            val candidate = parseClause(clause, inventory, defaultMealType)
            if (candidate != null) {
                items.add(candidate)
            }
        }

        return NutritionAiParseResult(
            isSuccess = true,
            waterMl = detectedWaterMl,
            items = items,
            providerUsed = providerName,
            summaryText = "Parsed ${items.size} food item(s) and $detectedWaterMl ml water"
        )
    }

    private fun parseClause(
        clause: String,
        inventory: List<FoodEntity>,
        defaultMealType: String
    ): ParsedItemCandidate? {
        // Strip common lead-in words: "I ate", "I had", "I drank", "drank", "ate", "had", "my", "some"
        val cleaned = clause
            .replace(Regex("(?i)^\\s*(?:i\\s+)?(?:ate|had|drank|consumed|have|eat|drink)\\s+"), "")
            .replace(Regex("(?i)\\b(?:my|some|a|an)\\b"), " ")
            .trim()

        if (cleaned.isBlank() || cleaned.equals("water", ignoreCase = true)) {
            return null
        }

        // Regex to extract quantity, unit, and food descriptor
        // E.g. "200g paneer", "2 bananas", "one scoop of whey protein", "100g cooked rice"
        val regex = Pattern.compile(
            """(?i)^\s*(\d+(?:\.\d+)?|one|two|three|four|five|half|a\s+quarter)?\s*(g|grams?|gm|gms?|kg|ml|milliliters?|l|liters?|scoops?|pieces?|pcs?|items?|slices?|cups?|tbsp|tsp|chapatis?|rotis?|bananas?|eggs?)?\s*(?:of\s+)?(.+)"""
        )
        val matcher = regex.matcher(cleaned)

        var quantity = 1.0
        var unit = "piece"
        var foodNameQuery = cleaned

        if (matcher.find()) {
            val qStr = matcher.group(1)
            val uStr = matcher.group(2)
            val nameStr = matcher.group(3)

            if (!qStr.isNullOrBlank()) {
                quantity = wordToNumber(qStr)
            }
            if (!uStr.isNullOrBlank()) {
                unit = NutritionCalculationEngine.normalizeUnit(uStr)
            }
            if (!nameStr.isNullOrBlank()) {
                foodNameQuery = nameStr.trim()
            }
        }

        // Clean up food name query: remove trailing/leading punctuation
        foodNameQuery = foodNameQuery.trim(' ', '.', ',', ';', '!', '?')
        if (foodNameQuery.isBlank()) return null

        // Match against inventory
        val matches = findMatchingFoods(foodNameQuery, inventory)

        return when {
            matches.isEmpty() -> {
                // Not found in inventory: ask user to select or create a food entry
                ParsedItemCandidate(
                    rawName = foodNameQuery,
                    matchedFood = null,
                    quantity = quantity,
                    servingUnit = unit,
                    mealType = defaultMealType,
                    isAmbiguous = false,
                    isUnmatched = true,
                    calculatedCalories = 0.0,
                    calculatedProtein = 0.0,
                    calculatedCarbs = 0.0,
                    calculatedFat = 0.0,
                    note = "Not found in your food inventory. Tap to match or create."
                )
            }
            matches.size == 1 -> {
                val matched = matches.first()
                val calculated = NutritionCalculationEngine.calculate(
                    food = matched,
                    consumedQuantity = quantity,
                    consumedUnit = if (unit == "piece" && matched.servingUnit != "piece") matched.servingUnit else unit
                )
                ParsedItemCandidate(
                    rawName = foodNameQuery,
                    matchedFood = matched,
                    quantity = quantity,
                    servingUnit = if (unit == "piece" && matched.servingUnit != "piece") matched.servingUnit else unit,
                    mealType = defaultMealType,
                    isAmbiguous = false,
                    isUnmatched = false,
                    calculatedCalories = calculated.calories,
                    calculatedProtein = calculated.protein,
                    calculatedCarbs = calculated.carbohydrates,
                    calculatedFat = calculated.fat
                )
            }
            else -> {
                // Multiple matches (e.g. Whey Protein A vs Whey Protein B) -> Ambiguous!
                val firstMatch = matches.first()
                val calculated = NutritionCalculationEngine.calculate(
                    food = firstMatch,
                    consumedQuantity = quantity,
                    consumedUnit = if (unit == "piece" && firstMatch.servingUnit != "piece") firstMatch.servingUnit else unit
                )
                ParsedItemCandidate(
                    rawName = foodNameQuery,
                    matchedFood = firstMatch,
                    quantity = quantity,
                    servingUnit = if (unit == "piece" && firstMatch.servingUnit != "piece") firstMatch.servingUnit else unit,
                    mealType = defaultMealType,
                    isAmbiguous = true,
                    candidateFoods = matches,
                    isUnmatched = false,
                    calculatedCalories = calculated.calories,
                    calculatedProtein = calculated.protein,
                    calculatedCarbs = calculated.carbohydrates,
                    calculatedFat = calculated.fat,
                    note = "Multiple matching foods found in your inventory. Please confirm which one."
                )
            }
        }
    }

    private fun findMatchingFoods(query: String, inventory: List<FoodEntity>): List<FoodEntity> {
        val q = query.lowercase(Locale.ROOT).trim()

        // 1. Exact match on name
        val exactMatches = inventory.filter { it.name.lowercase(Locale.ROOT) == q }
        if (exactMatches.isNotEmpty()) return exactMatches

        // 2. Token / Substring matching
        val candidates = inventory.filter { food ->
            val foodName = food.name.lowercase(Locale.ROOT)
            val brand = food.brand.lowercase(Locale.ROOT)

            foodName.contains(q) || q.contains(foodName) ||
                (brand.isNotBlank() && q.contains(brand)) ||
                sharesSignificantWords(q, foodName)
        }

        return candidates
    }

    private fun sharesSignificantWords(query: String, target: String): Boolean {
        val stopWords = setOf("a", "the", "of", "and", "with", "curry", "cooked", "fresh", "homemade")
        val qWords = query.split(Regex("\\s+")).map { it.trim() }.filter { it.length > 2 && it !in stopWords }
        val tWords = target.split(Regex("\\s+")).map { it.trim() }.filter { it.length > 2 && it !in stopWords }

        if (qWords.isEmpty()) return false
        return qWords.any { qw -> tWords.any { tw -> tw.contains(qw) || qw.contains(tw) } }
    }

    private fun wordToNumber(str: String): Double {
        return when (str.lowercase(Locale.ROOT).trim()) {
            "one", "a" -> 1.0
            "two" -> 2.0
            "three" -> 3.0
            "four" -> 4.0
            "five" -> 5.0
            "six" -> 6.0
            "half" -> 0.5
            "a quarter" -> 0.25
            else -> str.toDoubleOrNull() ?: 1.0
        }
    }

    private fun guessMealType(input: String): String {
        val lower = input.lowercase(Locale.ROOT)
        return when {
            lower.contains("breakfast") -> "Breakfast"
            lower.contains("lunch") -> "Lunch"
            lower.contains("dinner") -> "Dinner"
            lower.contains("snack") -> "Snack"
            else -> {
                val hour = LocalTime.now().hour
                when (hour) {
                    in 5..10 -> "Breakfast"
                    in 11..14 -> "Lunch"
                    in 15..17 -> "Snack"
                    in 18..22 -> "Dinner"
                    else -> "Snack"
                }
            }
        }
    }
}
