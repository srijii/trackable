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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.FoodLogEntity
import com.example.data.database.NutritionGoalEntity
import com.example.ui.components.AiConfirmationDialog
import com.example.ui.components.DashboardAiAnalysingCard
import com.example.ui.components.DashboardAiLoggedItems
import com.example.ui.components.DashboardFoodSkeleton
import com.example.ui.components.DashboardStatsSkeleton
import com.example.ui.components.EmptyState
import com.example.ui.components.MealEntryDialog
import com.example.ui.components.ScannedFoodDialog
import com.example.ui.components.ScreenshotAiInputBar
import com.example.ui.components.ScreenshotCalorieGoalWarningBanner
import com.example.ui.components.ScreenshotCaloriesAndMacrosCard
import com.example.ui.components.ScreenshotFoodLogGroupCard
import com.example.ui.components.ScreenshotTopHeader
import com.example.ui.components.ScreenshotWeekCalendarStrip
import com.example.ui.components.SectionHeader
import com.example.ui.components.WaterCardSkeleton
import com.example.ui.screens.inventory.FoodEditDialog
import com.example.ui.screens.inventory.NutritionLabelScannerDialog
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ProgressCyanBlue
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.time.LocalDate
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
    val aiStage by viewModel.aiStage.collectAsStateWithLifecycle()
    val aiParseResult by viewModel.aiParseResult.collectAsStateWithLifecycle()
    val aiFailure by viewModel.aiFailure.collectAsStateWithLifecycle()
    val aiLoggedSummary by viewModel.aiLoggedSummary.collectAsStateWithLifecycle()
    val canGoPreviousWeek by viewModel.canGoPreviousWeek.collectAsStateWithLifecycle()
    val canGoNextWeek by viewModel.canGoNextWeek.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var viewingLog by remember { mutableStateOf<FoodLogEntity?>(null) }
    var showCameraDialog by remember { mutableStateOf(false) }
    var scannedFood by remember { mutableStateOf<com.example.data.database.FoodEntity?>(null) }
    var editingScannedFood by remember { mutableStateOf<com.example.data.database.FoodEntity?>(null) }

    val settingsLoaded = appSettings != null

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedbackMessage()
        }
    }

    // While the AI result rows are still typing themselves in, hide the real
    // log rows they represent. Dismissing the result hands them over to the
    // normal timeline, so the same nutrition is never shown twice.
    val visibleLogs = remember(summary.foodLogs, aiLoggedSummary) {
        val hiddenIds = aiLoggedSummary?.logIds.orEmpty()
        if (hiddenIds.isEmpty()) summary.foodLogs
        else summary.foodLogs.filterNot { it.id in hiddenIds }
    }

    // Group logs by originalPrompt (or meal if no prompt)
    val groupedMeals = remember(visibleLogs) {
        visibleLogs.groupBy {
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
                    onDateSelect = { viewModel.setSelectedDate(it) },
                    allowFutureDates = appSettings?.blockAfterToday == false
                )
            }

            // 2. Week Calendar Strip (Sun 1 ... Sat 7) with bounded previous / next week
            val startDate = appSettings?.startDate?.let { LocalDate.parse(it) }
            item {
                ScreenshotWeekCalendarStrip(
                    selectedDate = selectedDate,
                    startDate = startDate,
                    useStartDate = appSettings?.useStartDate == true,
                    blockBeforeStartDate = appSettings?.blockBeforeStartDate == true,
                    blockAfterToday = appSettings?.blockAfterToday == true,
                    canGoPreviousWeek = canGoPreviousWeek,
                    canGoNextWeek = canGoNextWeek,
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

            if (!settingsLoaded) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        DashboardStatsSkeleton()
                    }
                }
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        WaterCardSkeleton()
                    }
                }
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        DashboardFoodSkeleton(rows = 2)
                    }
                }
            } else {
                if (!isWarningDismissed && showOnboardingWarning) {
                    item {
                        ScreenshotCalorieGoalWarningBanner(
                            goals = goals ?: NutritionGoalEntity(),
                            onTap = onNavigateToSettings,
                            onDismiss = { viewModel.dismissGoalWarning() }
                        )
                    }
                }

                // 4. Primary statistics: calories and macros, directly on the background.
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        ScreenshotCaloriesAndMacrosCard(
                            summary = summary,
                            exerciseCalories = 0
                        )
                    }
                }

                // 5. Water (animated wave vessel preserved, compact presentation).
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
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
            }

            // 6. Immediate AI skeleton, shaped like the food rows it will become.
            if (aiIsLoading) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        DashboardAiAnalysingCard(
                            stage = aiStage ?: com.example.ui.screens.dashboard.AiProcessingStage.ANALYSING,
                            onCancel = { viewModel.cancelAiRequest() }
                        )
                    }
                }
            }

            // 7. Real-AI result writes itself into food-row geometry (no popup).
            aiLoggedSummary?.let { logged ->
                if (!aiIsLoading) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            DashboardAiLoggedItems(
                                summary = logged,
                                onDismiss = { viewModel.dismissAiLoggedSummary() }
                            )
                        }
                    }
                }
            }

            // 8. Food log history
            if (settingsLoaded) {
                if (visibleLogs.isEmpty() && !aiIsLoading && aiLoggedSummary == null) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EmptyState(
                                icon = Icons.Default.Restaurant,
                                title = "Nothing logged yet",
                                message = "Describe what you ate in the bar below to add it to this day."
                            )
                        }
                    }
                } else if (visibleLogs.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Today's meals",
                            supportingText = "Tap an item to see its details",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    groupedMeals.forEach { (groupKey, mealLogs) ->
                        item(key = "meal_$groupKey") {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .animateItem()
                            ) {
                                ScreenshotFoodLogGroupCard(
                                    logs = mealLogs,
                                    goalCalories = summary.caloriesGoal,
                                    goalCarbs = summary.carbsGoal,
                                    goalProtein = summary.proteinGoal,
                                    goalFat = summary.fatGoal,
                                    onEditLog = { viewingLog = it },
                                    onDeleteLog = { viewModel.deleteFoodLog(it) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Local Smart Engine is the only provider that asks for confirmation.
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

    // AI failure dialog: retry, or explicitly pick another configured provider.
    aiFailure?.let { failure ->
        AiFailureDialog(
            failure = failure,
            onRetry = { viewModel.retryFailedPrompt() },
            onSwitchAndRetry = { providerId -> viewModel.switchProviderAndRetry(providerId) },
            onDismiss = { viewModel.dismissAiFailure() }
        )
    }

    // Tapping a logged food opens that meal entry: what it was, how much was
    // eaten and what it added up to. Editing changes the entry, never the
    // food in the inventory.
    viewingLog?.let { logToView ->
        MealEntryDialog(
            log = logToView,
            matchedFood = inventory.firstOrNull { it.id == logToView.foodId },
            onSave = { updatedLog ->
                viewModel.updateFoodLogEntry(updatedLog)
                viewingLog = null
            },
            onDelete = {
                viewModel.deleteFoodLog(logToView)
                viewingLog = null
            },
            onDismiss = { viewingLog = null }
        )
    }

    // Label scan from the dashboard: review the extracted food before saving.
    if (showCameraDialog) {
        NutritionLabelScannerDialog(
            onDismiss = { showCameraDialog = false },
            onExtracted = { food ->
                showCameraDialog = false
                scannedFood = food
            },
            scanLabel = viewModel::scanNutritionLabel
        )
    }

    scannedFood?.let { foodToReview ->
        ScannedFoodDialog(
            food = foodToReview,
            onEdit = {
                scannedFood = null
                editingScannedFood = foodToReview
            },
            onSave = {
                viewModel.saveScannedFoodAndLog(foodToReview)
                scannedFood = null
            },
            onDismiss = { scannedFood = null },
            titleOverride = "Scanned food",
            saveLabel = "Add to inventory & log"
        )
    }

    // Editing the scanned food before it is saved.
    editingScannedFood?.let { foodToEdit ->
        FoodEditDialog(
            food = foodToEdit,
            onDismiss = { editingScannedFood = null },
            onSave = { reviewedFood ->
                viewModel.saveScannedFoodAndLog(reviewedFood)
                editingScannedFood = null
            },
            titleOverride = "Edit scanned food",
            saveLabel = "Add to inventory & log"
        )
    }
}

@Composable
private fun AiFailureDialog(
    failure: AiFailureState,
    onRetry: () -> Unit,
    onSwitchAndRetry: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialProvider = remember(failure) {
        failure.providerOptions
            .filter { it.id != "LOCAL" }
            .firstOrNull { !failure.providerUsed.startsWith(it.label.substringBefore(" (")) }
            ?.id
            ?: "LOCAL"
    }
    var selectedProviderId by remember(failure) { mutableStateOf(initialProvider) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val selectedLabel = failure.providerOptions
        .firstOrNull { it.id == selectedProviderId }
        ?.label
        ?: "Local Smart Engine"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        modifier = Modifier.testTag("ai_failure_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "AI request failed",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = failure.message,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Used: ${failure.providerUsed}",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )

                if (failure.canChooseProvider) {
                    Text(
                        text = "Switch to another configured provider:",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Box {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = selectedLabel,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextMuted
                            )
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            failure.providerOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            color = if (option.id == selectedProviderId) ClassicRed else TextPrimary
                                        )
                                    },
                                    onClick = {
                                        selectedProviderId = option.id
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Only this provider is configured. Retry, or continue with the Local Smart Engine — no silent fallback happens.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onRetry, modifier = Modifier.testTag("ai_failure_retry")) {
                    Text("Retry", color = TextSecondary)
                }
                Button(
                    onClick = {
                        onSwitchAndRetry(if (failure.canChooseProvider) selectedProviderId else "LOCAL")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClassicRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("ai_failure_switch")
                ) {
                    Text(if (failure.canChooseProvider) "Switch & Retry" else "Use Local Engine")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
