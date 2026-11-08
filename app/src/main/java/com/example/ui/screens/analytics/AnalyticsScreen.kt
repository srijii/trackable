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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.WeightLogEntity
import com.example.data.repository.DayTrendPoint
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.ClassicRedGlow
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDays by viewModel.selectedDays.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val weightLogs by viewModel.weightLogsAsc.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        containerColor = BackgroundDark
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header + Refresh
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PERFORMANCE & PROGRESS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ClassicRed,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Text(
                            text = "Analytics & Reports",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                            .border(1.dp, SurfaceBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Time Range Selector Tabs (7d / 14d / 30d)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF16181E))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(7 to "Last 7 Days", 14 to "Last 14 Days", 30 to "Last 30 Days").forEach { (days, label) ->
                        val isSelected = selectedDays == days
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ClassicRed else Color.Transparent)
                                .clickable { viewModel.selectDaysRange(days) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
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
            // 4. MACRONUTRIENT DISTRIBUTION CARD
            // =========================================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Average Daily Macros",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Protein (Red)
                            MacroStatBox(
                                label = "PROTEIN",
                                value = "${state.averageProtein.toInt()}g",
                                subValue = "Target: ${goals?.proteinGoal?.toInt() ?: 125}g",
                                color = ClassicRed,
                                modifier = Modifier.weight(1f)
                            )

                            // Carbs (White / Light Silver)
                            MacroStatBox(
                                label = "CARBS",
                                value = "${state.averageCarbs.toInt()}g",
                                subValue = "Target: ${goals?.carbsGoal?.toInt() ?: 250}g",
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )

                            // Fat (Slate Gray)
                            MacroStatBox(
                                label = "FAT",
                                value = "${state.averageFat.toInt()}g",
                                subValue = "Target: ${goals?.fatGoal?.toInt() ?: 56}g",
                                color = TextMuted,
                                modifier = Modifier.weight(1f)
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
}

// -----------------------------------------------------------------------------
// WEIGHT PROGRESSION CHART CARD (Continuous curve with skipped days support)
// -----------------------------------------------------------------------------
@Composable
fun WeightProgressionChartCard(
    weightLogs: List<WeightLogEntity>,
    targetWeightKg: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weight_progression_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ClassicRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = ClassicRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Weight Progression",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Tracks weigh-ins across dates (handles skipped days)",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                        )
                    }
                }

                if (weightLogs.size >= 2) {
                    val start = weightLogs.first().weightKg
                    val current = weightLogs.last().weightKg
                    val diff = current - start
                    val isLoss = diff < 0
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF13151A))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${if (diff > 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f", diff)} kg",
                            color = if (isLoss) Color(0xFF10B981) else ClassicRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
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
                // Interactive Canvas Line Chart
                val minWeight = (weightLogs.minOf { it.weightKg } - 1.0).coerceAtLeast(20.0)
                val maxWeight = (weightLogs.maxOf { it.weightKg } + 1.0).coerceAtLeast(minWeight + 2.0)
                val weightRange = (maxWeight - minWeight).coerceAtLeast(1.0)

                val minEpoch = weightLogs.first().dateEpochDay
                val maxEpoch = weightLogs.last().dateEpochDay
                val epochRange = (maxEpoch - minEpoch).coerceAtLeast(1L).toFloat()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF12141A))
                        .padding(12.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val paddingBottom = 20.dp.toPx()
                        val chartHeight = height - paddingBottom

                        // 1. Draw Target weight horizontal line
                        if (targetWeightKg in minWeight..maxWeight) {
                            val targetY = chartHeight - ((targetWeightKg - minWeight) / weightRange).toFloat() * chartHeight
                            drawLine(
                                color = Color(0xFF525968),
                                start = Offset(0f, targetY),
                                end = Offset(width, targetY),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        // Calculate points (X mapped proportionally to time between minEpoch and maxEpoch)
                        val points = weightLogs.map { log ->
                            val xFraction = if (epochRange > 0) (log.dateEpochDay - minEpoch).toFloat() / epochRange else 0.5f
                            val x = xFraction * (width - 40.dp.toPx()) + 20.dp.toPx()
                            val yFraction = ((log.weightKg - minWeight) / weightRange).toFloat().coerceIn(0f, 1f)
                            val y = chartHeight - (yFraction * chartHeight)
                            Offset(x, y)
                        }

                        // Gradient fill under the curve
                        val fillPath = Path().apply {
                            moveTo(points.first().x, chartHeight)
                            lineTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                            lineTo(points.last().x, chartHeight)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(ClassicRed.copy(alpha = 0.35f), Color.Transparent),
                                startY = 0f,
                                endY = chartHeight
                            )
                        )

                        // Main curve line
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }

                        drawPath(
                            path = strokePath,
                            color = ClassicRed,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Draw White dots with red border at each logged weigh-in
                        points.forEach { pt ->
                            // Outer red glow
                            drawCircle(
                                color = ClassicRed,
                                radius = 6.dp.toPx(),
                                center = pt
                            )
                            // Inner white core
                            drawCircle(
                                color = Color.White,
                                radius = 3.5.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }

                // Date and value strip below graph
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ClassicRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = ClassicRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Calorie Intake",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Average: ${averageCalories.toInt()} kcal / day",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF13151A))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Goal: ${targetCalories.toInt()}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bar Chart
            val maxCal = ((trendPoints.maxOfOrNull { it.calories } ?: targetCalories) * 1.15).coerceAtLeast(500.0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF12141A))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                trendPoints.forEach { point ->
                    val barHeightFraction = (point.calories / maxCal).toFloat().coerceIn(0.04f, 1f)
                    val isLogged = point.calories > 0

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
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

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = point.displayLabel.substringAfter("/"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isLogged) TextPrimary else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isLogged) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ClassicRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = ClassicRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Protein Intake",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Average: ${averageProtein.toInt()}g / day",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF13151A))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Goal: ${targetProtein.toInt()}g",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Line Chart
            val maxProt = ((trendPoints.maxOfOrNull { it.protein } ?: targetProtein) * 1.25).coerceAtLeast(50.0)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF12141A))
                    .padding(12.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val paddingBottom = 30.dp.toPx()
                    val chartHeight = height - paddingBottom

                    // Horizontal goal line
                    val goalY = chartHeight - ((targetProtein / maxProt).toFloat().coerceIn(0f, 1f) * chartHeight)
                    drawLine(
                        color = Color(0xFF525968),
                        start = Offset(0f, goalY),
                        end = Offset(width, goalY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Calculate points
                    val points = trendPoints.withIndex().map { (index, point) ->
                        val x = index * (width / trendPoints.size).toFloat()
                        val yFraction = (point.protein / maxProt).toFloat().coerceIn(0f, 1f)
                        val y = chartHeight - (yFraction * chartHeight)
                        Offset(x, y)
                    }

                    // Gradient fill under the curve
                    if (points.size > 1) {
                        val fillPath = Path().apply {
                            moveTo(points.first().x, chartHeight)
                            lineTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                            lineTo(points.last().x, chartHeight)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(ClassicRed.copy(alpha = 0.35f), Color.Transparent),
                                startY = 0f,
                                endY = chartHeight
                            )
                        )

                        // Main curve line
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = strokePath,
                            color = ClassicRed,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Dots at each point
                        points.forEachIndexed { index, pt ->
                            val isLogged = trendPoints[index].protein > 0
                            drawCircle(
                                color = if (isLogged) ClassicRed else TextMuted.copy(alpha = 0.5f),
                                radius = 5.dp.toPx(),
                                center = pt
                            )
                            if (isLogged) {
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.5.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    }
                }

                // Goal label text below canvas
                Box(
                    modifier = Modifier.fillMaxWidth().height(20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Goal ${targetProtein.toInt()}g",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            // Date strip below graph
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                trendPoints.forEachIndexed { index, point ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = point.displayLabel.substringAfter("/"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (point.protein > 0) TextPrimary else TextMuted,
                                fontSize = 10.sp
                            )
                        )
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF13151A))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.Bold, fontSize = 10.sp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
            Text(text = subValue, style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp))
        }
    }
}
