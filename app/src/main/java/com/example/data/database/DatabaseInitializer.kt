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
     * Default data seeded into a fresh database: none.
     * The food inventory is intentionally left empty so the user only has foods
     * they've added themselves. No default goals, food logs, water logs or
     * weight logs are added either.
     */
    suspend fun populateInitialData(database: AppDatabase) = withContext(Dispatchers.IO) {
        // No default foods are seeded on install. Start with an empty inventory
        // so the user only has foods they've added themselves. Any foods that
        // already exist (e.g. across an app update/reinstall) are preserved
        // because nothing is inserted here and ensureInitialized() only seeds
        // when the food count is zero.
    }
}
