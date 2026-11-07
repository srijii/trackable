package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val aiProvider: String = "GEMINI", // "GEMINI", "NVIDIA_NIM", "OPENAI", "LOCAL"
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-2.5-flash",
    val nvidiaApiKey: String = "",
    val nvidiaModel: String = "meta/llama-3.3-70b-instruct",
    val nvidiaBaseUrl: String = "https://integrate.api.nvidia.com/v1",
    val openaiApiKey: String = "",
    val openaiModel: String = "gpt-4o-mini",
    val dismissedGoalWarning: Boolean = false,
    // Tracking starts from this day (inclusive). Days before this date are untrackable.
    val startDate: String? = null, // "YYYY-MM-DD"
    // Whether start date tracking is enabled at all
    val useStartDate: Boolean = false,
    // Block adding data before the start date
    val blockBeforeStartDate: Boolean = true,
    // Block adding data after today
    val blockAfterToday: Boolean = true
)
