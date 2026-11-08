package com.example.ui.screens.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.example.data.database.WeightLogEntity
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.ClassicRedGlow
import com.example.ui.theme.SelectedRedBorder
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCardHigh
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfacePill
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    val latestLog = weightLogs.firstOrNull()
    val currentWeight = latestLog?.weightKg ?: goals?.currentWeightKg ?: 75.0
    val targetWeight = goals?.targetWeightKg ?: 70.0
    val heightCm = goals?.heightCm ?: 175.0

    // Form state
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var weightInputText by remember(currentWeight) { mutableStateOf(String.format(java.util.Locale.US, "%.1f", currentWeight)) }
    var noteInputText by remember { mutableStateOf("") }
    var dateMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("weight_tracker_screen"),
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Text(
                    text = "BODY COMPOSITION",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ClassicRed,
                        letterSpacing = 1.2.sp
                    )
                )
                Text(
                    text = "Weight Tracker",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            // 1. Current Weight & Target Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ClassicRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Scale,
                                        contentDescription = null,
                                        tint = ClassicRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Current Weight",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }

                            if (latestLog != null) {
                                val logDate = LocalDate.ofEpochDay(latestLog.dateEpochDay)
                                val dateStr = if (logDate == LocalDate.now()) "Today" else logDate.format(DateTimeFormatter.ofPattern("MMM d"))
                                Text(
                                    text = "Updated $dateStr",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                            }
                        }

                        // Big Weight readout
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", currentWeight),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "kg",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = ClassicRed,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Targets & Stats Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF13151A))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // Target
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TARGET", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.1f", targetWeight)} kg",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                                )
                            }

                            // Diff to Target
                            val diffToTarget = currentWeight - targetWeight
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TO GO", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
                                Spacer(modifier = Modifier.height(2.dp))
                                val sign = if (diffToTarget > 0) "-" else "+"
                                Text(
                                    text = "$sign${String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(diffToTarget))} kg",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (diffToTarget <= 0) Color(0xFF10B981) else ClassicRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            // BMI
                            val heightM = heightCm / 100.0
                            val bmi = if (heightM > 0) currentWeight / (heightM * heightM) else 0.0
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("BMI", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f", bmi),
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Log / Edit Weight Entry Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Record Weigh-in",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Log your weight on days you step on the scale. Skipped days are handled automatically without breaking streaks or graphs.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                        )

                        // Date Selector
                        Box {
                            OutlinedButton(
                                onClick = { dateMenuExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = ClassicRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Date:", color = TextMuted)
                                    }
                                    val dateLabel = when (selectedDate) {
                                        LocalDate.now() -> "Today (${selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))})"
                                        LocalDate.now().minusDays(1) -> "Yesterday"
                                        else -> selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
                                    }
                                    Text(dateLabel, color = TextPrimary, fontWeight = FontWeight.Bold)
                                }
                            }

                            DropdownMenu(
                                expanded = dateMenuExpanded,
                                onDismissRequest = { dateMenuExpanded = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                listOf(
                                    LocalDate.now() to "Today",
                                    LocalDate.now().minusDays(1) to "Yesterday",
                                    LocalDate.now().minusDays(2) to "2 days ago",
                                    LocalDate.now().minusDays(3) to "3 days ago",
                                    LocalDate.now().minusDays(4) to "4 days ago",
                                    LocalDate.now().minusDays(7) to "1 week ago"
                                ).forEach { (d, label) ->
                                    DropdownMenuItem(
                                        text = { Text("$label (${d.format(DateTimeFormatter.ofPattern("MMM d"))})", color = TextPrimary) },
                                        onClick = {
                                            selectedDate = d
                                            dateMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Weight Input Field + Adjustment Quick Buttons
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = weightInputText,
                                onValueChange = { weightInputText = it },
                                label = { Text("Weight (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("weight_input_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = Color(0xFF13151A),
                                    unfocusedContainerColor = Color(0xFF13151A),
                                    focusedBorderColor = ClassicRed,
                                    unfocusedBorderColor = SurfaceBorder
                                )
                            )

                            // Quick Bump Buttons (-0.5, -0.1, +0.1, +0.5)
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
                                            .background(Color(0xFF1E2129))
                                            .clickable {
                                                val cur = weightInputText.toDoubleOrNull() ?: currentWeight
                                                val newW = (cur + delta).coerceAtLeast(20.0)
                                                weightInputText = String.format(java.util.Locale.US, "%.1f", newW)
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = prefix,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (delta > 0) ClassicRed else TextSecondary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Note input
                        OutlinedTextField(
                            value = noteInputText,
                            onValueChange = { noteInputText = it },
                            label = { Text("Note (optional)") },
                            placeholder = { Text("e.g., Morning fasted, post-workout") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = Color(0xFF13151A),
                                unfocusedContainerColor = Color(0xFF13151A),
                                focusedBorderColor = ClassicRed,
                                unfocusedBorderColor = SurfaceBorder
                            )
                        )

                        // Save Button
                        Button(
                            onClick = {
                                val w = weightInputText.toDoubleOrNull()
                                if (w != null && w in 20.0..300.0) {
                                    viewModel.logWeight(selectedDate, w, noteInputText.trim())
                                    noteInputText = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_weight_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ClassicRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Weight Entry",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }

            // 3. Weight History List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weigh-in History",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "${weightLogs.size} recorded",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }

            if (weightLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceDark)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No weight logs recorded yet. Log your first weight above!",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                        )
                    }
                }
            } else {
                itemsIndexed(weightLogs) { index, log ->
                    val nextOlderLog = weightLogs.getOrNull(index + 1)
                    val diff = if (nextOlderLog != null) log.weightKg - nextOlderLog.weightKg else null
                    val logDate = LocalDate.ofEpochDay(log.dateEpochDay)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = logDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    if (logDate == LocalDate.now()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(ClassicRed.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Today", color = ClassicRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (log.note.isNotBlank()) {
                                    Text(
                                        text = log.note,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${String.format(java.util.Locale.US, "%.1f", log.weightKg)} kg",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )

                                    if (diff != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val isLoss = diff < 0
                                            val icon = if (isLoss) Icons.AutoMirrored.Filled.TrendingDown else if (diff > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingFlat
                                            val color = if (isLoss) Color(0xFF10B981) else if (diff > 0) ClassicRed else TextMuted
                                            val sign = if (diff > 0) "+" else ""

                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = color,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "$sign${String.format(java.util.Locale.US, "%.1f", diff)} kg",
                                                style = MaterialTheme.typography.labelSmall.copy(color = color, fontSize = 11.sp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.deleteWeightLog(log) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = TextMuted.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
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
}
