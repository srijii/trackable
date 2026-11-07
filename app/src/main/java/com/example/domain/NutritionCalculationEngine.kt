package com.example.domain

import com.example.data.database.FoodEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

data class CalculatedNutrition(
    val calories: Double,
    val protein: Double,
    val carbohydrates: Double,
    val fat: Double,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val multiplier: Double
)

object NutritionCalculationEngine {

    /**
     * Normalizes unit strings (e.g., "grams" -> "g", "scoops" -> "scoop", "chapatis" -> "piece")
     */
    fun normalizeUnit(unit: String): String {
        return when (unit.trim().lowercase(Locale.ROOT)) {
            "g", "gram", "grams", "gm", "gms" -> "g"
            "kg", "kilogram", "kilograms" -> "kg"
            "ml", "milliliter", "milliliters", "mils" -> "ml"
            "l", "liter", "liters", "ltr" -> "l"
            "scoop", "scoops" -> "scoop"
            "piece", "pieces", "pc", "pcs", "item", "items", "banana", "bananas", "egg", "eggs", "roti", "rotis", "chapati", "chapatis", "slice", "slices" -> "piece"
            "cup", "cups" -> "cup"
            "tbsp", "tablespoon", "tablespoons" -> "tbsp"
            "tsp", "teaspoon", "teaspoons" -> "tsp"
            else -> unit.trim().lowercase(Locale.ROOT)
        }
    }

    /**
     * Computes the serving multiplier between consumed quantity/unit and reference servingSize/unit.
     */
    fun computeMultiplier(
        consumedQuantity: Double,
        consumedUnit: String,
        referenceServingSize: Double,
        referenceUnit: String
    ): Double {
        if (referenceServingSize <= 0.0) return 1.0

        val normConsumedUnit = normalizeUnit(consumedUnit)
        val normRefUnit = normalizeUnit(referenceUnit)

        // Same normalized unit
        if (normConsumedUnit == normRefUnit) {
            return consumedQuantity / referenceServingSize
        }

        // Mass conversions (kg -> g, g -> kg)
        if (normConsumedUnit == "kg" && normRefUnit == "g") {
            return (consumedQuantity * 1000.0) / referenceServingSize
        }
        if (normConsumedUnit == "g" && normRefUnit == "kg") {
            return (consumedQuantity / 1000.0) / referenceServingSize
        }

        // Volume conversions (l -> ml, ml -> l)
        if (normConsumedUnit == "l" && normRefUnit == "ml") {
            return (consumedQuantity * 1000.0) / referenceServingSize
        }
        if (normConsumedUnit == "ml" && normRefUnit == "l") {
            return (consumedQuantity / 1000.0) / referenceServingSize
        }

        // Approximate units if user specifies cups/spoons on gram-based foods
        if (normConsumedUnit == "tbsp" && normRefUnit == "g") {
            // Standard tbsp ~ 15g
            return (consumedQuantity * 15.0) / referenceServingSize
        }
        if (normConsumedUnit == "tsp" && normRefUnit == "g") {
            // Standard tsp ~ 5g
            return (consumedQuantity * 5.0) / referenceServingSize
        }

        // Fallback ratio directly
        return consumedQuantity / referenceServingSize
    }

    /**
     * Main formula: Nutrient consumed = (Quantity consumed / Reference quantity) * Nutrient value
     */
    fun calculate(
        food: FoodEntity,
        consumedQuantity: Double,
        consumedUnit: String
    ): CalculatedNutrition {
        val multiplier = computeMultiplier(
            consumedQuantity = consumedQuantity,
            consumedUnit = consumedUnit,
            referenceServingSize = food.servingSize,
            referenceUnit = food.servingUnit
        )

        return CalculatedNutrition(
            calories = roundToOneDecimal(food.calories * multiplier),
            protein = roundToOneDecimal(food.protein * multiplier),
            carbohydrates = roundToOneDecimal(food.carbohydrates * multiplier),
            fat = roundToOneDecimal(food.fat * multiplier),
            fiber = food.fiber?.let { roundToOneDecimal(it * multiplier) },
            sugar = food.sugar?.let { roundToOneDecimal(it * multiplier) },
            multiplier = multiplier
        )
    }

    fun calculateWithMultiplier(
        food: FoodEntity,
        multiplier: Double
    ): CalculatedNutrition {
        return CalculatedNutrition(
            calories = roundToOneDecimal(food.calories * multiplier),
            protein = roundToOneDecimal(food.protein * multiplier),
            carbohydrates = roundToOneDecimal(food.carbohydrates * multiplier),
            fat = roundToOneDecimal(food.fat * multiplier),
            fiber = food.fiber?.let { roundToOneDecimal(it * multiplier) },
            sugar = food.sugar?.let { roundToOneDecimal(it * multiplier) },
            multiplier = multiplier
        )
    }

    fun roundToOneDecimal(value: Double): Double {
        return BigDecimal(value.toString())
            .setScale(1, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun roundToFourDecimals(value: Double): Double {
        return BigDecimal(value.toString())
            .setScale(4, RoundingMode.HALF_UP)
            .toDouble()
    }
}
