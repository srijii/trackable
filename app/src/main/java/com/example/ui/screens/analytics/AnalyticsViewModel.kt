package com.example.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppSettingsEntity
import com.example.data.database.NutritionGoalEntity
import com.example.data.repository.DayTrendPoint
import com.example.data.repository.NutritionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class AnalyticsState(
    val daysCount: Int = 7,
    val trendPoints: List<DayTrendPoint> = emptyList(),
    val averageCalories: Double = 0.0,
    val averageProtein: Double = 0.0,
    val averageCarbs: Double = 0.0,
    val averageFat: Double = 0.0,
    val averageWaterMl: Int = 0,
    val calorieGoal: Double = 2200.0,
    val proteinGoal: Double = 140.0,
    val goalCompletionPercentage: Int = 0,
    val totalLoggedDays: Int = 0
)

class AnalyticsViewModel(
    private val repository: NutritionRepository
) : ViewModel() {

    private val _selectedDays = MutableStateFlow(7)
    val selectedDays: StateFlow<Int> = _selectedDays.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)
    val customDateRange: StateFlow<Pair<LocalDate, LocalDate>?> = _customDateRange.asStateFlow()

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

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

    val weightLogsAsc: StateFlow<List<com.example.data.database.WeightLogEntity>> = repository.weightLogsAscFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            _selectedDays.collect { days ->
                if (days != -1) {
                    loadAnalytics(days)
                }
            }
        }
    }

    fun selectDaysRange(days: Int) {
        _customDateRange.value = null
        _selectedDays.value = days
    }

    fun selectCustomRange(startDate: LocalDate, endDate: LocalDate) {
        _customDateRange.value = Pair(startDate, endDate)
        _selectedDays.value = -1
        loadAnalyticsCustom(startDate, endDate)
    }

    fun refresh() {
        val customRange = _customDateRange.value
        if (_selectedDays.value == -1 && customRange != null) {
            loadAnalyticsCustom(customRange.first, customRange.second)
        } else {
            loadAnalytics(_selectedDays.value)
        }
    }

    private fun loadAnalyticsCustom(startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            val daysCount = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
            val points = repository.getTrendData(startDate.toString(), endDate.toString(), daysCount)
            val currentGoals = repository.getGoals()

            val loggedDays = points.filter { it.calories > 0 }
            val avgCal = if (loggedDays.isNotEmpty()) loggedDays.sumOf { it.calories } / loggedDays.size else 0.0
            val avgProt = if (loggedDays.isNotEmpty()) loggedDays.sumOf { it.protein } / loggedDays.size else 0.0
            val avgCarbs = if (loggedDays.isNotEmpty()) loggedDays.sumOf { it.carbs } / loggedDays.size else 0.0
            val avgFat = if (loggedDays.isNotEmpty()) loggedDays.sumOf { it.fat } / loggedDays.size else 0.0
            val avgWater = if (loggedDays.isNotEmpty()) (loggedDays.sumOf { it.waterMl } / loggedDays.size) else 0

            val goalHits = loggedDays.count { it.calories in (currentGoals.caloriesGoal * 0.85)..(currentGoals.caloriesGoal * 1.1) }
            val completionPct = if (loggedDays.isNotEmpty()) (goalHits * 100) / loggedDays.size else 0

            _state.value = AnalyticsState(
                daysCount = daysCount,
                trendPoints = points,
                averageCalories = Math.round(avgCal * 10.0) / 10.0,
                averageProtein = Math.round(avgProt * 10.0) / 10.0,
                averageCarbs = Math.round(avgCarbs * 10.0) / 10.0,
                averageFat = Math.round(avgFat * 10.0) / 10.0,
                averageWaterMl = avgWater,
                calorieGoal = currentGoals.caloriesGoal,
                proteinGoal = currentGoals.proteinGoal,
                goalCompletionPercentage = completionPct,
                totalLoggedDays = loggedDays.size
            )
        }
    }

    private fun loadAnalytics(days: Int) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val settings = repository.getSettings()
            val startDateStr = settings?.startDate
            val useStartDate = settings?.useStartDate == true

            val start = if (useStartDate && startDateStr != null) {
                LocalDate.parse(startDateStr)
            } else {
                today.minusDays((days - 1).toLong())
            }
            val points = repository.getTrendData(start.toString(), today.toString(), days)
            val currentGoals = repository.getGoals()

            val loggedDays = points.filter { it.calories > 0 }
            val avgCal = if (loggedDays.isNotEmpty()) loggedDays.sumOf { it.calories } / loggedDays.size else 0.0
            val avgProt = if (loggedDays.isNotEmpty()) loggedDays.sumOf { it.protein } / loggedDays.size else 0.0
            val avgCarbs = if (loggedDays.isNotEmpty()) loggedDays.sumOf { it.carbs } / loggedDays.size else 0.0
            val avgFat = if (loggedDays.isNotEmpty()) loggedDays.sumOf { it.fat } / loggedDays.size else 0.0
            val avgWater = if (loggedDays.isNotEmpty()) (loggedDays.sumOf { it.waterMl } / loggedDays.size) else 0

            val goalHits = loggedDays.count { it.calories in (currentGoals.caloriesGoal * 0.85)..(currentGoals.caloriesGoal * 1.1) }
            val completionPct = if (loggedDays.isNotEmpty()) (goalHits * 100) / loggedDays.size else 0

            _state.value = AnalyticsState(
                daysCount = days,
                trendPoints = points,
                averageCalories = Math.round(avgCal * 10.0) / 10.0,
                averageProtein = Math.round(avgProt * 10.0) / 10.0,
                averageCarbs = Math.round(avgCarbs * 10.0) / 10.0,
                averageFat = Math.round(avgFat * 10.0) / 10.0,
                averageWaterMl = avgWater,
                calorieGoal = currentGoals.caloriesGoal,
                proteinGoal = currentGoals.proteinGoal,
                goalCompletionPercentage = completionPct,
                totalLoggedDays = loggedDays.size
            )
        }
    }
}
