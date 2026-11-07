package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getWaterLogsForDateFlow(dateString: String): Flow<List<WaterLogEntity>>

    @Query("SELECT SUM(amountMl) FROM water_logs WHERE dateString = :dateString")
    fun getTotalWaterForDateFlow(dateString: String): Flow<Int?>

    @Query("SELECT SUM(amountMl) FROM water_logs WHERE dateString = :dateString")
    suspend fun getTotalWaterForDate(dateString: String): Int?

    @Query("SELECT * FROM water_logs WHERE dateString >= :startDate AND dateString <= :endDate ORDER BY dateString ASC")
    fun getWaterLogsBetweenDatesFlow(startDate: String, endDate: String): Flow<List<WaterLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(waterLog: WaterLogEntity): Long

    @Delete
    suspend fun deleteWaterLog(waterLog: WaterLogEntity)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteWaterLogById(id: Long)
}
