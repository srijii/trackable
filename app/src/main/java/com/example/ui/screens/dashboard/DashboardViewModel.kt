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
import com.example.util.ApiKeyUtils
import com.example.util.DateBounds
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class AiProviderOption(
    val id: String,
    val label: String
)

data class AiFailureState(
    val message: String,
    val prompt: String,
    val providerUsed: String,
    val providerOptions: List<AiProviderOption>,
    val externalProviderCount: Int
) {
    /** True when there is more than one usable API key to choose between. */
    val canChooseProvider: Boolean get() = externalProviderCount > 1
}

data class AiLoggedFoodItem(
    val key: Long,
    val name: String,
    val brand: String,
    val quantity: String,
    val servingUnit: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val mealType: String
)

data class AiLoggedSummary(
    val items: List<AiLoggedFoodItem>,
    val skippedNames: List<String>,
    val waterMl: Int,
    val logIds: Set<Long>
)

enum class AiProcessingStage {
    ANALYSING,
    SAVING
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: NutritionRepository,
    private val aiService: NutritionAIService
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Start of the week (Sunday) containing the selected date
    val weekStart: StateFlow<LocalDate> = selectedDate
        .map { date -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
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
            initialValue = null
        )

    val goals: StateFlow<NutritionGoalEntity> = repository.goalsFlow
        .map { it ?: NutritionGoalEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NutritionGoalEntity()
        )

    val canGoPreviousWeek: StateFlow<Boolean> = combine(_selectedDate, appSettings) { date, settings ->
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        DateBounds.canGoEarlier(weekStart, settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val canGoNextWeek: StateFlow<Boolean> = combine(_selectedDate, appSettings) { date, settings ->
        val weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        DateBounds.canGoLater(weekEnd, settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _aiIsAnalysing = MutableStateFlow(false)
    val aiIsLoading: StateFlow<Boolean> = _aiIsAnalysing.asStateFlow()

    private val _aiParseResult = MutableStateFlow<NutritionAiParseResult?>(null)
    val aiParseResult: StateFlow<NutritionAiParseResult?> = _aiParseResult.asStateFlow()

    private val _aiFailure = MutableStateFlow<AiFailureState?>(null)
    val aiFailure: StateFlow<AiFailureState?> = _aiFailure.asStateFlow()

    private val _aiLoggedSummary = MutableStateFlow<AiLoggedSummary?>(null)
    val aiLoggedSummary: StateFlow<AiLoggedSummary?> = _aiLoggedSummary.asStateFlow()

    private val _aiStage = MutableStateFlow<AiProcessingStage?>(null)
    val aiStage: StateFlow<AiProcessingStage?> = _aiStage.asStateFlow()

    private var aiJob: Job? = null

    private var lastSentPrompt: String = ""

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initialize()
        }
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = DateBounds.clamp(date, appSettings.value)
    }

    fun previousWeek() {
        val date = _selectedDate.value.minusWeeks(1)
        _selectedDate.value = DateBounds.clamp(date, appSettings.value)
    }

    fun nextWeek() {
        val date = _selectedDate.value.plusWeeks(1)
        _selectedDate.value = DateBounds.clamp(date, appSettings.value)
    }

    fun previousDay() {
        val date = _selectedDate.value.minusDays(1)
        _selectedDate.value = DateBounds.clamp(date, appSettings.value)
    }

    fun nextDay() {
        val date = _selectedDate.value.plusDays(1)
        _selectedDate.value = DateBounds.clamp(date, appSettings.value)
    }

    fun goToToday() {
        _selectedDate.value = DateBounds.clamp(LocalDate.now(), appSettings.value)
    }

    fun dismissGoalWarning() {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(dismissedGoalWarning = true))
        }
    }

    // ---------------------------------------------------------------------
    // AI text logging
    // ---------------------------------------------------------------------

    fun onSendAiPrompt(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty() || _aiIsAnalysing.value) return

        lastSentPrompt = prompt
        _aiParseResult.value = null
        _aiFailure.value = null
        _aiLoggedSummary.value = null
        // The skeleton is shown immediately, before any network call starts.
        _aiStage.value = AiProcessingStage.ANALYSING
        _aiIsAnalysing.value = true

