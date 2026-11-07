package com.example.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.FoodLogEntity
import com.example.data.repository.DailyNutritionSummary
import com.example.data.repository.NutritionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val repository: NutritionRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val dailySummary: StateFlow<DailyNutritionSummary> = _selectedDate
        .flatMapLatest { date ->
            repository.getDailySummary(date.toString())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DailyNutritionSummary(dateString = LocalDate.now().toString())
        )

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun deleteLog(log: FoodLogEntity) {
        viewModelScope.launch {
            repository.deleteFoodLog(log)
        }
    }
}
