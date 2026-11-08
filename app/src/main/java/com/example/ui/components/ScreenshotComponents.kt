package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.WaterCyan
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.FoodLogEntity
import com.example.data.repository.DailyNutritionSummary
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ChipBackground
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.MacroPink
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.ProgressCyanBlue
import com.example.ui.theme.SelectedGreenBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCardHigh
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfacePill
import com.example.ui.theme.SurfacePillSelected
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningCardBg
import com.example.ui.theme.WarningCardBorder
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

// -------------------------------------------------------------------------
// 1. TOP APP BAR MATCHING SCREENSHOT
// -------------------------------------------------------------------------
@Composable
fun ScreenshotTopHeader(
    selectedDate: LocalDate,
    onMenuClick: () -> Unit = {},
    onDateSelect: (LocalDate) -> Unit = {}
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Menu Hamburger
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(36.dp).testTag("header_menu_button")
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open Navigation Menu",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Month label (left side of Today dropdown, gray small font)
        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )

        // Center "Today ▾" dropdown
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { dropdownExpanded = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val label = if (selectedDate == LocalDate.now()) "Today" else selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Date",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.background(SurfaceDark)
            ) {
                DropdownMenuItem(
                    text = { Text("Today", color = TextPrimary) },
                    onClick = {
                        onDateSelect(LocalDate.now())
                        dropdownExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Yesterday", color = TextPrimary) },
                    onClick = {
                        onDateSelect(LocalDate.now().minusDays(1))
                        dropdownExpanded = false
                    }
                )
            }
        }

    }
}

// -------------------------------------------------------------------------
// 1b. PAGE HEADER (Hamburger + Title) – used on every non-dashboard page
// -------------------------------------------------------------------------
@Composable
fun PageHeader(
    title: String,
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(36.dp).testTag("header_menu_button")
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open Navigation Menu",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 18.sp
            )
        )
    }
}

