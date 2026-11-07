package com.example.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "weight_logs",
    indices = [Index(value = ["dateEpochDay"], unique = true)]
)
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateEpochDay: Long = LocalDate.now().toEpochDay(),
    val weightKg: Double,
    val note: String = "",
    val loggedAtTimestamp: Long = System.currentTimeMillis()
)
