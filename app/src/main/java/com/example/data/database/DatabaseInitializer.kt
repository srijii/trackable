package com.example.data.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseInitializer {

    suspend fun ensureInitialized(database: AppDatabase) = withContext(Dispatchers.IO) {
        if (database.foodDao().getFoodCount() == 0) {
            populateInitialData(database)
        }
    }

    /**
     * The ONLY default data seeded into a fresh database: the food inventory.
     * No default goals, food logs, water logs or weight logs are added.
     */
    suspend fun populateInitialData(database: AppDatabase) = withContext(Dispatchers.IO) {
        val initialFoods = listOf(
            // ---------------------------------------------------------------
            // 1. High-protein vegetarian foods (Best for muscle gain)
            // Serving is per 100g unless noted otherwise.
            // Note: Dal and legumes figures above are dry-weight, except sprouts.
            // ---------------------------------------------------------------
            FoodEntity(
                name = "Soya chunks",
                brand = "Packaged / Nutrela-type",
                category = "High-Protein Vegetarian",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 345.0,
                protein = 52.0,
                carbohydrates = 33.0,
                fat = 0.5,
                fiber = 13.0,
                sugar = 3.0
            ),
            FoodEntity(
                name = "Paneer",
                brand = "Dairy / Fresh",
                category = "High-Protein Vegetarian",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 305.0,
                protein = 18.9,
                carbohydrates = 1.2,
                fat = 24.8,
                fiber = 0.0,
                sugar = 1.2
            ),
            FoodEntity(
                name = "Peanuts",
                brand = "Fresh / Raw",
                category = "High-Protein Vegetarian",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 567.0,
                protein = 25.8,
                carbohydrates = 16.1,
                fat = 49.2,
                fiber = 8.5,
                sugar = 4.7
            ),
            FoodEntity(
                name = "Moong dal",
                brand = "Packaged / Dry",
                category = "High-Protein Vegetarian",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 326.0,
                protein = 23.9,
                carbohydrates = 56.0,
                fat = 1.2,
                fiber = 8.0,
                sugar = 6.0
            ),
            FoodEntity(
                name = "Masoor dal",
                brand = "Packaged / Dry",
                category = "High-Protein Vegetarian",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 322.0,
                protein = 24.4,
                carbohydrates = 56.0,
                fat = 1.5,
                fiber = 11.0,
                sugar = 2.0
            ),
            FoodEntity(
                name = "Chana dal",
                brand = "Packaged / Dry",
                category = "High-Protein Vegetarian",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 329.0,
                protein = 21.6,
                carbohydrates = 58.0,
                fat = 5.3,
                fiber = 12.0,
                sugar = 2.0
            ),
            FoodEntity(
                name = "Toor dal",
                brand = "Packaged / Dry",
                category = "High-Protein Vegetarian",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 331.0,
                protein = 21.7,
                carbohydrates = 58.0,
                fat = 1.5,
                fiber = 6.0,
                sugar = 3.0
            ),
            FoodEntity(
                name = "Black chana",
                brand = "Fresh / Dry",
                category = "High-Protein Vegetarian",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 287.0,
                protein = 18.8,
                carbohydrates = 52.0,
                fat = 5.0,
                fiber = 12.0,
                sugar = 10.0
            ),
            FoodEntity(
                name = "Rajma",
                brand = "Packaged / Dry",
                category = "High-Protein Vegetarian",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 299.0,
                protein = 19.9,
                carbohydrates = 60.0,
                fat = 1.2,
                fiber = 15.0,
                sugar = 2.0
            ),
            FoodEntity(
                name = "Green gram",
                brand = "Fresh / Dry",
                category = "High-Protein Vegetarian",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 294.0,
                protein = 22.5,
                carbohydrates = 56.0,
                fat = 1.2,
                fiber = 16.0,
                sugar = 7.0
            ),
            FoodEntity(
                name = "Besan",
                brand = "Packaged / Gram flour",
                category = "High-Protein Vegetarian",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 387.0,
                protein = 22.0,
                carbohydrates = 58.0,
                fat = 6.7,
                fiber = 11.0,
                sugar = 10.0
            ),
            FoodEntity(
                name = "Sprouted green gram",
                brand = "Fresh / Sprouts",
                category = "High-Protein Vegetarian",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 30.0,
                protein = 3.0,
                carbohydrates = 6.0,
                fat = 0.2,
                fiber = 1.8,
                sugar = 4.0
            ),

            // ---------------------------------------------------------------
            // Dairy products
            // ---------------------------------------------------------------
            FoodEntity(
                name = "Cow milk",
                brand = "Dairy / Fresh",
                category = "Dairy",
                servingSize = 100.0,
                servingUnit = "ml",
                calories = 61.0,
                protein = 3.3,
                carbohydrates = 4.8,
                fat = 3.3,
                fiber = 0.0,
                sugar = 4.8
            ),
            FoodEntity(
                name = "Buffalo milk",
                brand = "Dairy / Fresh",
                category = "Dairy",
                servingSize = 100.0,
                servingUnit = "ml",
                calories = 97.0,
                protein = 3.7,
                carbohydrates = 5.2,
                fat = 6.9,
                fiber = 0.0,
                sugar = 5.2
            ),
            FoodEntity(
                name = "Curd",
                brand = "Dairy / Homemade",
                category = "Dairy",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 61.0,
                protein = 3.5,
                carbohydrates = 4.7,
                fat = 3.3,
                fiber = 0.0,
                sugar = 4.7
            ),
            FoodEntity(
                name = "Greek yogurt",
                brand = "Dairy / Packaged",
                category = "Dairy",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 97.0,
                protein = 9.0,
                carbohydrates = 4.0,
                fat = 5.0,
                fiber = 0.0,
                sugar = 4.0
            ),
            FoodEntity(
                name = "Buttermilk",
                brand = "Dairy / Fresh",
                category = "Dairy",
                servingSize = 100.0,
                servingUnit = "ml",
                calories = 40.0,
                protein = 1.5,
                carbohydrates = 4.0,
                fat = 1.5,
                fiber = 0.0,
                sugar = 4.0
            ),
            FoodEntity(
                name = "Ghee",
                brand = "Dairy / Packaged",
                category = "Dairy",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 900.0,
                protein = 0.0,
                carbohydrates = 0.0,
                fat = 100.0,
                fiber = 0.0,
                sugar = 0.0
            ),
            FoodEntity(
                name = "Cheese",
                brand = "Dairy / Packaged",
                category = "Dairy",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 350.0,
                protein = 22.0,
                carbohydrates = 3.0,
                fat = 27.0,
                fiber = 0.0,
                sugar = 1.0
            ),

            // ---------------------------------------------------------------
            // 3. Grains and carbohydrates (Useful for calorie surplus)
            // ---------------------------------------------------------------
            FoodEntity(
                name = "White rice",
                brand = "Fresh / Raw",
                category = "Grains & Carbs",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 356.0,
                protein = 7.9,
                carbohydrates = 78.0,
                fat = 0.5,
                fiber = 1.0,
                sugar = 0.1
            ),
            FoodEntity(
                name = "White rice",
                brand = "Fresh / Cooked",
                category = "Grains & Carbs",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 129.0,
                protein = 2.7,
                carbohydrates = 28.0,
                fat = 0.3,
                fiber = 0.4,
                sugar = 0.1
            ),
            FoodEntity(
                name = "Oats",
                brand = "Packaged / Quaker-type",
                category = "Grains & Carbs",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 379.0,
                protein = 13.2,
                carbohydrates = 67.7,
                fat = 6.5,
                fiber = 10.1,
                sugar = 0.9
            ),
            FoodEntity(
                name = "Wheat flour",
                brand = "Fresh / Atta",
                category = "Grains & Carbs",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 320.0,
                protein = 10.6,
                carbohydrates = 71.0,
                fat = 1.5,
                fiber = 11.0,
                sugar = 0.5
            ),
            FoodEntity(
                name = "Ragi flour",
                brand = "Fresh / Flour",
                category = "Grains & Carbs",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 336.0,
                protein = 7.3,
                carbohydrates = 72.0,
                fat = 1.5,
                fiber = 11.0,
                sugar = 1.0
            ),
            FoodEntity(
                name = "Puttu podi",
                brand = "Packaged / Rice flour",
                category = "Grains & Carbs",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 360.0,
                protein = 6.5,
                carbohydrates = 80.0,
                fat = 1.0,
                fiber = 2.0,
                sugar = 0.5
            ),
            FoodEntity(
                name = "Bread",
                brand = "Packaged / Whole wheat",
                category = "Grains & Carbs",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 254.0,
                protein = 12.3,
                carbohydrates = 43.1,
                fat = 3.5,
                fiber = 6.0,
                sugar = 5.0
            ),
            FoodEntity(
                name = "Sweet potato",
                brand = "Fresh / Raw",
                category = "Grains & Carbs",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 86.0,
                protein = 1.6,
                carbohydrates = 20.1,
                fat = 0.1,
                fiber = 3.0,
                sugar = 4.2
            ),
            FoodEntity(
                name = "Potato",
                brand = "Fresh / Raw",
                category = "Grains & Carbs",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 77.0,
                protein = 2.0,
                carbohydrates = 17.5,
                fat = 0.1,
                fiber = 2.2,
                sugar = 0.8
            ),

            // ---------------------------------------------------------------
            // Fruits
            // ---------------------------------------------------------------
            FoodEntity(
                name = "Banana",
                brand = "Fresh / Nendran-type",
                category = "Fruits",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 116.0,
                protein = 1.2,
                carbohydrates = 27.0,
                fat = 0.3,
                fiber = 2.6,
                sugar = 15.0
            ),
            FoodEntity(
                name = "Apple",
                brand = "Fresh",
                category = "Fruits",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 52.0,
                protein = 0.3,
                carbohydrates = 14.0,
                fat = 0.2,
                fiber = 2.4,
                sugar = 10.4
            ),
            FoodEntity(
                name = "Mango",
                brand = "Fresh",
                category = "Fruits",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 60.0,
                protein = 0.8,
                carbohydrates = 15.0,
                fat = 0.4,
                fiber = 1.6,
                sugar = 13.7
            ),
            FoodEntity(
                name = "Papaya",
                brand = "Fresh",
                category = "Fruits",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 43.0,
                protein = 0.5,
                carbohydrates = 11.0,
                fat = 0.3,
                fiber = 1.7,
                sugar = 7.8
            ),
            FoodEntity(
                name = "Dates",
                brand = "Fresh / Dried",
                category = "Fruits",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 282.0,
                protein = 2.5,
                carbohydrates = 75.0,
                fat = 0.4,
                fiber = 8.0,
                sugar = 63.0
            ),
            FoodEntity(
                name = "Orange",
                brand = "Fresh",
                category = "Fruits",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 47.0,
                protein = 0.9,
                carbohydrates = 11.8,
                fat = 0.1,
                fiber = 2.4,
                sugar = 9.4
            ),
            FoodEntity(
                name = "Avocado",
                brand = "Fresh",
                category = "Fruits",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 160.0,
                protein = 2.0,
                carbohydrates = 8.5,
                fat = 14.7,
                fiber = 6.7,
                sugar = 0.7
            ),

            // ---------------------------------------------------------------
            // Nuts and seeds
            // ---------------------------------------------------------------
            FoodEntity(
                name = "Almonds",
                brand = "Packaged / Raw",
                category = "Nuts & Seeds",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 579.0,
                protein = 21.2,
                carbohydrates = 21.6,
                fat = 49.9,
                fiber = 12.5,
                sugar = 4.4
            ),
            FoodEntity(
                name = "Cashews",
                brand = "Packaged / Raw",
                category = "Nuts & Seeds",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 553.0,
                protein = 18.2,
                carbohydrates = 30.2,
                fat = 43.9,
                fiber = 3.3,
                sugar = 5.9
            ),
            FoodEntity(
                name = "Walnuts",
                brand = "Packaged / Raw",
                category = "Nuts & Seeds",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 654.0,
                protein = 15.2,
                carbohydrates = 13.7,
                fat = 65.2,
                fiber = 6.7,
                sugar = 2.6
            ),
            FoodEntity(
                name = "Sesame seeds",
                brand = "Fresh / Raw",
                category = "Nuts & Seeds",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 573.0,
                protein = 17.7,
                carbohydrates = 23.4,
                fat = 49.7,
                fiber = 11.8,
                sugar = 0.3
            ),
            FoodEntity(
                name = "Pumpkin seeds",
                brand = "Packaged / Raw",
                category = "Nuts & Seeds",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 559.0,
                protein = 30.0,
                carbohydrates = 11.0,
                fat = 49.0,
                fiber = 6.0,
                sugar = 1.0
            ),
            FoodEntity(
                name = "Flax seeds",
                brand = "Packaged / Raw",
                category = "Nuts & Seeds",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 534.0,
                protein = 18.3,
                carbohydrates = 28.9,
                fat = 42.2,
                fiber = 27.3,
                sugar = 1.6
            ),
            FoodEntity(
                name = "Sunflower seeds",
                brand = "Packaged / Raw",
                category = "Nuts & Seeds",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 584.0,
                protein = 20.8,
                carbohydrates = 20.0,
                fat = 51.5,
                fiber = 8.6,
                sugar = 2.6
            ),

            // ---------------------------------------------------------------
            // Supplements and packaged products
            // ---------------------------------------------------------------
            FoodEntity(
                name = "Whey concentrate",
                brand = "Supplement",
                category = "Supplements",
                servingSize = 30.0,
                servingUnit = "g",
                calories = 120.0,
                protein = 24.0,
                carbohydrates = 3.0,
                fat = 2.0,
                fiber = 0.0,
                sugar = 2.0
            ),
            FoodEntity(
                name = "Whey isolate",
                brand = "Supplement",
                category = "Supplements",
                servingSize = 30.0,
                servingUnit = "g",
                calories = 110.0,
                protein = 27.0,
                carbohydrates = 1.0,
                fat = 0.5,
                fiber = 0.0,
                sugar = 0.5
            ),
            FoodEntity(
                name = "Mass gainer",
                brand = "Supplement",
                category = "Supplements",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 375.0,
                protein = 17.0,
                carbohydrates = 75.0,
                fat = 2.0,
                fiber = 2.0,
                sugar = 15.0
            ),
            FoodEntity(
                name = "Plant protein",
                brand = "Supplement",
                category = "Supplements",
                servingSize = 30.0,
                servingUnit = "g",
                calories = 120.0,
                protein = 24.0,
                carbohydrates = 3.0,
                fat = 2.0,
                fiber = 1.0,
                sugar = 1.0
            ),
            FoodEntity(
                name = "Peanut butter",
                brand = "Packaged / Pintola-type",
                category = "Supplements",
                servingSize = 100.0,
                servingUnit = "g",
                calories = 588.0,
                protein = 25.0,
                carbohydrates = 20.0,
                fat = 50.0,
                fiber = 6.0,
                sugar = 9.0
            ),
            FoodEntity(
                name = "Protein bar",
                brand = "Packaged",
                category = "Supplements",
                servingSize = 60.0,
                servingUnit = "g",
                calories = 220.0,
                protein = 20.0,
                carbohydrates = 22.0,
                fat = 7.0,
                fiber = 5.0,
                sugar = 5.0
            )
        )
        database.foodDao().insertAllFoods(initialFoods)
    }
}