// -------------------------------------------------------------------------
// 2. WEEK CALENDAR STRIP (Sun 1 ... Sat 7)
// -------------------------------------------------------------------------
@Composable
fun ScreenshotWeekCalendarStrip(
    selectedDate: LocalDate,
    startDate: LocalDate?,
    useStartDate: Boolean = false,
    blockBeforeStartDate: Boolean = true,
    blockAfterToday: Boolean = true,
    weekSummaries: List<DailyNutritionSummary> = emptyList(),
    onSelectDate: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {}
) {
    val today = LocalDate.now()
    val startOfWeek = remember(selectedDate) {
        selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    }
    val summariesByDate = weekSummaries.associateBy { it.dateString }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous week
        IconButton(
            onClick = onPreviousWeek,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous week",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        // Days
        for (i in 0 until 7) {
            val date = startOfWeek.plusDays(i.toLong())
            val isSelected = date == selectedDate
            val dayName = date.format(DateTimeFormatter.ofPattern("EEE"))
            val dayNum = date.dayOfMonth.toString()

            // Tracking rules based on settings:
            // - before start date: always grayed + prohibition sign (visual), clickable depends on blockBeforeStartDate
            // - after today: always grayed + prohibition sign (visual), clickable depends on blockAfterToday
            // - start date .. today: trackable, color coded by logging status
            val isBeforeStartVisually = useStartDate && startDate != null && date < startDate
            val isFuture = date > today
            val isBeforeStartBlocked = isBeforeStartVisually && blockBeforeStartDate
            val isFutureBlocked = isFuture && blockAfterToday
            val trackable = !isBeforeStartBlocked && !isFutureBlocked
            val showProhibitionSign = isBeforeStartVisually || isFuture

            val summary = summariesByDate[date.toString()]
            val tracked = summary?.foodLogs?.isNotEmpty() == true
            val proteinGoal = summary?.proteinGoal ?: 0.0
            val proteinHit = tracked && proteinGoal > 0.0 && summary.totalProtein >= proteinGoal

            // Background: selected + trackable = crimson. Unselectable days never get the
            // selected red color, even when they happen to be the currently selected date.
            val pillColor = if (isSelected && trackable) ClassicRed else SurfacePill
            // Text color: selected + trackable = white, untrackable = gray,
            // tracked = white, protein goal hit = green
            val dayTextColor = when {
                isSelected && trackable -> Color.White
                !trackable -> TextMuted
                tracked && proteinHit -> SuccessGreen
                tracked -> Color.White
                else -> TextMuted
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(pillColor)
                    .clickable(enabled = trackable) { onSelectDate(date) }
                    .padding(vertical = 8.dp)
                    .drawWithContent {
                        drawContent() // draws background + day text
                        if (showProhibitionSign) {
                            val size = this.size
                            val strokePx = 1f.dp.toPx() // thin strikethrough line
                            val insetPx = 1f.dp.toPx()
                            // Thin diagonal line from the date's top-left corner to bottom-right corner.
                            // Only prohibited days draw this line; selectable days stay clean.
                            drawLine(
                                start = Offset(insetPx, insetPx),
                                end = Offset(size.width - insetPx, size.height - insetPx),
                                color = Color(0xFF9CA3AF),
                                strokeWidth = strokePx
                            )
                        }
                    }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = if (isSelected && trackable) Color.White else dayTextColor,
                            fontWeight = if (isSelected && trackable) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dayNum,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = dayTextColor,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }

        // Next week
        IconButton(
            onClick = onNextWeek,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next week",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// -------------------------------------------------------------------------
// 3. SET CALORIE GOAL WARNING BANNER
// -------------------------------------------------------------------------
@Composable
fun ScreenshotCalorieGoalWarningBanner(
    goals: com.example.data.database.NutritionGoalEntity,
    onTap: () -> Unit,
    onDismiss: () -> Unit
) {
    val missingFields = mutableListOf<String>()
    if (goals.name.isBlank()) missingFields.add("Name")
    if (goals.sex.isBlank()) missingFields.add("Sex")
    if (goals.age == null) missingFields.add("Age")
    if (goals.currentWeightKg == null) missingFields.add("Weight")
    if (goals.heightCm == null) missingFields.add("Height")
    if (goals.activityLevel.isBlank()) missingFields.add("Activity Level")
    if (goals.caloriesGoal == 2200.0 && goals.proteinGoal == 140.0 &&
        goals.carbsGoal == 220.0 && goals.fatGoal == 65.0) {
        missingFields.add("Daily Nutritional Targets")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onTap() }
            .testTag("set_calorie_goal_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarningCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarningCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = WarningAmber,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (missingFields.isNotEmpty()) "Complete Your Profile" else "Set Your Calorie Goal",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (missingFields.isNotEmpty()) {
                        "Please complete: ${missingFields.joinToString(", ")}"
                    } else {
                        "You are using the default calorie target\nTap to update your goal"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFD1D5DB),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                )
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// 4. UNIFIED CALORIES & MACROS CARD MATCHING SCREENSHOT
// -------------------------------------------------------------------------
@Composable
fun ScreenshotCaloriesAndMacrosCard(
    summary: DailyNutritionSummary,
    exerciseCalories: Int = 0,
    modifier: Modifier = Modifier
) {
    val calorieProgress = summary.caloriesProgress
    val animatedCalProgress by animateFloatAsState(
        targetValue = calorieProgress,
        animationSpec = tween(600),
        label = "cal_prog"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("screenshot_calories_macros_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header: Flame + "Calories"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Calories",
                    tint = FlameOrange,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Calories",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Calorie Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF26303E))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedCalProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(ProgressCyanBlue)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Column Stats: Food | Exercise | Remaining
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Food", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${summary.totalCalories.toInt()}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }

                Column {
                    Text("Exercise", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$exerciseCalories",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }

                Column {
                    Text("Remaining", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${summary.caloriesRemaining.toInt()}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Macros Header: Donut/Ring + "Macros"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DonutLarge,
                    contentDescription = "Macros",
                    tint = MacroPink,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Macros",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Macro Trackers side by side: Carbs (g) | Protein (g) | Fat (g)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MacroColumnItem(
                    label = "Carbs (g)",
                    consumed = summary.totalCarbs,
                    target = summary.carbsGoal,
                    modifier = Modifier.weight(1f)
                )
                MacroColumnItem(
                    label = "Protein (g)",
                    consumed = summary.totalProtein,
                    target = summary.proteinGoal,
                    modifier = Modifier.weight(1f)
                )
                MacroColumnItem(
                    label = "Fat (g)",
                    consumed = summary.totalFat,
                    target = summary.fatGoal,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MacroColumnItem(
    label: String,
    consumed: Double,
    target: Double,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) (consumed / target).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(500), label = "m_prog")

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF26303E))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(ProgressCyanBlue)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${consumed.toInt()} / ${target.toInt()}",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                fontSize = 12.sp
            )
        )
    }
}

// -------------------------------------------------------------------------
// 5. WATER CARD MATCHING SCREENSHOT ("Water: 0L", minus, "0 Cups", plus)
// -------------------------------------------------------------------------
@Composable
fun ScreenshotWaterCard(
    waterConsumedMl: Int,
    waterGoalMl: Int = 3000,
    onAddCup: () -> Unit,
    onRemoveCup: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1 cup ~ 250ml
    val cupsConsumed = waterConsumedMl / 250
    val totalCupsGoal = (waterGoalMl / 250).coerceAtLeast(1)
    val cupsRemaining = (totalCupsGoal - cupsConsumed).coerceAtLeast(0)
    val litersFormatted = String.format(java.util.Locale.US, "%.1fL", waterConsumedMl / 1000.0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("screenshot_water_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Water: $litersFormatted",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minus button
                IconButton(
                    onClick = onRemoveCup,
                    enabled = waterConsumedMl > 0,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Minus cup",
                        tint = if (waterConsumedMl > 0) TextPrimary else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center Cups Counter
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$cupsConsumed Cups",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 20.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$cupsRemaining Cups Remaining",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }

                // Plus button
                IconButton(
                    onClick = onAddCup,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add cup",
                        tint = TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 6. FOOD LOG CARD MATCHING SCREENSHOT
// -------------------------------------------------------------------------
@Composable
fun ScreenshotFoodLogGroupCard(
    logs: List<FoodLogEntity>,
    goalCalories: Double,
    goalCarbs: Double,
    goalProtein: Double,
    goalFat: Double,
    onEditLog: (FoodLogEntity) -> Unit,
    onDeleteLog: (FoodLogEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (logs.isEmpty()) return

    val totalCal = logs.sumOf { it.calories }
    val totalC = logs.sumOf { it.carbohydrates }
    val totalP = logs.sumOf { it.protein }
    val totalF = logs.sumOf { it.fat }

    val calPct = if (goalCalories > 0) ((totalCal / goalCalories) * 100).toInt() else 0
    val cPct = if (goalCarbs > 0) ((totalC / goalCarbs) * 100).toInt() else 0
    val pPct = if (goalProtein > 0) ((totalP / goalProtein) * 100).toInt() else 0
    val fPct = if (goalFat > 0) ((totalF / goalFat) * 100).toInt() else 0

    val timeFormatted = remember(logs.first().timestamp) {
        val instant = Instant.ofEpochMilli(logs.first().timestamp)
        val time = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        time.format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("screenshot_food_group_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Optional summary sentence
            val sentence = logs.joinToString(separator = ", ") { "${it.foodNameSnapshot} (${if (it.quantity % 1.0 == 0.0) it.quantity.toInt() else it.quantity}${it.servingUnit})" }
            Text(
                text = sentence,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-food items
            logs.forEach { item ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()}" else "${item.quantity}"
                    Text(
                        text = "${item.foodNameSnapshot} ($qtyStr ${item.servingUnit})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Chip badges in row: Calories: 330 | Carbs: 14g | Protein: 43g | Fat: 12g
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScreenshotPillBadge("Calories: ${item.calories.toInt()}")
                        ScreenshotPillBadge("Carbs: ${item.carbohydrates.toInt()}g")
                        ScreenshotPillBadge("Protein: ${item.protein.toInt()}g")
                        ScreenshotPillBadge("Fat: ${item.fat.toInt()}g")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Total row with mini progress bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScreenshotMacroTotalCol("Calories", "${totalCal.toInt()}", calPct, Modifier.weight(1f))
                ScreenshotMacroTotalCol("Carbs", "${totalC.toInt()}g", cPct, Modifier.weight(1f))
                ScreenshotMacroTotalCol("Protein", "${totalP.toInt()}g", pPct, Modifier.weight(1f))
                ScreenshotMacroTotalCol("Fat", "${totalF.toInt()}g", fPct, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Timestamp + Edit + Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onEditLog(logs.first()) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete All in Meal", color = Color(0xFFEF4444)) },
                                onClick = {
                                    logs.forEach { onDeleteLog(it) }
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenshotPillBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ChipBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun ScreenshotMacroTotalCol(
    label: String,
    value: String,
    pct: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF26303E))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((pct / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(ProgressCyanBlue)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text("$pct%", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp))
    }
}

// -------------------------------------------------------------------------
// 7. BOTTOM CAPSULE INPUT BAR MATCHING SCREENSHOT
// -------------------------------------------------------------------------
@Composable
fun ScreenshotAiInputBar(
    isLoading: Boolean,
    onSendText: (String) -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceCardHigh)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(28.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        text = "What did you eat or exercise?",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 15.sp
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("screenshot_ai_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (text.isNotBlank() && !isLoading) {
                        onSendText(text)
                        text = ""
                    }
                })
            )

            // Camera Icon
            IconButton(
                onClick = onCameraClick,
                modifier = Modifier.size(36.dp).testTag("camera_scan_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Camera Scan",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Plus / Send Icon
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = PrimaryGreen,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSendText(text)
                            text = ""
                        }
                    },
                    modifier = Modifier.size(36.dp).testTag("send_log_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Send/Add",
                        tint = if (text.isNotBlank()) PrimaryGreen else TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
