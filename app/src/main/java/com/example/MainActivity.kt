package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ai.GeminiNutritionAIService
import com.example.ai.NutritionAIServiceCoordinator
import com.example.data.database.AppDatabase
import com.example.data.repository.NutritionRepository
import com.example.ui.navigation.NutriTrackApp
import com.example.ui.theme.NutriTrackTheme

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: NutritionRepository
    private lateinit var aiCoordinator: NutritionAIServiceCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getDatabase(applicationContext)
        repository = NutritionRepository(database)
        aiCoordinator = NutritionAIServiceCoordinator(repository)

        setContent {
            NutriTrackTheme {
                NutriTrackApp(
                    repository = repository,
                    aiCoordinator = aiCoordinator
                )
            }
        }
    }
}
