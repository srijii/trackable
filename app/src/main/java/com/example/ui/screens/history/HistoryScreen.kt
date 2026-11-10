package com.example.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.FoodLogEntity
import com.example.ui.components.EmptyState
import com.example.ui.components.HistoryDaySkeleton
import com.example.ui.components.MacroSummaryLine
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrackableDivider
import com.example.ui.components.formatFoodQuantity
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WaterCyan
import com.example.util.DateBounds
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val summary by viewModel.dailySummary.collectAsStateWithLifecycle()
    val canGoPreviousDay by viewModel.canGoPreviousDay.collectAsStateWithLifecycle()
    val canGoNextDay by viewModel.canGoNextDay.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    val loaded = appSettings != null
    val dateFormatted = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
    }
    val groupedLogs = remember(summary.foodLogs) {
        summary.foodLogs.groupBy { it.mealType }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen"),
        containerColor = BackgroundDark,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 4.dp,
                end = 16.dp,
                bottom = 24.dp
            )
        ) {
            // Compact date navigation, no surrounding card.
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.previousDay() },
                        enabled = canGoPreviousDay,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Day",
                            tint = if (canGoPreviousDay) TextSecondary else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDatePicker = true }
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dateFormatted,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            if (selectedDate == LocalDate.now()) {
                                Text(
                                    text = "Today",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ClassicRed
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Choose date",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextDay() },
                        enabled = canGoNextDay,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Day",
                            tint = if (canGoNextDay) TextSecondary else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            if (!loaded) {
                item {
                    HistoryDaySkeleton(modifier = Modifier.padding(top = 18.dp))
                }
            } else {
                // Day summary on the background.
                item {
                    Column(modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CALORIES",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                                    color = TextMuted
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${summary.totalCalories.toInt()}",
                                        style = MaterialTheme.typography.displaySmall,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "/ ${summary.caloriesGoal.toInt()} kcal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "WATER",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                                    color = TextMuted
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "%.1f L",
                                        summary.totalWaterMl / 1000.0
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WaterCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            SummaryMacro(
                                "Protein",
                                summary.totalProtein,
                                summary.proteinGoal,
                                Modifier.weight(1f)
                            )
                            SummaryMacro(
                                "Carbs",
                                summary.totalCarbs,
                                summary.carbsGoal,
                                Modifier.weight(1f)
                            )
                            SummaryMacro(
                                "Fat",
                                summary.totalFat,
                                summary.fatGoal,
                                Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        TrackableDivider()
                    }
                }

                if (summary.foodLogs.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.History,
                            title = "No entries for this day",
                            message = "Foods logged on this date will appear here."
                        )
                    }
                } else {
                    listOf("Breakfast", "Lunch", "Dinner", "Snack").forEach { mealCategory ->
                        val mealItems = groupedLogs[mealCategory] ?: emptyList()
                        if (mealItems.isNotEmpty()) {
                            item(key = "header_$mealCategory") {
                                SectionHeader(
                                    title = mealCategory,
                                    supportingText = "${mealItems.size} item${if (mealItems.size == 1) "" else "s"}",
                                    trailing = {
                                        Text(
                                            text = "${mealItems.sumOf { it.calories }.toInt()} kcal",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary
                                        )
                                    },
                                    modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)
                                )
                            }

                            items(mealItems, key = { it.id }) { log ->
                                HistoryFoodRow(
                                    log = log,
                                    onDelete = { viewModel.deleteLog(log) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showDatePicker) {
        HistoryDatePickerDialog(
            initialDate = selectedDate,
            earliestDate = DateBounds.earliestAllowedDate(appSettings),
            latestDate = DateBounds.latestAllowedDate(appSettings),
            onConfirm = { date ->
                viewModel.setDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun SummaryMacro(
    label: String,
    consumed: Double,
    target: Double,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${consumed.toInt()}",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = " / ${target.toInt()}g",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun HistoryFoodRow(
    log: FoodLogEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val time = remember(log.timestamp) {
        Instant.ofEpochMilli(log.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.foodNameSnapshot,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = buildString {
                    append(time)
                    append(" · ")
                    append(formatFoodQuantity(log.quantity))
                    append(" ")
                    append(log.servingUnit)
                    if (log.brandSnapshot.isNotBlank()) {
                        append(" · ")
                        append(log.brandSnapshot)
                    }
                },
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                modifier = Modifier.padding(top = 1.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            MacroSummaryLine(
                protein = log.protein,
                carbs = log.carbohydrates,
                fat = log.fat
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${log.calories.toInt()}",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = "kcal",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Entry options",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(SurfaceDark)
            ) {
                DropdownMenuItem(
                    text = { Text("Delete entry", color = Color(0xFFEF4444)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        onDelete()
                        menuExpanded = false
                    }
                )
            }
        }
    }
    TrackableDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDatePickerDialog(
    initialDate: LocalDate,
    earliestDate: LocalDate?,
    latestDate: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                if (earliestDate != null && date < earliestDate) return false
                if (latestDate != null && date > latestDate) return false
                return true
            }

            override fun isSelectableYear(year: Int): Boolean {
                if (earliestDate != null && year < earliestDate.year) return false
                if (latestDate != null && year > latestDate.year) return false
                return true
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text("Select", color = ClassicRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = "Select a day",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    color = TextPrimary
                )
            }
        )
    }
}
