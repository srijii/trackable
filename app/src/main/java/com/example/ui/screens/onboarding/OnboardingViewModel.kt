package com.example.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.NutritionGoalEntity
import com.example.data.repository.NutritionRepository
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

data class NutritionCalculationResult(
    val goal: String, // "gain", "loss", "maintenance"
    val bmr: Int,
    val tdee: Int,
    val calories: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int,
    val adjustment: Int,
    val isUnsafeRate: Boolean
)

class OnboardingViewModel(
    private val repository: NutritionRepository
) : ViewModel() {

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    private val _calculationResult = MutableStateFlow<NutritionCalculationResult?>(null)
    val calculationResult: StateFlow<NutritionCalculationResult?> = _calculationResult.asStateFlow()

    // Activity level multipliers (matching the UI dropdown labels)
    private val activityMultipliers = mapOf(
        "Sedentary" to 1.2,
        "Lightly Active" to 1.375,
        "Moderately Active" to 1.55,
        "Very Active" to 1.725,
        "Extra Active" to 1.9
    )

    /**
     * Calculate nutrition targets using Mifflin-St Jeor equation.
     * Safe weekly rate limits: gain 0.5% body weight/week, loss 1.0% body weight/week.
     */
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

        // 1. Determine goal (maintenance threshold = 1% of current body weight)
        val difference = goalWeight - weightW
        val threshold = weightW * 0.01
        val goal = when {
            difference > threshold -> "gain"
            difference < -threshold -> "loss"
            else -> "maintenance"
        }

        // 2. BMR — Mifflin-St Jeor
        val bmr = when (sex.lowercase(Locale.ROOT)) {
            "male" -> 10.0 * weightW + 6.25 * heightH - 5.0 * ageA + 5.0
            "female" -> 10.0 * weightW + 6.25 * heightH - 5.0 * ageA - 161.0
            else -> 10.0 * weightW + 6.25 * heightH - 5.0 * ageA + 5.0
        }

        // 3. TDEE
        val multiplier = activityMultipliers[activityLevel] ?: 1.55
        val tdee = bmr * multiplier

        // 4. Calorie adjustment with safety limits
        var adjustment = 0.0
        var isUnsafeRate = false

        if (goal != "maintenance") {
            adjustment = difference * 7700.0 / 182.0 // 6 months = 182 days

            val maxWeeklyRate = if (goal == "gain") 0.005 else 0.01 // 0.5% / 1.0%
            val maxDailyAdjustment = weightW * maxWeeklyRate * 7700.0 / 7.0

            val clampedAdjustment = adjustment.coerceIn(-maxDailyAdjustment, maxDailyAdjustment)
            if (clampedAdjustment != adjustment) {
                isUnsafeRate = true
            }
            adjustment = clampedAdjustment
        }

        // 5. Target calories
        val calories = (tdee + adjustment).coerceAtLeast(1200.0) // safety floor

        // 6. Protein
        val proteinMultiplier = when (goal) {
            "gain" -> 1.8
            "loss" -> 2.0
            else -> 1.6
        }
        val protein = weightW * proteinMultiplier

        // 7. Fat (25% of calories, min 0.6g/kg)
        val fatFromCalories = calories * 0.25 / 9.0
        val fatMin = weightW * 0.6
        val fat = maxOf(fatFromCalories, fatMin)

        // 8. Carbs from remaining calories
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

    fun saveOnboarding(
        name: String,
        age: Int?,
        sex: String?,
        weightKg: Double?,
        targetWeightKg: Double?,
        heightCm: Double?,
        activityLevel: String?,
        caloriesGoal: Double,
        proteinGoal: Double,
        carbsGoal: Double,
        fatGoal: Double,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            // Save personal profile as nutrition goals
            val goals = NutritionGoalEntity(
                name = name.trim(),
                sex = sex ?: "",
                caloriesGoal = caloriesGoal,
                proteinGoal = proteinGoal,
                carbsGoal = carbsGoal,
                fatGoal = fatGoal,
                currentWeightKg = weightKg,
                targetWeightKg = targetWeightKg,
                heightCm = heightCm,
                age = age,
                activityLevel = activityLevel ?: ""
            )
            repository.updateGoals(goals)

            // Mark onboarding as completed in app settings
            val current = repository.getSettings()
            val updatedSettings = current.copy(
                hasCompletedOnboarding = true,
                useStartDate = true,
                startDate = LocalDate.now().toString()
            )
            repository.saveSettings(updatedSettings)

            _saveMessage.value = "Profile saved successfully!"
            onComplete()
        }
    }

    fun clearMessage() {
        _saveMessage.value = null
    }
}
