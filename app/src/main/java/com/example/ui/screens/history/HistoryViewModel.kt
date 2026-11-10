package com.example.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppSettingsEntity
import com.example.data.database.FoodEntity
import com.example.data.database.FoodLogEntity
import com.example.data.repository.DailyNutritionSummary
import com.example.data.repository.NutritionRepository
import com.example.util.DateBounds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    val appSettings: StateFlow<AppSettingsEntity?> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val canGoPreviousDay: StateFlow<Boolean> = combine(_selectedDate, appSettings) { date, settings ->
        DateBounds.canGoEarlier(date, settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val canGoNextDay: StateFlow<Boolean> = combine(_selectedDate, appSettings) { date, settings ->
        DateBounds.canGoLater(date, settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

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
        _selectedDate.value = DateBounds.clamp(date, appSettings.value)
    }

    fun previousDay() {
        _selectedDate.value = DateBounds.clamp(_selectedDate.value.minusDays(1), appSettings.value)
    }

    fun nextDay() {
        _selectedDate.value = DateBounds.clamp(_selectedDate.value.plusDays(1), appSettings.value)
    }

    fun deleteLog(log: FoodLogEntity) {
        viewModelScope.launch {
            repository.deleteFoodLog(log)
        }
    }

    /** Saves an edited entry back to the day log. */
    fun updateLog(log: FoodLogEntity) {
        viewModelScope.launch {
            repository.updateFoodLog(log)
        }
    }

    /** The inventory, so an edited entry can recalculate its macros. */
    val inventory: StateFlow<List<FoodEntity>> = repository.allFoodsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
