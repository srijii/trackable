package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.FoodEntity
import com.example.domain.NutritionCalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NutriTrack AI", appName)
  }

  @Test
  fun `test nutrition calculation engine proportional math`() {
    // 100g paneer -> 20g protein, 281 kcal
    val paneer = FoodEntity(
      name = "Paneer",
      servingSize = 100.0,
      servingUnit = "g",
      calories = 281.0,
      protein = 20.0,
      carbohydrates = 3.0,
      fat = 21.0
    )

    // Eating 200g
    val result = NutritionCalculationEngine.calculate(paneer, 200.0, "g")
    assertEquals(40.0, result.protein, 0.01)
    assertEquals(562.0, result.calories, 0.01)
    assertEquals(6.0, result.carbohydrates, 0.01)
    assertEquals(42.0, result.fat, 0.01)
  }

  @Test
  fun `test unit normalization and conversion`() {
    val milk = FoodEntity(
      name = "Milk",
      servingSize = 100.0,
      servingUnit = "ml",
      calories = 64.0,
      protein = 3.2,
      carbohydrates = 4.8,
      fat = 3.6
    )

    // 1 liter = 1000ml -> 10x
    val result = NutritionCalculationEngine.calculate(milk, 1.0, "l")
    assertEquals(32.0, result.protein, 0.01)
    assertEquals(640.0, result.calories, 0.01)
  }
}
