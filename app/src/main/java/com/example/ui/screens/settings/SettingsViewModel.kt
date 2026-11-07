package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppSettingsEntity
import com.example.data.database.NutritionGoalEntity
import com.example.data.repository.NutritionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: NutritionRepository
) : ViewModel() {

    val goals: StateFlow<NutritionGoalEntity?> = repository.goalsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NutritionGoalEntity()
        )

    val appSettings: StateFlow<AppSettingsEntity?> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettingsEntity()
        )

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    fun updateGoalsAndSettings(goals: NutritionGoalEntity, settings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.updateGoals(goals)
            repository.saveSettings(settings)
            _saveMessage.value = "Settings and API keys saved successfully!"
        }
    }

    fun updateAiSettings(settings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.saveSettings(settings)
            val modelName = when (settings.aiProvider) {
                "GEMINI" -> "Gemini (${settings.geminiModel})"
                "NVIDIA_NIM" -> "NVIDIA NIM (${settings.nvidiaModel})"
                "OPENAI" -> "OpenAI (${settings.openaiModel})"
                else -> "Local Smart Engine"
            }
            _saveMessage.value = "AI Model ($modelName) and API key saved!"
        }
    }

    fun saveStartDate(startDate: String?) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(startDate = startDate))
            _saveMessage.value = if (startDate == null) "Tracking start date cleared" else "Tracking starts from $startDate"
        }
    }

    fun saveStartDateSettings(
        useStartDate: Boolean,
        startDate: String?,
        blockBeforeStartDate: Boolean,
        blockAfterToday: Boolean
    ) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(
                useStartDate = useStartDate,
                startDate = startDate,
                blockBeforeStartDate = blockBeforeStartDate,
                blockAfterToday = blockAfterToday
            ))
            _saveMessage.value = "Start date settings saved"
        }
    }

    fun clearMessage() {
        _saveMessage.value = null
    }
}
