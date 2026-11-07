package com.example

import com.example.data.database.FoodEntity
import com.example.domain.NutritionCalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testNutritionCalculationProportions() {
    val paneer = FoodEntity(
      name = "Paneer",
      servingSize = 100.0,
      servingUnit = "g",
      calories = 281.0,
      protein = 20.0,
      carbohydrates = 3.0,
      fat = 21.0
    )

    // Eating 200g of 100g serving: exactly double
    val calc = NutritionCalculationEngine.calculate(paneer, 200.0, "g")
    assertEquals(562.0, calc.calories, 0.01)
    assertEquals(40.0, calc.protein, 0.01)
    assertEquals(6.0, calc.carbohydrates, 0.01)
    assertEquals(42.0, calc.fat, 0.01)
    assertEquals(2.0, calc.multiplier, 0.01)
  }

  @Test
  fun testUserExampleFortySixGramsCalculation() {
    val paneer = FoodEntity(
      name = "Paneer",
      servingSize = 100.0,
      servingUnit = "g",
      calories = 281.0,
      protein = 20.0,
      carbohydrates = 3.0,
      fat = 21.0
    )

    // User prompt example: 100g serving, ate 46 gram -> multiplier = 0.46
    val calc = NutritionCalculationEngine.calculate(paneer, 46.0, "g")
    assertEquals(0.46, calc.multiplier, 0.001)
    assertEquals(129.3, calc.calories, 0.01)
    assertEquals(9.2, calc.protein, 0.01)
    assertEquals(1.4, calc.carbohydrates, 0.01)
    assertEquals(9.7, calc.fat, 0.01)

    // Using calculateWithMultiplier directly:
    val calcMultiplier = NutritionCalculationEngine.calculateWithMultiplier(paneer, 0.46)
    assertEquals(129.3, calcMultiplier.calories, 0.01)
    assertEquals(9.2, calcMultiplier.protein, 0.01)
    assertEquals(1.4, calcMultiplier.carbohydrates, 0.01)
    assertEquals(9.7, calcMultiplier.fat, 0.01)
  }

  @Test
  fun testVolumeConversions() {
    val milk = FoodEntity(
      name = "Milk",
      servingSize = 100.0,
      servingUnit = "ml",
      calories = 64.0,
      protein = 3.2,
      carbohydrates = 4.8,
      fat = 3.6
    )

    // 300 ml -> 3x
    val calc300 = NutritionCalculationEngine.calculate(milk, 300.0, "ml")
    assertEquals(192.0, calc300.calories, 0.01)
    assertEquals(9.6, calc300.protein, 0.01)

    // 1 liter = 1000 ml -> 10x
    val calcLiter = NutritionCalculationEngine.calculate(milk, 1.0, "l")
    assertEquals(640.0, calcLiter.calories, 0.01)
    assertEquals(32.0, calcLiter.protein, 0.01)
  }

  @Test
  fun testUnitNormalization() {
    assertEquals("g", NutritionCalculationEngine.normalizeUnit("grams"))
    assertEquals("g", NutritionCalculationEngine.normalizeUnit("gms"))
    assertEquals("ml", NutritionCalculationEngine.normalizeUnit("milliliters"))
    assertEquals("scoop", NutritionCalculationEngine.normalizeUnit("scoops"))
    assertEquals("piece", NutritionCalculationEngine.normalizeUnit("bananas"))
    assertEquals("piece", NutritionCalculationEngine.normalizeUnit("chapatis"))
  }
}
