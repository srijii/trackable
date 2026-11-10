package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_logs WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getLogsForDateFlow(dateString: String): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE dateString = :dateString ORDER BY timestamp DESC")
    suspend fun getLogsForDate(dateString: String): List<FoodLogEntity>

    @Query("SELECT * FROM food_logs WHERE dateString >= :startDate AND dateString <= :endDate ORDER BY dateString ASC, timestamp ASC")
    fun getLogsBetweenDatesFlow(startDate: String, endDate: String): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE dateString >= :startDate AND dateString <= :endDate ORDER BY dateString ASC, timestamp ASC")
    suspend fun getLogsBetweenDates(startDate: String, endDate: String): List<FoodLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FoodLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<FoodLogEntity>): List<Long>

    @Update
    suspend fun updateLog(log: FoodLogEntity)

    @Delete
    suspend fun deleteLog(log: FoodLogEntity)

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("SELECT * FROM food_logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: Long): FoodLogEntity?

    @Query("SELECT * FROM food_logs ORDER BY timestamp ASC")
    suspend fun getAllLogsForBackup(): List<FoodLogEntity>

    @Query("SELECT COUNT(*) FROM food_logs")
    suspend fun getLogCount(): Int

    @Query("DELETE FROM food_logs")
    suspend fun deleteAllLogs()
}
