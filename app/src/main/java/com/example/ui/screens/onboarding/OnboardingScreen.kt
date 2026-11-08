package com.example.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppSettingsEntity
import com.example.data.database.NutritionGoalEntity
import com.example.data.repository.NutritionRepository
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.ProgressCyanBlue
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val saveMessage by viewModel.saveMessage.collectAsStateWithLifecycle()
    val calculationResult by viewModel.calculationResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Step state ---
    var currentStep by remember { mutableStateOf(1) } // 1 = profile, 2 = targets

    // --- Page 1: Personal profile fields ---
    var name by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("Male") }
    var weightText by remember { mutableStateOf("") }
    var targetWeightText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var activityLevel by remember { mutableStateOf("Moderately Active") }

    // --- Page 2: Nutritional targets fields (auto-filled) ---
    var caloriesTarget by remember { mutableStateOf("") }
    var proteinTarget by remember { mutableStateOf("") }
    var carbsTarget by remember { mutableStateOf("") }
    var fatTarget by remember { mutableStateOf("") }

    val sexes = listOf("Male", "Female")
    val activityLevels = listOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extra Active")

    var sexMenuExpanded by remember { mutableStateOf(false) }
    var activityMenuExpanded by remember { mutableStateOf(false) }

    // Page 1 validation: name, age, weight, height required (sex & activity have defaults)
    val profileComplete = name.isNotBlank() &&
        ageText.isNotBlank() &&
        weightText.isNotBlank() &&
        heightText.isNotBlank()

    LaunchedEffect(saveMessage) {
        saveMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Auto-fill page 2 targets when entering page 2
    LaunchedEffect(currentStep) {
        if (currentStep == 2) {
            val age = ageText.toIntOrNull()
            val weight = weightText.toDoubleOrNull()
            val targetWeight = targetWeightText.toDoubleOrNull()
            val height = heightText.toDoubleOrNull()
            val result = viewModel.calculateNutrition(
                age = age,
                sex = sex,
                currentWeightKg = weight,
                targetWeightKg = targetWeight,
                heightCm = height,
                activityLevel = activityLevel
            )
            if (result != null) {
                caloriesTarget = result.calories.toString()
                proteinTarget = result.protein.toString()
                carbsTarget = result.carbs.toString()
                fatTarget = result.fat.toString()
            }
        }
    }

    fun saveAndFinish(skipTargets: Boolean) {
        val age = ageText.toIntOrNull()
        val weightKg = weightText.toDoubleOrNull()
        val targetWeightKg = targetWeightText.toDoubleOrNull()
        val heightCm = heightText.toDoubleOrNull()

        // Use calculated values if available, else defaults
        val cal = if (skipTargets) 2200.0 else caloriesTarget.toDoubleOrNull() ?: 2200.0
        val prot = if (skipTargets) 140.0 else proteinTarget.toDoubleOrNull() ?: 140.0
        val carbs = if (skipTargets) 220.0 else carbsTarget.toDoubleOrNull() ?: 220.0
        val fat = if (skipTargets) 65.0 else fatTarget.toDoubleOrNull() ?: 65.0

        viewModel.saveOnboarding(
            name = name.trim(),
            age = age,
            sex = sex,
            weightKg = weightKg,
            targetWeightKg = targetWeightKg,
            heightCm = heightCm,
            activityLevel = activityLevel,
            caloriesGoal = cal,
            proteinGoal = prot,
            carbsGoal = carbs,
            fatGoal = fat,
            onComplete = onFinish
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep == 2) {
                    IconButton(
                        onClick = { currentStep = 1 },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (currentStep == 1) "Personal Profile" else "Nutritional Targets",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Step $currentStep of 2",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress indicator
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (currentStep >= 1) ClassicRed else SurfaceBorder)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (currentStep >= 2) ClassicRed else SurfaceBorder)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (currentStep == 1) {
                                // Page 1: Personal Profile
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Your Personal Details",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                }

                                Text(
                                    text = "These help us tailor your nutrition tracking.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                )

                                // Name
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Full Name") },
                                    modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input"),
                                    colors = customTextFieldColors(),
                                    singleLine = true
                                )

                                // Sex dropdown
                                Box {
                                    OutlinedButton(
                                        onClick = { sexMenuExpanded = true },
                                        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("onboarding_sex_dropdown"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Biological Sex:", color = TextSecondary)
                                            Text(sex, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = sexMenuExpanded,
                                        onDismissRequest = { sexMenuExpanded = false },
                                        modifier = Modifier.background(SurfaceDark)
                                    ) {
                                        sexes.forEach { s ->
                                            DropdownMenuItem(
                                                text = { Text(s, color = TextPrimary) },
                                                onClick = {
                                                    sex = s
                                                    sexMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Age & Weight row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = ageText,
                                        onValueChange = { ageText = it.filter { c -> c.isDigit() } },
                                        label = { Text("Age (years)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f).testTag("onboarding_age_input"),
                                        colors = customTextFieldColors(),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = weightText,
                                        onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                                        label = { Text("Weight (kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f).testTag("onboarding_weight_input"),
                                        colors = customTextFieldColors(),
                                        singleLine = true
                                    )
                                }

                                // Target Weight & Height row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = targetWeightText,
                                        onValueChange = { targetWeightText = it.filter { c -> c.isDigit() || c == '.' } },
                                        label = { Text("Target Wt (kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f).testTag("onboarding_target_weight_input"),
                                        colors = customTextFieldColors(),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = heightText,
                                        onValueChange = { heightText = it.filter { c -> c.isDigit() || c == '.' } },
                                        label = { Text("Height (cm)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f).testTag("onboarding_height_input"),
                                        colors = customTextFieldColors(),
                                        singleLine = true
                                    )
                                }

                                // Activity level dropdown
                                Box {
                                    OutlinedButton(
                                        onClick = { activityMenuExpanded = true },
                                        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("onboarding_activity_dropdown"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Activity Level:", color = TextSecondary)
                                            Text(activityLevel, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = activityMenuExpanded,
                                        onDismissRequest = { activityMenuExpanded = false },
                                        modifier = Modifier.background(SurfaceDark)
                                    ) {
                                        activityLevels.forEach { lvl ->
                                            DropdownMenuItem(
                                                text = { Text(lvl, color = TextPrimary) },
                                                onClick = {
                                                    activityLevel = lvl
                                                    activityMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Continue button
                                Button(
                                    onClick = {
                                        if (!profileComplete) return@Button
                                        currentStep = 2
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("onboarding_continue_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (profileComplete) ClassicRed else SurfaceBorder,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = "Continue to Targets",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            } else {
                                // Page 2: Nutritional Targets (auto-filled)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = "Targets",
                                        tint = ProgressCyanBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Daily Nutritional Targets",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                }

                                Text(
                                    text = "Auto-calculated from your profile. Adjust if needed, or tap “Set Later” to use defaults.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                )

                                // Calories row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = caloriesTarget,
                                        onValueChange = { caloriesTarget = it.filter { c -> c.isDigit() } },
                                        label = { Text("Calories (kcal)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f).testTag("onboarding_calories_input"),
                                        colors = customTextFieldColors(),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = proteinTarget,
                                        onValueChange = { proteinTarget = it.filter { c -> c.isDigit() } },
                                        label = { Text("Protein (g)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f).testTag("onboarding_protein_input"),
                                        colors = customTextFieldColors(),
                                        singleLine = true
                                    )
                                }

                                // Carbs & Fat row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = carbsTarget,
                                        onValueChange = { carbsTarget = it.filter { c -> c.isDigit() } },
                                        label = { Text("Carbs (g)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f).testTag("onboarding_carbs_input"),
                                        colors = customTextFieldColors(),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = fatTarget,
                                        onValueChange = { fatTarget = it.filter { c -> c.isDigit() } },
                                        label = { Text("Fat (g)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f).testTag("onboarding_fat_input"),
                                        colors = customTextFieldColors(),
                                        singleLine = true
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            saveAndFinish(skipTargets = false)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("onboarding_save_targets_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ClassicRed,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Save & Finish", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = Color(0xFF13171F),
    unfocusedContainerColor = Color(0xFF13171F),
    focusedBorderColor = ProgressCyanBlue,
    unfocusedBorderColor = SurfaceBorder
)
