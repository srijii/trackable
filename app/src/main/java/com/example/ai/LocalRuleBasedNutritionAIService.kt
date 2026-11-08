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
        val preprocessed = preprocessInput(userInput)
        val trimmed = preprocessed.trim()
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
                val countWord = glassMatch.group(1) ?: "1"
                val count = wordToNumber(countWord)
                detectedWaterMl = (count * 250).toInt()
                textWithoutWater = textWithoutWater.replace(glassMatch.group(0) ?: "", " ")
            }
        }

        // 2. Determine meal type from context or time of day
        val defaultMealType = guessMealType(trimmed)

        // 3. Split remaining input into clauses (comma, 'and', 'with', 'plus', punctuation)
        val clauses = textWithoutWater
            .split(Regex("(?i)\\b(?:and|with|plus|also)\\b|[,;.•]"))
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

    /**
     * Preprocess raw user input to handle broken/messy formats:
     * - "50 g" -> "50g", "200 m l" -> "200ml"
     * - Removes extra spaces, normalizes punctuation
     */
    private fun preprocessInput(input: String): String {
        var result = input.lowercase(Locale.ROOT)

        // Remove extra whitespace
        result = result.replace(Regex("\\s+"), " ").trim()

        // Fix unit spacing: "50 g" -> "50g", "200 ml" -> "200ml", "1 cup" -> "1cup"
        result = result.replace(Regex("""(\d)\s+([a-zA-Z])"""), "$1$2")

        // Fix split unit parts: "200m l" -> "200ml", "50gms" stays
        result = result.replace(Regex("""(\d+)m\s*l"""), "$1ml")
        result = result.replace(Regex("""(\d+)g\s*m\s*s"""), "$1gms")

        // Normalize punctuation: replace multiple commas/dots with single comma
        result = result.replace(Regex("[,;.•]+"), ", ")
        result = result.replace(Regex("\\s+,"), ",")
        result = result.replace(Regex(",\\s+"), ", ")
        result = result.replace(Regex("\\s+"), " ")

        return result.trim()
    }

    private fun parseClause(
        clause: String,
        inventory: List<FoodEntity>,
        defaultMealType: String
    ): ParsedItemCandidate? {
        var cleaned = clause
            .replace(Regex("(?i)^\\s*(?:i\\s+)?(?:ate|had|drank|consumed|have|eat|drink)\\s+"), "")
            .replace(Regex("(?i)\\b(?:my|some|a|an)\\b"), " ")
            .trim()

        if (cleaned.isBlank() || cleaned.equals("water", ignoreCase = true)) {
            return null
        }

        // Handle patterns like "paneer 50,60" where quantity comes after food with comma
        var quantitySpecifiedExternally = false
        var externalQuantity = 1.0
        var externalUnit = "piece"

        val commaParts = clause.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (commaParts.size >= 2) {
            val part0 = commaParts[0]
            val part1 = commaParts[1]
            val qtyPattern = Pattern.compile("""(?i)^(\d+(?:\.\d+)?)\s*(g|ml|mg|kg|l|oz|lb|cup|scoop|piece|slice|tbsp|tsp|banana|egg|chapati|roti)?$""")
            val m0 = qtyPattern.matcher(part0)
            val m1 = qtyPattern.matcher(part1)
            if (m1.find() && !m0.find()) {
                cleaned = part0
                externalQuantity = m1.group(1)?.toDoubleOrNull() ?: 1.0
                externalUnit = m1.group(2) ?: "piece"
                quantitySpecifiedExternally = true
            } else if (m0.find() && !m1.find()) {
                cleaned = part1
                externalQuantity = m0.group(1)?.toDoubleOrNull() ?: 1.0
                externalUnit = m0.group(2) ?: "piece"
                quantitySpecifiedExternally = true
            }
        }

        var quantity = 1.0
        var unit = "piece"
        var foodNameQuery = cleaned

        if (!quantitySpecifiedExternally) {
            // Regex to extract quantity, unit, and food descriptor
            val regex = Pattern.compile(
                """(?i)^\s*(\d+(?:\.\d+)?|one|two|three|four|five|half|a\s+quarter)?\s*(g|grams?|gm|gms?|kg|ml|milliliters?|l|liters?|scoops?|pieces?|pcs?|items?|slices?|cups?|tbsp|tsp|chapatis?|rotis?|bananas?|eggs?|glass(?:es)?|cups?|bottle(?:s)?)?\s*(?:of\s+)?(.+)"""
            )
            val matcher = regex.matcher(cleaned)

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
                } else if (!qStr.isNullOrBlank() && uStr.isNullOrBlank()) {
                    foodNameQuery = cleaned.trim()
                }
            }

            foodNameQuery = foodNameQuery.trim(' ', '.', ',', ';', '!', '?')
            if (foodNameQuery.isBlank()) return null

            quantity = quantity.coerceAtLeast(0.1)
        } else {
            foodNameQuery = cleaned.trim(' ', '.', ',', ';', '!', '?')
            if (foodNameQuery.isBlank()) return null
            quantity = externalQuantity.coerceAtLeast(0.1)
            unit = externalUnit
        }

        // Match against inventory with fuzzy/typo tolerance
        val matches = findMatchingFoods(foodNameQuery, inventory)

        return when {
            matches.isEmpty() -> {
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

    /**
     * Find matching foods with fuzzy/typo tolerance using Levenshtein distance.
     * Handles: "paner" -> "paneer", "milkk" -> "milk", etc.
     */
    private fun findMatchingFoods(query: String, inventory: List<FoodEntity>): List<FoodEntity> {
        val q = query.lowercase(Locale.ROOT).trim()
        if (q.isEmpty()) return emptyList()

        // 1. Exact match on name
        val exactMatches = inventory.filter { it.name.lowercase(Locale.ROOT) == q }
        if (exactMatches.isNotEmpty()) return exactMatches

        // 2. Substring matching (query contained in food name or vice versa)
        val substringMatches = inventory.filter { food ->
            val foodName = food.name.lowercase(Locale.ROOT)
            val brand = food.brand.lowercase(Locale.ROOT)
            foodName.contains(q) || q.contains(foodName) ||
                (brand.isNotBlank() && q.contains(brand))
        }
        if (substringMatches.isNotEmpty()) return substringMatches

        // 3. Fuzzy match using Levenshtein distance (typo tolerance)
        val fuzzyMatches = inventory.mapNotNull { food ->
            val foodName = food.name.lowercase(Locale.ROOT)
            val dist = levenshtein(q, foodName)
            val maxAllowed = when {
                foodName.length <= 4 -> 1
                foodName.length <= 8 -> 2
                else -> maxOf(2, (foodName.length * 0.25).toInt())
            }
            if (dist <= maxAllowed) food else null
        }
        if (fuzzyMatches.isNotEmpty()) return fuzzyMatches

        // 4. Word-based matching (shares significant words)
        val wordMatches = inventory.filter { food ->
            sharesSignificantWords(q, food.name.lowercase(Locale.ROOT))
        }
        if (wordMatches.isNotEmpty()) return wordMatches

        return emptyList()
    }

    /**
     * Levenshtein edit distance for typo-tolerant matching.
     */
    private fun levenshtein(s1: String, s2: String): Int {
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = when {
                    s1[i - 1] == s2[j - 1] -> dp[i - 1][j - 1]
                    else -> 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun sharesSignificantWords(query: String, target: String): Boolean {
        val stopWords = setOf("a", "the", "of", "and", "with", "curry", "cooked", "fresh", "homemade")
        val qWords = query.split(Regex("\\s+")).map { it.trim() }.filter { it.length > 2 && it !in stopWords }
        val tWords = target.split(Regex("\\s+")).map { it.trim() }.filter { it.length > 2 && it !in stopWords }

        if (qWords.isEmpty()) return false
        return qWords.any { qw ->
            tWords.any { tw ->
                tw.contains(qw) || qw.contains(tw) || levenshtein(qw, tw) <= 1
            }
        }
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