        aiJob = viewModelScope.launch {
            executePrompt(prompt)
        }
    }

    fun cancelAiRequest() {
        if (!_aiIsAnalysing.value) return
        aiJob?.cancel()
        aiJob = null
        _aiIsAnalysing.value = false
        _aiStage.value = null
        _aiParseResult.value = null
        _aiFailure.value = null
        _userFeedbackMessage.value = "AI request cancelled"
    }

    fun dismissAiDialog() {
        _aiParseResult.value = null
    }

    fun retryFailedPrompt() {
        val failure = _aiFailure.value ?: return
        if (_aiIsAnalysing.value) return

        _aiFailure.value = null
        lastSentPrompt = failure.prompt
        _aiStage.value = AiProcessingStage.ANALYSING
        _aiIsAnalysing.value = true
        aiJob = viewModelScope.launch {
            executePrompt(failure.prompt)
        }
    }

    fun switchProviderAndRetry(providerId: String) {
        val failure = _aiFailure.value ?: return
        if (_aiIsAnalysing.value) return

        _aiFailure.value = null
        lastSentPrompt = failure.prompt
        _aiStage.value = AiProcessingStage.ANALYSING
        _aiIsAnalysing.value = true
        aiJob = viewModelScope.launch {
            val settings = repository.getSettings()
            repository.saveSettings(settings.copy(aiProvider = providerId))
            executePrompt(failure.prompt)
        }
    }

    fun dismissAiFailure() {
        _aiFailure.value = null
    }

    fun dismissAiLoggedSummary() {
        _aiLoggedSummary.value = null
    }

    private suspend fun executePrompt(prompt: String) {
        try {
            _aiStage.value = AiProcessingStage.ANALYSING
            val result = aiService.parseFoodInput(prompt, repository.getAllFoods())
            if (!result.isSuccess) {
                _aiFailure.value = buildFailureState(
                    message = result.errorMessage ?: "The AI request failed.",
                    providerUsed = result.providerUsed,
                    prompt = prompt
                )
                return
            }

            val settings = repository.getSettings()
            if (settings.aiProvider == "LOCAL") {
                // Local Smart Engine is the only provider that asks for confirmation.
                _aiParseResult.value = result
            } else {
                // Real AI providers log straight away. The dashboard then writes
                // out the individual food rows instead of showing a popup.
                _aiStage.value = AiProcessingStage.SAVING
                val insertedIds = insertParsedItems(result.items.filter { it.matchedFood != null })
                if (result.waterMl > 0) {
                    repository.addWater(result.waterMl, _selectedDate.value.toString())
                }
                _aiLoggedSummary.value = buildLoggedSummary(result, insertedIds)
            }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            _aiFailure.value = buildFailureState(
                message = t.message ?: "The AI request failed unexpectedly.",
                providerUsed = "AI",
                prompt = prompt
            )
        } finally {
            _aiIsAnalysing.value = false
            _aiStage.value = null
            aiJob = null
        }
    }

    private suspend fun buildFailureState(
        message: String,
        providerUsed: String,
        prompt: String
    ): AiFailureState {
        val settings = repository.getSettings()
        val options = providerOptions(settings)
        return AiFailureState(
            message = message,
            prompt = prompt,
            providerUsed = providerUsed,
            providerOptions = options,
            externalProviderCount = options.count { it.id != "LOCAL" }
        )
    }

    private fun providerOptions(settings: AppSettingsEntity): List<AiProviderOption> {
        val options = mutableListOf<AiProviderOption>()
        if (hasUsableKey(settings.geminiApiKey)) {
            options += AiProviderOption("GEMINI", "Gemini (${settings.geminiModel})")
        }
        if (hasUsableKey(settings.nvidiaApiKey)) {
            options += AiProviderOption("NVIDIA_NIM", "NVIDIA NIM (${settings.nvidiaModel})")
        }
        if (hasUsableKey(settings.openaiApiKey)) {
            options += AiProviderOption("OPENAI", "OpenAI (${settings.openaiModel})")
        }
        if (hasUsableKey(settings.groqApiKey)) {
            options += AiProviderOption("GROQ", "Groq (${settings.groqModel})")
        }
        options += AiProviderOption("LOCAL", "Local Smart Engine")
        return options
    }

    private fun hasUsableKey(rawKey: String): Boolean {
        val key = ApiKeyUtils.normalize(rawKey)
        return key.isNotBlank() && !key.startsWith("MY_")
    }

    private fun buildLoggedSummary(
        result: NutritionAiParseResult,
        insertedIds: List<Long>
    ): AiLoggedSummary {
        val matched = result.items.filter { it.matchedFood != null }
        val skipped = result.items.filter { it.matchedFood == null }.map { it.rawName }
        val items = matched.mapIndexed { index, item ->
            val food = item.matchedFood ?: return@mapIndexed null
            AiLoggedFoodItem(
                key = insertedIds.getOrNull(index) ?: (index.toLong() + 1L),
                name = food.name,
                brand = food.brand,
                quantity = formatQuantity(item.quantity),
                servingUnit = item.servingUnit,
                calories = item.calculatedCalories,
                protein = item.calculatedProtein,
                carbs = item.calculatedCarbs,
                fat = item.calculatedFat,
                mealType = item.mealType
            )
        }.filterNotNull()
        return AiLoggedSummary(
            items = items,
            skippedNames = skipped,
            waterMl = result.waterMl,
            logIds = insertedIds.toSet()
        )
    }

    fun confirmAiLog(confirmedItems: List<ParsedItemCandidate>, waterMl: Int) {
        viewModelScope.launch {
            insertParsedItems(confirmedItems.filter { it.matchedFood != null })

            if (waterMl > 0) {
                repository.addWater(waterMl, _selectedDate.value.toString())
            }

            _aiParseResult.value = null
            _userFeedbackMessage.value = "Logged ${confirmedItems.count { it.matchedFood != null }} food item(s)" +
                if (waterMl > 0) " & ${waterMl}ml water" else ""
        }
    }

    private suspend fun insertParsedItems(items: List<ParsedItemCandidate>): List<Long> {
        val dateStr = _selectedDate.value.toString()
        val logsToInsert = items.mapNotNull { item ->
            val food = item.matchedFood ?: return@mapNotNull null
            val multiplier = if (food.calories > 0.0) {
                item.calculatedCalories / food.calories
            } else {
                1.0
            }
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
                fiber = food.fiber?.let { NutritionCalculationEngine.roundToOneDecimal(it * multiplier) },
                sugar = food.sugar?.let { NutritionCalculationEngine.roundToOneDecimal(it * multiplier) },
                dateString = dateStr,
                mealType = item.mealType,
                originalPrompt = lastSentPrompt.ifBlank {
                    "${food.name} (${formatQuantity(item.quantity)}${item.servingUnit})"
                }
            )
        }

        return if (logsToInsert.isNotEmpty()) {
            repository.insertFoodLogs(logsToInsert)
        } else {
            emptyList()
        }
    }

    /**
     * Saves a label-scanned food (after the user reviewed name/brand) and logs
     * one serving to the currently selected day.
     */
    fun saveScannedFoodAndLog(food: FoodEntity) {
        viewModelScope.launch {
            val foodId = repository.insertFood(food)
            val calc = NutritionCalculationEngine.calculate(food, food.servingSize, food.servingUnit)
            repository.insertFoodLog(
                FoodLogEntity(
                    foodId = foodId,
                    foodNameSnapshot = food.name,
                    brandSnapshot = food.brand,
                    quantity = food.servingSize,
                    servingUnit = food.servingUnit,
                    calories = calc.calories,
                    protein = calc.protein,
                    carbohydrates = calc.carbohydrates,
                    fat = calc.fat,
                    fiber = calc.fiber,
                    sugar = calc.sugar,
                    dateString = _selectedDate.value.toString(),
                    mealType = mealTypeForNow(),
                    originalPrompt = "Scanned label — ${food.name}"
                )
            )
            _userFeedbackMessage.value = "Added ${food.name} to inventory and logged it"
        }
    }

    suspend fun scanNutritionLabel(imageBase64: String): NutritionAiParseResult {
        return aiService.extractNutritionLabel(imageBase64)
    }

    // ---------------------------------------------------------------------
    // Water & log editing
    // ---------------------------------------------------------------------

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

    fun updateFoodLogEntry(updatedLog: FoodLogEntity) {
        viewModelScope.launch {
            repository.updateFoodLog(updatedLog)
            _userFeedbackMessage.value = "Updated ${updatedLog.foodNameSnapshot}"
        }
    }

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }

    private fun formatQuantity(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString()
        else String.format(Locale.US, "%.1f", value)
    }

    private fun formatMacro(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString()
        else String.format(Locale.US, "%.1f", value)
    }

    private fun mealTypeForNow(): String {
        return when (LocalTime.now().hour) {
            in 5..10 -> "Breakfast"
            in 11..14 -> "Lunch"
            in 15..17 -> "Snack"
            in 18..22 -> "Dinner"
            else -> "Snack"
        }
    }
}
