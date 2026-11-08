package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.FoodLogEntity
import com.example.data.database.NutritionGoalEntity
import com.example.ui.components.AiConfirmationDialog
import com.example.ui.screens.inventory.NutritionLabelScannerDialog
import com.example.ui.components.ScreenshotAiInputBar
import com.example.ui.components.ScreenshotCalorieGoalWarningBanner
import com.example.ui.components.ScreenshotCaloriesAndMacrosCard
import com.example.ui.components.ScreenshotFoodLogGroupCard
import com.example.ui.components.ScreenshotTopHeader
import com.example.ui.components.ScreenshotWaterCard
import com.example.ui.components.ScreenshotWeekCalendarStrip
import java.time.LocalDate

import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.ProgressCyanBlue
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

import com.example.ui.components.AnimatedWaterCard

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenDrawer: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val summary by viewModel.dailySummary.collectAsStateWithLifecycle()
    val weekSummaries by viewModel.weekSummaries.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val aiIsLoading by viewModel.aiIsLoading.collectAsStateWithLifecycle()
    val aiParseResult by viewModel.aiParseResult.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var editingLog by remember { mutableStateOf<FoodLogEntity?>(null) }
    var showCameraDialog by remember { mutableStateOf(false) }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedbackMessage()
        }
    }

    // Group logs by originalPrompt (or meal if no prompt)
    val groupedMeals = remember(summary.foodLogs) {
        summary.foodLogs.groupBy {
            if (it.originalPrompt.isNotBlank()) it.originalPrompt else "Meal (${it.mealType})"
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        containerColor = BackgroundDark,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(modifier = Modifier.navigationBarsPadding()) {
                ScreenshotAiInputBar(
                    isLoading = aiIsLoading,
                    onSendText = { text -> viewModel.onSendAiPrompt(text) },
                    onCameraClick = { showCameraDialog = true }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Top Header: Menu (opens left sidebar), "Today ▾"
            item {
                ScreenshotTopHeader(
                    selectedDate = selectedDate,
                    onMenuClick = onOpenDrawer,
                    onDateSelect = { viewModel.setSelectedDate(it) }
                )
            }

            // 2. Week Calendar Strip (Sun 1 ... Sat 7) with previous / next week
            val startDate = appSettings?.startDate?.let { LocalDate.parse(it) }
            item {
                ScreenshotWeekCalendarStrip(
                    selectedDate = selectedDate,
                    startDate = startDate,
                    useStartDate = appSettings?.useStartDate == true,
                    blockBeforeStartDate = appSettings?.blockBeforeStartDate == true,
                    blockAfterToday = appSettings?.blockAfterToday == true,
                    weekSummaries = weekSummaries,
                    onSelectDate = { viewModel.setSelectedDate(it) },
                    onPreviousWeek = { viewModel.previousWeek() },
                    onNextWeek = { viewModel.nextWeek() }
                )
            }

            // 3. Set Your Calorie Goal Warning Banner
            val isWarningDismissed = appSettings?.dismissedGoalWarning == true
            val goalsFilled = goals?.name?.isNotBlank() == true &&
                goals?.sex?.isNotBlank() == true &&
                goals?.age != null &&
                goals?.currentWeightKg != null &&
                goals?.heightCm != null
            val targetsFilled = goals?.caloriesGoal != 2200.0 ||
                goals?.proteinGoal != 140.0 ||
                goals?.carbsGoal != 220.0 ||
                goals?.fatGoal != 65.0
            val showOnboardingWarning = !goalsFilled || !targetsFilled

            if (!isWarningDismissed && showOnboardingWarning) {
                item {
                    ScreenshotCalorieGoalWarningBanner(
                        goals = goals ?: NutritionGoalEntity(),
                        onTap = onNavigateToSettings,
                        onDismiss = { viewModel.dismissGoalWarning() }
                    )
                }
            }

            // 4. Main Combined Calories & Macros Card
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ScreenshotCaloriesAndMacrosCard(
                        summary = summary,
                        exerciseCalories = 0
                    )
                }
            }

            // 5. Water Card (Fluid Animated Wave Progress)
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AnimatedWaterCard(
                        waterConsumedMl = summary.totalWaterMl,
                        waterGoalMl = summary.waterGoalMl,
                        cupSize1Ml = appSettings?.cupSize1Ml ?: 250,
                        cupSize2Ml = appSettings?.cupSize2Ml ?: 500,
                        onAddCup1 = { viewModel.addWaterCupSize1() },
                        onRemoveCup = { viewModel.removeWaterCup() },
                        onAddCup2 = { viewModel.addWaterCupSize2() }
                    )
                }
            }

            // 6. Food Log History Section
            if (summary.foodLogs.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No foods logged for this date",
                                    style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Type e.g. \"46g paneer\" in the bottom bar to log!",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    }
                }
            } else {
                groupedMeals.forEach { (_, mealLogs) ->
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ScreenshotFoodLogGroupCard(
                                logs = mealLogs,
                                goalCalories = summary.caloriesGoal,
                                goalCarbs = summary.carbsGoal,
                                goalProtein = summary.proteinGoal,
                                goalFat = summary.fatGoal,
                                onEditLog = { editingLog = it },
                                onDeleteLog = { viewModel.deleteFoodLog(it) }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // AI Parse Confirmation Modal
    aiParseResult?.let { result ->
        AiConfirmationDialog(
            parseResult = result,
            inventory = inventory,
            onConfirm = { confirmedItems, waterMl ->
                viewModel.confirmAiLog(confirmedItems, waterMl)
            },
            onDismiss = { viewModel.dismissAiDialog() }
        )
    }

    // Edit Quantity Dialog
    editingLog?.let { logToEdit ->
        var editQtyText by remember { mutableStateOf("${logToEdit.quantity}") }
        AlertDialog(
            onDismissRequest = { editingLog = null },
            title = {
                Text(
                    text = "Edit ${logToEdit.foodNameSnapshot}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Current serving unit: ${logToEdit.servingUnit}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editQtyText,
                        onValueChange = { editQtyText = it },
                        label = { Text("Quantity (${logToEdit.servingUnit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_quantity_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newQty = editQtyText.toDoubleOrNull()
                        if (newQty != null && newQty > 0.0) {
                            viewModel.updateFoodLogQuantity(logToEdit, newQty)
                        }
                        editingLog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProgressCyanBlue, contentColor = Color.White),
                    modifier = Modifier.testTag("save_edit_quantity_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingLog = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Camera Scan Dialog
    if (showCameraDialog) {
        NutritionLabelScannerDialog(
            onDismiss = { showCameraDialog = false },
            onExtracted = { scannedFood ->
                showCameraDialog = false
                viewModel.onSendAiPrompt("I ate ${scannedFood.name}")
            }
        )
    }
}
