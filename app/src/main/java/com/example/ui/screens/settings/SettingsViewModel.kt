package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppSettingsEntity
import com.example.data.database.NutritionGoalEntity
import com.example.data.repository.ApiKeyTestResult
import com.example.data.repository.NutritionRepository
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class SaveResult(
    val success: Boolean,
    val message: String,
    val navigateToDashboard: Boolean = false
)

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

    private val _testError = MutableStateFlow<String?>(null)
    val testError: StateFlow<String?> = _testError.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult.asStateFlow()

    // Track original values for unsaved changes detection
    private var originalGoals: NutritionGoalEntity = NutritionGoalEntity()
    private var originalSettings: AppSettingsEntity = AppSettingsEntity()

    fun initializeOriginals(currentGoals: NutritionGoalEntity?, currentSettings: AppSettingsEntity?) {
        originalGoals = currentGoals ?: NutritionGoalEntity()
        originalSettings = currentSettings ?: AppSettingsEntity()
    }

    fun hasUnsavedChanges(
        currentGoals: NutritionGoalEntity?,
        currentSettings: AppSettingsEntity?,
        geminiApiKey: String,
        nvidiaApiKey: String,
        openaiApiKey: String,
        nvidiaBaseUrl: String,
        calTarget: String,
        proteinTarget: String,
        carbsTarget: String,
        fatTarget: String,
        waterTarget: String,
        cupSize1Target: String,
        cupSize2Target: String,
        nameText: String,
        sexValue: String,
        weightText: String,
        targetWeightText: String,
        heightText: String,
        ageText: String,
        activityLevel: String,
        useStartDateLocal: Boolean,
        startDateLocal: String?,
        blockBeforeStartLocal: Boolean,
        blockAfterTodayLocal: Boolean
    ): Boolean {
        val goals = currentGoals ?: NutritionGoalEntity()
        val settings = currentSettings ?: AppSettingsEntity()

        // Check API key changes (provider-aware)
        when (selectedAiProvider) {
            "GEMINI" -> if (geminiApiKey.trim() != settings.geminiApiKey.trim()) return true
            "NVIDIA_NIM" -> if (nvidiaApiKey.trim() != settings.nvidiaApiKey.trim()) return true
            "OPENAI" -> if (openaiApiKey.trim() != settings.openaiApiKey.trim()) return true
        }
        if (selectedAiProvider != settings.aiProvider) return true
        if (selectedModelName != when {
            selectedAiProvider == "GEMINI" -> settings.geminiModel
            selectedAiProvider == "NVIDIA_NIM" -> settings.nvidiaModel
            selectedAiProvider == "OPENAI" -> settings.openaiModel
            else -> settings.geminiModel
        }) return true

        // Check goals changes
        if (calTarget.toDoubleOrNull() ?: -1 != goals.caloriesGoal) return true
        if (proteinTarget.toDoubleOrNull() ?: -1 != goals.proteinGoal) return true
        if (carbsTarget.toDoubleOrNull() ?: -1 != goals.carbsGoal) return true
        if (fatTarget.toDoubleOrNull() ?: -1 != goals.fatGoal) return true
        if (waterTarget.toIntOrNull() ?: -1 != goals.waterGoalMl) return true

        // Check profile changes
        if (nameText.trim() != goals.name) return true
        if (sexValue != goals.sex) return true
        if (weightText.toDoubleOrNull() != goals.currentWeightKg) return true
        if (targetWeightText.toDoubleOrNull() != goals.targetWeightKg) return true
        if (heightText.toDoubleOrNull() != goals.heightCm) return true
        if (ageText.toIntOrNull() != goals.age) return true
        if (activityLevel != goals.activityLevel) return true

        // Check cup sizes
        if (cupSize1Target.toIntOrNull() != settings.cupSize1Ml) return true
        if (cupSize2Target.toIntOrNull() != settings.cupSize2Ml) return true

        // Check start date settings
        if (useStartDateLocal != settings.useStartDate) return true
        if (startDateLocal != settings.startDate) return true
        if (blockBeforeStartLocal != settings.blockBeforeStartDate) return true
        if (blockAfterTodayLocal != settings.blockAfterToday) return true

        return false
    }

    // Track selected AI provider and model for change detection
    private var selectedAiProvider: String = "LOCAL"
    private var selectedModelName: String = "local-smart"

    fun setSelectedAiModel(provider: String, modelName: String) {
        selectedAiProvider = provider
        selectedModelName = modelName
    }

    // --- Nutrition Calculation (Mifflin-St Jeor) ---
    private val activityMultipliers = mapOf(
        "Sedentary" to 1.2,
        "Lightly Active" to 1.375,
        "Moderately Active" to 1.55,
        "Very Active" to 1.725,
        "Extra Active" to 1.9
    )

    fun calculateNutrition(
        age: Int?,
        sex: String?,
        currentWeightKg: Double?,
        targetWeightKg: Double?,
        heightCm: Double?,
        activityLevel: String?
    ): NutritionCalculationResult? {
        if (age == null || sex == null || currentWeightKg == null || targetWeightKg == null ||
            heightCm == null || activityLevel == null || currentWeightKg <= 0 || heightCm <= 0
        ) {
            return null
        }

        val ageA = age.toDouble()
        val weightW = currentWeightKg
        val heightH = heightCm
        val goalWeight = targetWeightKg

        val difference = goalWeight - weightW
        val threshold = weightW * 0.01
        val goal = when {
            difference > threshold -> "gain"
            difference < -threshold -> "loss"
            else -> "maintenance"
        }

        val bmr = when (sex.lowercase(Locale.ROOT)) {
            "male" -> 10.0 * weightW + 6.25 * heightH - 5.0 * ageA + 5.0
            "female" -> 10.0 * weightW + 6.25 * heightH - 5.0 * ageA - 161.0
            else -> 10.0 * weightW + 6.25 * heightH - 5.0 * ageA + 5.0
        }

        val multiplier = activityMultipliers[activityLevel] ?: 1.55
        val tdee = bmr * multiplier

        var adjustment = 0.0
        var isUnsafeRate = false

        if (goal != "maintenance") {
            adjustment = difference * 7700.0 / 182.0
            val maxWeeklyRate = if (goal == "gain") 0.005 else 0.01
            val maxDailyAdjustment = weightW * maxWeeklyRate * 7700.0 / 7.0
            val clampedAdjustment = adjustment.coerceIn(-maxDailyAdjustment, maxDailyAdjustment)
            if (clampedAdjustment != adjustment) isUnsafeRate = true
            adjustment = clampedAdjustment
        }

        val calories = (tdee + adjustment).coerceAtLeast(1200.0)

        val proteinMultiplier = when (goal) {
            "gain" -> 1.8
            "loss" -> 2.0
            else -> 1.6
        }
        val protein = weightW * proteinMultiplier

        val fatFromCalories = calories * 0.25 / 9.0
        val fatMin = weightW * 0.6
        val fat = maxOf(fatFromCalories, fatMin)

        val carbs = (calories - protein * 4.0 - fat * 9.0) / 4.0
        val finalCarbs = if (carbs < 0) 0.0 else carbs

        return NutritionCalculationResult(
            goal = goal,
            bmr = bmr.roundToInt(),
            tdee = tdee.roundToInt(),
            calories = calories.roundToInt(),
            protein = protein.roundToInt(),
            fat = fat.roundToInt(),
            carbs = finalCarbs.roundToInt(),
            adjustment = adjustment.roundToInt(),
            isUnsafeRate = isUnsafeRate
        )
    }

    // --- Save API Settings (with key test) ---
    fun saveApiSettings(settings: AppSettingsEntity) {
        viewModelScope.launch {
            _isTesting.value = true
            _testError.value = null

            val provider = settings.aiProvider
            val modelName = when (provider) {
                "GEMINI" -> "Gemini (${settings.geminiModel})"
                "NVIDIA_NIM" -> "NVIDIA NIM (${settings.nvidiaModel})"
                "OPENAI" -> "OpenAI (${settings.openaiModel})"
                else -> "Local Smart Engine"
            }

            if (provider == "LOCAL") {
                repository.saveSettings(settings)
                _isTesting.value = false
                _saveResult.value = SaveResult(true, "AI Model ($modelName) saved!", navigateToDashboard = true)
                return@launch
            }

            val apiKey = when (provider) {
                "GEMINI" -> settings.geminiApiKey.trim()
                "NVIDIA_NIM" -> settings.nvidiaApiKey.trim()
                "OPENAI" -> settings.openaiApiKey.trim()
                else -> ""
            }
            val baseUrl = when (provider) {
                "NVIDIA_NIM" -> settings.nvidiaBaseUrl.trim()
                "OPENAI" -> "https://api.openai.com/v1"
                else -> ""
            }
            val model = when (provider) {
                "GEMINI" -> settings.geminiModel
                "NVIDIA_NIM" -> settings.nvidiaModel
                "OPENAI" -> settings.openaiModel
                else -> ""
            }

            val result = repository.testApiKey(provider, apiKey, model, baseUrl)

            when (result) {
                is ApiKeyTestResult.Success -> {
                    repository.saveSettings(settings)
                    _saveResult.value = SaveResult(true, "API key verified and AI Model ($modelName) saved!", navigateToDashboard = true)
                }
                is ApiKeyTestResult.Failure -> {
                    _testError.value = result.error
                    if (result.error.contains("No internet", ignoreCase = true)
                        || result.error.contains("Network error", ignoreCase = true)
                        || result.error.contains("timed out", ignoreCase = true)) {
                        val localSettings = settings.copy(
                            aiProvider = "LOCAL",
                            geminiApiKey = settings.geminiApiKey,
                            nvidiaApiKey = settings.nvidiaApiKey,
                            openaiApiKey = settings.openaiApiKey
                        )
                        repository.saveSettings(localSettings)
                        _saveResult.value = SaveResult(true, "No internet — AI set to Local. ${result.error}", navigateToDashboard = true)
                    }
                    // Don't save on API failure
                }
            }
            _isTesting.value = false
        }
    }

    // --- Save General Settings (no API key test) ---
    fun saveGeneralSettings(goals: NutritionGoalEntity, settings: AppSettingsEntity) {
        viewModelScope.launch {
            _isTesting.value = true
            _testError.value = null

            repository.updateGoals(goals)
            repository.saveSettings(settings)

            _saveResult.value = SaveResult(true, "General settings saved!", navigateToDashboard = true)
            _isTesting.value = false
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }

    fun clearMessage() {
        _saveMessage.value = null
    }

    fun clearTestError() {
        _testError.value = null
    }
}

data class NutritionCalculationResult(
    val goal: String,
    val bmr: Int,
    val tdee: Int,
    val calories: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int,
    val adjustment: Int,
    val isUnsafeRate: Boolean
)
