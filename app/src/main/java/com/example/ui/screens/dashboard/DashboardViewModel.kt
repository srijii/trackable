package com.example.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.NutritionAIService
import com.example.ai.NutritionAiParseResult
import com.example.ai.ParsedItemCandidate
import com.example.data.database.AppSettingsEntity
import com.example.data.database.FoodEntity
import com.example.data.database.FoodLogEntity
import com.example.data.database.NutritionGoalEntity
import com.example.data.repository.DailyNutritionSummary
import com.example.data.repository.NutritionRepository
import com.example.domain.NutritionCalculationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: NutritionRepository,
    private val aiService: NutritionAIService
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Start of the week (Sunday) containing the selected date
    val weekStart: StateFlow<LocalDate> = selectedDate
        .map { date -> date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY))
        )

    val weekSummaries: StateFlow<List<DailyNutritionSummary>> = weekStart
        .flatMapLatest { repository.getWeekSummaries(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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

    val inventory: StateFlow<List<FoodEntity>> = repository.allFoodsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val appSettings: StateFlow<AppSettingsEntity?> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettingsEntity()
        )

    val goals: StateFlow<NutritionGoalEntity> = repository.goalsFlow
        .map { it ?: NutritionGoalEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NutritionGoalEntity()
        )

    private val _aiIsLoading = MutableStateFlow(false)
    val aiIsLoading: StateFlow<Boolean> = _aiIsLoading.asStateFlow()

    private val _aiParseResult = MutableStateFlow<NutritionAiParseResult?>(null)
    val aiParseResult: StateFlow<NutritionAiParseResult?> = _aiParseResult.asStateFlow()

    private var lastSentPrompt: String = ""

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initialize()
        }
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun previousWeek() {
        _selectedDate.value = _selectedDate.value.minusDays(7)
    }

    fun nextWeek() {
        _selectedDate.value = _selectedDate.value.plusDays(7)
    }

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun goToToday() {
        _selectedDate.value = LocalDate.now()
    }

    fun dismissGoalWarning() {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(dismissedGoalWarning = true))
        }
    }

    fun onSendAiPrompt(text: String) {
        lastSentPrompt = text.trim()
        viewModelScope.launch {
            _aiIsLoading.value = true
            try {
                val currentInventory = repository.getAllFoods()
                val result = aiService.parseFoodInput(text, currentInventory)
                _aiParseResult.value = result
            } catch (e: Exception) {
                _userFeedbackMessage.value = "Failed to parse: ${e.localizedMessage}"
            } finally {
                _aiIsLoading.value = false
            }
        }
    }

    fun dismissAiDialog() {
        _aiParseResult.value = null
    }

    fun confirmAiLog(confirmedItems: List<ParsedItemCandidate>, waterMl: Int) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.toString()
            val logsToInsert = mutableListOf<FoodLogEntity>()

            for (item in confirmedItems) {
                val food = item.matchedFood
                if (food != null) {
                    logsToInsert.add(
                        FoodLogEntity(
                            foodId = food.id,
                            foodNameSnapshot = food.name,
                            brandSnapshot = food.brand,
                            quantity = item.quantity,
                            servingUnit = item.servingUnit,
                            calories = item.calculatedCalories,
                            protein = item.calculatedProtein,
                            carbohydrates = item.calculatedCarbs,
                            fat = item.calculatedFat,
                            fiber = food.fiber?.let { it * (item.calculatedCalories / (if (food.calories > 0) food.calories else 1.0)) },
                            sugar = food.sugar?.let { it * (item.calculatedCalories / (if (food.calories > 0) food.calories else 1.0)) },
                            dateString = dateStr,
                            mealType = item.mealType,
                            originalPrompt = lastSentPrompt.ifBlank { "${food.name} (${item.quantity}${item.servingUnit})" }
                        )
                    )
                }
            }

            if (logsToInsert.isNotEmpty()) {
                repository.insertFoodLogs(logsToInsert)
            }

            if (waterMl > 0) {
                repository.addWater(waterMl, dateStr)
            }

            _aiParseResult.value = null
            _userFeedbackMessage.value = "Logged ${logsToInsert.size} food item(s)${if (waterMl > 0) " & ${waterMl}ml water" else ""}!"
        }
    }

    fun addWaterCupSize1() {
        viewModelScope.launch {
            val size = appSettings.value?.cupSize1Ml ?: 250
            repository.addWater(size, _selectedDate.value.toString())
            _userFeedbackMessage.value = "Added $size ml water"
        }
    }

    fun addWaterCupSize2() {
        viewModelScope.launch {
            val size = appSettings.value?.cupSize2Ml ?: 500
            repository.addWater(size, _selectedDate.value.toString())
            _userFeedbackMessage.value = "Added $size ml water"
        }
    }

    fun removeWaterCup() {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.toString()
            // Look up water logs for today and delete the most recent or add -250
            val currentSummary = dailySummary.value
            val lastWaterLog = currentSummary.waterLogs.firstOrNull()
            if (lastWaterLog != null) {
                repository.deleteWaterLog(lastWaterLog)
            }
        }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            repository.addWater(amountMl, _selectedDate.value.toString())
            _userFeedbackMessage.value = "Added $amountMl ml water"
        }
    }

    fun deleteFoodLog(log: FoodLogEntity) {
        viewModelScope.launch {
            repository.deleteFoodLog(log)
            _userFeedbackMessage.value = "Removed ${log.foodNameSnapshot}"
        }
    }

    fun updateFoodLogQuantity(log: FoodLogEntity, newQuantity: Double) {
        viewModelScope.launch {
            val food = log.foodId?.let { repository.getFoodById(it) }
            val updatedLog = if (food != null) {
                val calc = NutritionCalculationEngine.calculate(food, newQuantity, log.servingUnit)
                log.copy(
                    quantity = newQuantity,
                    calories = calc.calories,
                    protein = calc.protein,
                    carbohydrates = calc.carbohydrates,
                    fat = calc.fat
                )
            } else {
                val ratio = if (log.quantity > 0) newQuantity / log.quantity else 1.0
                log.copy(
                    quantity = newQuantity,
                    calories = NutritionCalculationEngine.roundToOneDecimal(log.calories * ratio),
                    protein = NutritionCalculationEngine.roundToOneDecimal(log.protein * ratio),
                    carbohydrates = NutritionCalculationEngine.roundToOneDecimal(log.carbohydrates * ratio),
                    fat = NutritionCalculationEngine.roundToOneDecimal(log.fat * ratio)
                )
            }
            repository.updateFoodLog(updatedLog)
            _userFeedbackMessage.value = "Updated ${log.foodNameSnapshot}"
        }
    }

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }
}
