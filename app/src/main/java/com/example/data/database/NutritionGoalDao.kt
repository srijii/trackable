package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionGoalDao {
    @Query("SELECT * FROM nutrition_goals WHERE id = 1 LIMIT 1")
    fun getGoalsFlow(): Flow<NutritionGoalEntity?>

    @Query("SELECT * FROM nutrition_goals WHERE id = 1 LIMIT 1")
    suspend fun getGoals(): NutritionGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGoals(goals: NutritionGoalEntity)
}
