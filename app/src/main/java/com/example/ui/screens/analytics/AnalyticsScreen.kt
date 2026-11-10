package com.example.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.WeightLogEntity
import com.example.data.repository.DayTrendPoint
import com.example.ui.components.MetricBlock
import com.example.ui.components.SectionHeader
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.ClassicRedGlow
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter

// Helper to get short month name
private fun monthShort(m: Month): String = when (m) {
    Month.JANUARY -> "Jan"; Month.FEBRUARY -> "Feb"; Month.MARCH -> "Mar"
    Month.APRIL -> "Apr"; Month.MAY -> "May"; Month.JUNE -> "Jun"
    Month.JULY -> "Jul"; Month.AUGUST -> "Aug"; Month.SEPTEMBER -> "Sep"
    Month.OCTOBER -> "Oct"; Month.NOVEMBER -> "Nov"; Month.DECEMBER -> "Dec"
}

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDays by viewModel.selectedDays.collectAsStateWithLifecycle()
    val customDateRange by viewModel.customDateRange.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val weightLogs by viewModel.weightLogsAsc.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()

    var showCustomDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        containerColor = BackgroundDark,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time range: quiet text tabs, refresh at the end.
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnalyticsRangeTab(
                            label = "7 days",
                            selected = selectedDays == 7,
                            onClick = { viewModel.selectDaysRange(7) }
                        )
                        AnalyticsRangeTab(
                            label = "30 days",
                            selected = selectedDays == 30,
                            onClick = { viewModel.selectDaysRange(30) }
                        )
                        val customLabel = if (selectedDays == -1 && customDateRange != null) {
                            val fmt = DateTimeFormatter.ofPattern("MMM d")
                            "${customDateRange!!.first.format(fmt)} – ${customDateRange!!.second.format(fmt)}"
                        } else {
                            "Custom"
                        }
                        AnalyticsRangeTab(
                            label = customLabel,
                            selected = selectedDays == -1,
                            showCalendar = selectedDays != -1,
                            onClick = { showCustomDatePicker = true }
                        )
                    }

                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Summary stats: three aligned figures, no containers.
            item {
                val totalPeriod = state.trendPoints.size
                val loggedDays = state.totalLoggedDays
                val bestCal = state.trendPoints.maxOfOrNull { it.calories }?.toInt() ?: 0
                val avgPct = if (state.calorieGoal > 0)
                    (state.averageCalories / state.calorieGoal * 100).toInt() else 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    MetricBlock(
                        label = "Logged",
                        value = "$loggedDays / $totalPeriod",
                        supporting = "days",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBlock(
                        label = "Avg kcal",
                        value = "${state.averageCalories.toInt()}",
                        valueColor = ClassicRed,
                        supporting = "$avgPct% of goal",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBlock(
                        label = "Best day",
                        value = "$bestCal",
                        supporting = "kcal",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // =========================================================================
            // 1. WEIGHT PROGRESSION GRAPH (Handles skipped days accurately)
            // =========================================================================
            item {
                WeightProgressionChartCard(
                    weightLogs = weightLogs,
                    targetWeightKg = goals?.targetWeightKg ?: 70.0
                )
            }

            // =========================================================================
            // 2. DAILY CALORIE INTAKE BAR CHART
            // =========================================================================
            item {
                CalorieTrendChartCard(
                    trendPoints = state.trendPoints,
                    targetCalories = state.calorieGoal,
                    averageCalories = state.averageCalories
                )
            }

            // =========================================================================
            // 3. DAILY PROTEIN INTAKE LINE GRAPH
            // =========================================================================
            item {
                ProteinTrendChartCard(
                    trendPoints = state.trendPoints,
                    targetProtein = state.proteinGoal,
                    averageProtein = state.averageProtein
                )
            }

            // =========================================================================
            // 4. MACRONUTRIENT DISTRIBUTION — flat section
            // =========================================================================
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionHeader(
                        title = "Average daily macros",
                        supportingText = "Across the selected range."
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        MacroStatBox(
                            label = "Protein",
                            value = "${state.averageProtein.toInt()}g",
                            subValue = "Target ${goals?.proteinGoal?.toInt() ?: 125}g",
                            color = ClassicRed,
                            modifier = Modifier.weight(1f)
                        )
                        MacroStatBox(
                            label = "Carbs",
                            value = "${state.averageCarbs.toInt()}g",
                            subValue = "Target ${goals?.carbsGoal?.toInt() ?: 250}g",
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        MacroStatBox(
                            label = "Fat",
                            value = "${state.averageFat.toInt()}g",
                            subValue = "Target ${goals?.fatGoal?.toInt() ?: 56}g",
                            color = TextMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Custom Date Range Picker Dialog
    if (showCustomDatePicker) {
        CustomDateRangePickerDialog(
            initialStart = customDateRange?.first ?: LocalDate.now().minusDays(13),
            initialEnd = customDateRange?.second ?: LocalDate.now(),
            onConfirm = { start, end ->
                viewModel.selectCustomRange(start, end)
                showCustomDatePicker = false
            },
            onDismiss = { showCustomDatePicker = false }
        )
    }
}

// -----------------------------------------------------------------------------
// CUSTOM DATE RANGE PICKER DIALOG
// -----------------------------------------------------------------------------
@Composable
private fun CustomDateRangePickerDialog(
    initialStart: LocalDate,
    initialEnd: LocalDate,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var startYear by remember { mutableIntStateOf(initialStart.year) }
    var startMonth by remember { mutableIntStateOf(initialStart.monthValue) }
    var startDay by remember { mutableIntStateOf(initialStart.dayOfMonth) }
    var endYear by remember { mutableIntStateOf(initialEnd.year) }
    var endMonth by remember { mutableIntStateOf(initialEnd.monthValue) }
    var endDay by remember { mutableIntStateOf(initialEnd.dayOfMonth) }

    fun clampDay(day: Int, month: Int, year: Int): Int {
        val maxDay = java.time.YearMonth.of(year, month).lengthOfMonth()
        return day.coerceIn(1, maxDay)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "Select Date Range",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold, color = TextPrimary
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Start Date
                Text(
                    text = "START DATE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ClassicRed, fontWeight = FontWeight.Bold,
                        fontSize = 10.sp, letterSpacing = 0.8.sp
                    )
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Month
                    DateSpinner(
                        value = startMonth,
                        range = 1..12,
                        label = { m -> monthShort(Month.of(m)) },
                        onDecrease = { if (startMonth > 1) startMonth-- else { startMonth = 12; startYear-- }; startDay = clampDay(startDay, startMonth, startYear) },
                        onIncrease = { if (startMonth < 12) startMonth++ else { startMonth = 1; startYear++ }; startDay = clampDay(startDay, startMonth, startYear) },
                        modifier = Modifier.weight(1.4f)
                    )
                    // Day
                    DateSpinner(
                        value = startDay,
                        range = 1..java.time.YearMonth.of(startYear, startMonth).lengthOfMonth(),
                        label = { it.toString() },
                        onDecrease = { val max = java.time.YearMonth.of(startYear, startMonth).lengthOfMonth(); if (startDay > 1) startDay-- else startDay = max },
                        onIncrease = { val max = java.time.YearMonth.of(startYear, startMonth).lengthOfMonth(); if (startDay < max) startDay++ else startDay = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    // Year
                    DateSpinner(
                        value = startYear,
                        range = 2020..2099,
                        label = { it.toString() },
                        onDecrease = { if (startYear > 2020) startYear--; startDay = clampDay(startDay, startMonth, startYear) },
                        onIncrease = { startYear++; startDay = clampDay(startDay, startMonth, startYear) },
                        modifier = Modifier.weight(1.4f)
                    )
                }

                // End Date
                Text(
                    text = "END DATE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ClassicRed, fontWeight = FontWeight.Bold,
                        fontSize = 10.sp, letterSpacing = 0.8.sp
                    )
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DateSpinner(
                        value = endMonth,
                        range = 1..12,
                        label = { m -> monthShort(Month.of(m)) },
                        onDecrease = { if (endMonth > 1) endMonth-- else { endMonth = 12; endYear-- }; endDay = clampDay(endDay, endMonth, endYear) },
                        onIncrease = { if (endMonth < 12) endMonth++ else { endMonth = 1; endYear++ }; endDay = clampDay(endDay, endMonth, endYear) },
                        modifier = Modifier.weight(1.4f)
                    )
                    DateSpinner(
                        value = endDay,
                        range = 1..java.time.YearMonth.of(endYear, endMonth).lengthOfMonth(),
                        label = { it.toString() },
                        onDecrease = { val max = java.time.YearMonth.of(endYear, endMonth).lengthOfMonth(); if (endDay > 1) endDay-- else endDay = max },
                        onIncrease = { val max = java.time.YearMonth.of(endYear, endMonth).lengthOfMonth(); if (endDay < max) endDay++ else endDay = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    DateSpinner(
                        value = endYear,
                        range = 2020..2099,
                        label = { it.toString() },
                        onDecrease = { if (endYear > 2020) endYear--; endDay = clampDay(endDay, endMonth, endYear) },
                        onIncrease = { endYear++; endDay = clampDay(endDay, endMonth, endYear) },
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val start = LocalDate.of(startYear, startMonth, startDay)
                    val end = LocalDate.of(endYear, endMonth, endDay)
                    if (!end.isBefore(start)) {
                        onConfirm(start, end)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ClassicRed, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
private fun DateSpinner(
    value: Int,
    range: IntRange,
    label: (Int) -> String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF22252C))
                .clickable { onIncrease() }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("▲", color = TextMuted, fontSize = 10.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF16181E))
                .border(1.dp, ClassicRed.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label(value),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimary, fontWeight = FontWeight.Bold
                ),
                fontSize = 13.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF22252C))
                .clickable { onDecrease() }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("▼", color = TextMuted, fontSize = 10.sp)
        }
    }
}

// -----------------------------------------------------------------------------
// WEIGHT PROGRESSION CHART CARD (Continuous curve with skipped days support)
// -----------------------------------------------------------------------------
@Composable
fun WeightProgressionChartCard(
    weightLogs: List<WeightLogEntity>,
    targetWeightKg: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Weight progression",
                    supportingText = "Weigh-ins across the selected days.",
                    modifier = Modifier.weight(1f)
                )

                if (weightLogs.size >= 2) {
                    val start = weightLogs.first().weightKg
                    val current = weightLogs.last().weightKg
                    val diff = current - start
                    val isLoss = diff < 0
                    Text(
                        text = "${if (diff > 0) "+" else ""}${
                            String.format(java.util.Locale.US, "%.1f", diff)
                        } kg",
                        color = if (isLoss) SuccessGreen else ClassicRed,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (weightLogs.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF13151A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log at least 2 weigh-ins to render progression curve",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            } else {
                val minWeight = (weightLogs.minOf { it.weightKg } - 1.0).coerceAtLeast(20.0)
                val maxWeight = (weightLogs.maxOf { it.weightKg } + 1.0).coerceAtLeast(minWeight + 2.0)
                val weightRange = (maxWeight - minWeight).coerceAtLeast(1.0)

                val minEpoch = weightLogs.first().dateEpochDay
                val maxEpoch = weightLogs.last().dateEpochDay
                val epochRange = (maxEpoch - minEpoch).coerceAtLeast(1L).toFloat()

                val yAxisWidth = 40.dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    // Y-axis labels
                    Column(
                        modifier = Modifier
                            .width(yAxisWidth)
                            .height(220.dp)
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        val midWeight = (minWeight + maxWeight) / 2.0
                        listOf(maxWeight, midWeight, minWeight).forEach { w ->
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", w),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted, fontSize = 9.sp
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF12141A))
                            .padding(12.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height
                            val paddingBottom = 20.dp.toPx()
                            val paddingTop = 16.dp.toPx()
                            val chartHeight = height - paddingBottom - paddingTop

                            // Draw target weight horizontal dashed line
                            if (targetWeightKg in minWeight..maxWeight) {
                                val targetY = paddingTop + chartHeight - ((targetWeightKg - minWeight) / weightRange).toFloat() * chartHeight
                                drawLine(
                                    color = Color(0xFF525968),
                                    start = Offset(0f, targetY),
                                    end = Offset(width, targetY),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            }

                            val points = weightLogs.map { log ->
                                val xFraction = if (epochRange > 0) (log.dateEpochDay - minEpoch).toFloat() / epochRange else 0.5f
                                val x = xFraction * (width - 40.dp.toPx()) + 20.dp.toPx()
                                val yFraction = ((log.weightKg - minWeight) / weightRange).toFloat().coerceIn(0f, 1f)
                                val y = paddingTop + chartHeight - (yFraction * chartHeight)
                                Pair(Offset(x, y), log.weightKg)
                            }

                            // Gradient fill under curve
                            val fillPath = Path().apply {
                                moveTo(points.first().first.x, paddingTop + chartHeight)
                                lineTo(points.first().first.x, points.first().first.y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].first.x, points[i].first.y)
                                }
                                lineTo(points.last().first.x, paddingTop + chartHeight)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(ClassicRed.copy(alpha = 0.35f), Color.Transparent),
                                    startY = paddingTop,
                                    endY = paddingTop + chartHeight
                                )
                            )

                            // Main curve line
                            val strokePath = Path().apply {
                                moveTo(points.first().first.x, points.first().first.y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].first.x, points[i].first.y)
                                }
                            }
                            drawPath(
                                path = strokePath,
                                color = ClassicRed,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Dots + floating weight labels
                            points.forEach { (pt, wKg) ->
                                drawCircle(color = ClassicRed, radius = 6.dp.toPx(), center = pt)
                                drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = pt)
                                // Weight label above dot
                                drawIntoCanvas { canvas ->
                                    val paint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 9.dp.toPx()
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isAntiAlias = true
                                    }
                                    canvas.nativeCanvas.drawText(
                                        String.format(java.util.Locale.US, "%.1f", wKg),
                                        pt.x,
                                        pt.y - 10.dp.toPx(),
                                        paint
                                    )
                                }
                            }
                        }
                    }
                }

                // Date strip below graph
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val firstDate = LocalDate.ofEpochDay(weightLogs.first().dateEpochDay).format(DateTimeFormatter.ofPattern("MMM d"))
                    val lastDate = LocalDate.ofEpochDay(weightLogs.last().dateEpochDay).format(DateTimeFormatter.ofPattern("MMM d"))
                    Text(
                        text = "Start: $firstDate (${String.format(java.util.Locale.US, "%.1f", weightLogs.first().weightKg)} kg)",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                    )
                    Text(
                        text = "Latest: $lastDate (${String.format(java.util.Locale.US, "%.1f", weightLogs.last().weightKg)} kg)",
                        style = MaterialTheme.typography.bodySmall.copy(color = ClassicRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// CALORIE TREND CHART CARD
// -----------------------------------------------------------------------------
@Composable
fun CalorieTrendChartCard(
    trendPoints: List<DayTrendPoint>,
    targetCalories: Double,
    averageCalories: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Daily calories",
                    supportingText = "Average ${averageCalories.toInt()} kcal per day.",
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Goal ${targetCalories.toInt()}",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // Bar Chart with Y-axis labels
            val maxCal = ((trendPoints.maxOfOrNull { it.calories } ?: targetCalories) * 1.15).coerceAtLeast(500.0)
            val midCal = maxCal / 2.0
            val yAxisWidth = 40.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Y-axis
                Column(
                    modifier = Modifier
                        .width(yAxisWidth)
                        .fillMaxSize()
                        .padding(end = 4.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${maxCal.toInt()}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
                    )
                    Text(
                        text = "${midCal.toInt()}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
                    )
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
                    )
                }

                // Chart area
                val n = trendPoints.size
                val showEvery = when {
                    n <= 7 -> 1
                    n <= 14 -> 2
                    else -> (n / 7).coerceAtLeast(1)
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF12141A))
                        .padding(horizontal = 4.dp, vertical = 0.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    trendPoints.forEachIndexed { index, point ->
                        val barHeightFraction = (point.calories / maxCal).toFloat().coerceIn(0.04f, 1f)
                        val isLogged = point.calories > 0
                        val showLabel = index % showEvery == 0

                        // Parse displayLabel as date for "MMM d" format
                        val dateLabel = try {
                            val parts = point.displayLabel.split("/")
                            if (parts.size == 2) {
                                val month = parts[0].trim().toIntOrNull() ?: 0
                                val day = parts[1].trim().toIntOrNull() ?: 0
                                if (month > 0 && day > 0) "${monthShort(Month.of(month))} $day" else point.displayLabel
                            } else point.displayLabel
                        } catch (e: Exception) {
                            point.displayLabel
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Value label above bar (shown when logged)
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (isLogged) {
                                        Text(
                                            text = "${point.calories.toInt()}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextPrimary.copy(alpha = 0.8f),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(14.dp)
                                            .fillMaxSize(barHeightFraction)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .then(
                                                if (isLogged) {
                                                    Modifier.background(Brush.verticalGradient(listOf(ClassicRedGlow, ClassicRed)))
                                                } else {
                                                    Modifier.background(Color(0xFF262B35))
                                                }
                                            )
                                    )
                                }
                            }
                            // Goal line drawn via Canvas overlay — just show date label
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (showLabel) dateLabel else "",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isLogged) TextPrimary else TextMuted,
                                    fontSize = 8.sp,
                                    fontWeight = if (isLogged) FontWeight.Bold else FontWeight.Normal
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Dashed goal line indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(1.5.dp)
                        .background(Color(0xFF525968))
                )
                Text(
                    text = "Goal line: ${targetCalories.toInt()} kcal",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// PROTEIN TREND CHART CARD (Line graph showing daily protein intake)
// -----------------------------------------------------------------------------
@Composable
fun ProteinTrendChartCard(
    trendPoints: List<DayTrendPoint>,
    targetProtein: Double,
    averageProtein: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Daily protein",
                    supportingText = "Average ${averageProtein.toInt()}g per day.",
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Goal ${targetProtein.toInt()}g",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // Line Chart with Y-axis labels
            val maxProt = ((trendPoints.maxOfOrNull { it.protein } ?: targetProtein) * 1.25).coerceAtLeast(50.0)
            val midProt = maxProt / 2.0
            val yAxisWidth = 36.dp
            val n = trendPoints.size
            val showEvery = when {
                n <= 7 -> 1
                n <= 14 -> 2
                else -> (n / 7).coerceAtLeast(1)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Y-axis
                Column(
                    modifier = Modifier
                        .width(yAxisWidth)
                        .fillMaxSize()
                        .padding(end = 4.dp, bottom = 30.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${maxProt.toInt()}g",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
                    )
                    Text(
                        text = "${midProt.toInt()}g",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
                    )
                    Text(
                        text = "0g",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF12141A))
                        .padding(12.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val paddingBottom = 30.dp.toPx()
                        val paddingTop = 16.dp.toPx()
                        val chartHeight = height - paddingBottom - paddingTop

                        // Horizontal goal line
                        val goalY = paddingTop + chartHeight - ((targetProtein / maxProt).toFloat().coerceIn(0f, 1f) * chartHeight)
                        drawLine(
                            color = Color(0xFF525968),
                            start = Offset(0f, goalY),
                            end = Offset(width, goalY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        if (trendPoints.isEmpty()) return@Canvas

                        val stepX = width / trendPoints.size.toFloat()
                        val points = trendPoints.withIndex().map { (index, point) ->
                            val x = index * stepX + stepX / 2f
                            val yFraction = (point.protein / maxProt).toFloat().coerceIn(0f, 1f)
                            val y = paddingTop + chartHeight - (yFraction * chartHeight)
                            Pair(Offset(x, y), point)
                        }

                        // Gradient fill under curve
                        if (points.size > 1) {
                            val fillPath = Path().apply {
                                moveTo(points.first().first.x, paddingTop + chartHeight)
                                lineTo(points.first().first.x, points.first().first.y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].first.x, points[i].first.y)
                                }
                                lineTo(points.last().first.x, paddingTop + chartHeight)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(ClassicRed.copy(alpha = 0.35f), Color.Transparent),
                                    startY = paddingTop,
                                    endY = paddingTop + chartHeight
                                )
                            )

                            val strokePath = Path().apply {
                                moveTo(points.first().first.x, points.first().first.y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].first.x, points[i].first.y)
                                }
                            }
                            drawPath(
                                path = strokePath,
                                color = ClassicRed,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Dots + value labels
                        points.forEachIndexed { index, (pt, point) ->
                            val isLogged = point.protein > 0
                            drawCircle(
                                color = if (isLogged) ClassicRed else TextMuted.copy(alpha = 0.5f),
                                radius = 5.dp.toPx(),
                                center = pt
                            )
                            if (isLogged) {
                                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = pt)
                                // Value label above dot
                                drawIntoCanvas { canvas ->
                                    val paint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 8.dp.toPx()
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isAntiAlias = true
                                    }
                                    canvas.nativeCanvas.drawText(
                                        "${point.protein.toInt()}g",
                                        pt.x,
                                        pt.y - 8.dp.toPx(),
                                        paint
                                    )
                                }
                            }
                        }
                    }

                    // Date strip at bottom of chart box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .align(Alignment.BottomCenter),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Top
                        ) {
                            trendPoints.forEachIndexed { index, point ->
                                val showLabel = index % showEvery == 0
                                val dateLabel = try {
                                    val parts = point.displayLabel.split("/")
                                    if (parts.size == 2) {
                                        val month = parts[0].trim().toIntOrNull() ?: 0
                                        val day = parts[1].trim().toIntOrNull() ?: 0
                                        if (month > 0 && day > 0) "${monthShort(Month.of(month))} $day" else point.displayLabel
                                    } else point.displayLabel
                                } catch (e: Exception) {
                                    point.displayLabel
                                }
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (showLabel) {
                                        Text(
                                            text = dateLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (point.protein > 0) TextPrimary else TextMuted,
                                                fontSize = 8.sp
                                            )
                                        )
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
private fun MacroStatBox(
    label: String,
    value: String,
    subValue: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Text(
            text = subValue,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
private fun AnalyticsRangeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    showCalendar: Boolean = false
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showCalendar) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) TextPrimary else TextMuted
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (selected) ClassicRed else Color.Transparent)
        )
    }
}
