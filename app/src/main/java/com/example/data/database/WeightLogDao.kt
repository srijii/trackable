package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(log: WeightLogEntity): Long

    @Query("SELECT * FROM weight_logs ORDER BY dateEpochDay ASC")
    fun getAllWeightLogsAscFlow(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs ORDER BY dateEpochDay DESC")
    fun getAllWeightLogsDescFlow(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getWeightLogForDate(dateEpochDay: Long): WeightLogEntity?

    @Query("SELECT * FROM weight_logs ORDER BY dateEpochDay DESC LIMIT 1")
    suspend fun getLatestWeightLog(): WeightLogEntity?

    @Delete
    suspend fun deleteWeightLog(log: WeightLogEntity)

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteWeightLogById(id: Long)
}
