package com.example.ui.screens.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.NutritionGoalEntity
import com.example.data.database.WeightLogEntity
import com.example.data.repository.NutritionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class WeightTrackerViewModel(
    private val repository: NutritionRepository
) : ViewModel() {

    val weightLogsDesc: StateFlow<List<WeightLogEntity>> = repository.weightLogsDescFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val goals: StateFlow<NutritionGoalEntity?> = repository.goalsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun logWeight(date: LocalDate, weightKg: Double, note: String = "") {
        viewModelScope.launch {
            repository.logWeight(date, weightKg, note)
            _feedbackMessage.value = "Weight logged: ${String.format("%.1f", weightKg)} kg for ${date}"
        }
    }

    fun deleteWeightLog(log: WeightLogEntity) {
        viewModelScope.launch {
            repository.deleteWeightLog(log)
            _feedbackMessage.value = "Weight entry removed"
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }
}
