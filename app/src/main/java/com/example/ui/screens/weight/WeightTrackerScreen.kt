package com.example.ui.screens.weight

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.WeightLogEntity
import com.example.ui.components.EmptyState
import com.example.ui.components.MetricBlock
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrackableDivider
import com.example.ui.components.WeightHistorySkeleton
import com.example.ui.components.WeightSummarySkeleton
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCardHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeightTrackerScreen(
    viewModel: WeightTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val weightLogs by viewModel.weightLogsDesc.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    val loaded = goals != null
    val latestLog = weightLogs.firstOrNull()
    val currentWeight = latestLog?.weightKg ?: goals?.currentWeightKg ?: 75.0
    val targetWeight = goals?.targetWeightKg ?: 70.0
    val heightCm = goals?.heightCm ?: 175.0

    // Form state
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var weightInputText by remember(currentWeight) {
        mutableStateOf(String.format(Locale.US, "%.1f", currentWeight))
    }
    var noteInputText by remember { mutableStateOf("") }
    var dateMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("weight_tracker_screen"),
        containerColor = BackgroundDark,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp)
        ) {
            if (!loaded) {
                item {
                    WeightSummarySkeleton(modifier = Modifier.padding(top = 8.dp))
                }
                item {
                    WeightHistorySkeleton(
                        rows = 4,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            } else {
                // Current weight is the hero: one big number, supporting stats
                // below it, no container competing with the value.
                item {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CURRENT WEIGHT",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                modifier = Modifier.weight(1f)
                            )
                            latestLog?.let { log ->
                                val logDate = LocalDate.ofEpochDay(log.dateEpochDay)
                                val dateStr = if (logDate == LocalDate.now()) "today"
                                else logDate.format(DateTimeFormatter.ofPattern("MMM d"))
                                Text(
                                    text = "Updated $dateStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%.1f", currentWeight),
                                style = MaterialTheme.typography.displayLarge,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "kg",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 7.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        val diffToTarget = currentWeight - targetWeight
                        val heightM = heightCm / 100.0
                        val bmi = if (heightM > 0) currentWeight / (heightM * heightM) else 0.0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            MetricBlock(
                                label = "Target",
                                value = "${String.format(Locale.US, "%.1f", targetWeight)} kg",
                                modifier = Modifier.weight(1f)
                            )
                            MetricBlock(
                                label = "To go",
                                value = "${if (diffToTarget > 0) "-" else "+"}${
                                    String.format(Locale.US, "%.1f", kotlin.math.abs(diffToTarget))
                                } kg",
                                valueColor = if (diffToTarget <= 0) SuccessGreen else ClassicRed,
                                modifier = Modifier.weight(1f)
                            )
                            MetricBlock(
                                label = "BMI",
                                value = String.format(Locale.US, "%.1f", bmi),
                                supporting = bmiLabel(bmi),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Record form: compact fields with a single dominant action.
                item {
                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        SectionHeader(
                            title = "Record weigh-in",
                            supportingText = "Skipped days never break your history."
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { dateMenuExpanded = true }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = dateLabel(selectedDate),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Choose weigh-in date",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = dateMenuExpanded,
                                onDismissRequest = { dateMenuExpanded = false },
                                modifier = Modifier.background(SurfaceCardHigh)
                            ) {
                                listOf(
                                    LocalDate.now() to "Today",
                                    LocalDate.now().minusDays(1) to "Yesterday",
                                    LocalDate.now().minusDays(2) to "2 days ago",
                                    LocalDate.now().minusDays(3) to "3 days ago",
                                    LocalDate.now().minusDays(4) to "4 days ago",
                                    LocalDate.now().minusDays(7) to "1 week ago"
                                ).forEach { (date, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "$label (${date.format(DateTimeFormatter.ofPattern("MMM d"))})",
                                                color = TextPrimary
                                            )
                                        },
                                        onClick = {
                                            selectedDate = date
                                            dateMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = weightInputText,
                                onValueChange = { weightInputText = it },
                                suffix = { Text("kg", color = TextMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("weight_input_field"),
                                shape = RoundedCornerShape(12.dp),
                                colors = weightFieldColors()
                            )

                            Button(
                                onClick = {
                                    val w = weightInputText.toDoubleOrNull()
                                    if (w != null && w in 20.0..300.0) {
                                        viewModel.logWeight(selectedDate, w, noteInputText.trim())
                                        noteInputText = ""
                                    }
                                },
                                modifier = Modifier
                                    .height(56.dp)
                                    .testTag("save_weight_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ClassicRed,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Save",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Fine adjustment: small, quiet deltas next to the field.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(-0.5, -0.1, 0.1, 0.5).forEach { delta ->
                                val prefix = if (delta > 0) "+$delta" else "$delta"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceCardHigh)
                                        .clickable {
                                            val cur = weightInputText.toDoubleOrNull() ?: currentWeight
                                            val newW = (cur + delta).coerceAtLeast(20.0)
                                            weightInputText = String.format(Locale.US, "%.1f", newW)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = prefix,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (delta > 0) ClassicRed else TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = noteInputText,
                            onValueChange = { noteInputText = it },
                            placeholder = {
                                Text(
                                    "Note (optional) — morning fasted, post-workout…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = weightFieldColors()
                        )
                    }
                }

                // History: flat rows, hairline separators, compact deltas.
                item {
                    SectionHeader(
                        title = "Weigh-in history",
                        trailing = {
                            Text(
                                text = "${weightLogs.size} recorded",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                        },
                        modifier = Modifier.padding(top = 24.dp, bottom = 2.dp)
                    )
                }

                if (weightLogs.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.MonitorWeight,
                            title = "No weigh-ins yet",
                            message = "Log your weight above to start the trend."
                        )
                    }
                } else {
                    itemsIndexed(weightLogs, key = { _, log -> log.id }) { index, log ->
                        val nextOlderLog = weightLogs.getOrNull(index + 1)
                        val diff = if (nextOlderLog != null) log.weightKg - nextOlderLog.weightKg else null
                        WeightHistoryRow(
                            log = log,
                            diff = diff,
                            onDelete = { viewModel.deleteWeightLog(log) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun WeightHistoryRow(
    log: WeightLogEntity,
    diff: Double?,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logDate = LocalDate.ofEpochDay(log.dateEpochDay)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (logDate == LocalDate.now()) "Today"
                        else logDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    if (logDate == LocalDate.now()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "· latest",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
                if (log.note.isNotBlank()) {
                    Text(
                        text = log.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }

            if (diff != null) {
                val isLoss = diff < 0
                val icon = when {
                    isLoss -> Icons.AutoMirrored.Filled.TrendingDown
                    diff > 0 -> Icons.AutoMirrored.Filled.TrendingUp
                    else -> Icons.AutoMirrored.Filled.TrendingFlat
                }
                val color = when {
                    isLoss -> SuccessGreen
                    diff > 0 -> ClassicRed
                    else -> TextMuted
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${if (diff > 0) "+" else ""}${String.format(Locale.US, "%.1f", diff)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = color
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Text(
                text = String.format(Locale.US, "%.1f kg", log.weightKg),
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete weigh-in",
                    tint = TextMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        TrackableDivider(color = SurfaceBorder.copy(alpha = 0.45f))
    }
}

@Composable
private fun weightFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
    focusedBorderColor = ClassicRed.copy(alpha = 0.75f),
    unfocusedBorderColor = SurfaceBorder.copy(alpha = 0.7f)
)

private fun dateLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> "Today, ${date.format(DateTimeFormatter.ofPattern("MMM d"))}"
    LocalDate.now().minusDays(1) -> "Yesterday"
    else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
}

private fun bmiLabel(bmi: Double): String = when {
    bmi <= 0.0 -> ""
    bmi < 18.5 -> "Under"
    bmi < 25.0 -> "Normal"
    bmi < 30.0 -> "Over"
    else -> "Obese"
}
