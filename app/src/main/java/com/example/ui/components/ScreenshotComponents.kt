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
import androidx.compose.material.icons.filled.CalendarToday
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
            .padding(start = 8.dp, top = 8.dp, end = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(40.dp).testTag("header_menu_button")
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open Navigation Menu",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { dropdownExpanded = true }
                        .padding(vertical = 1.dp)
                ) {
                    Text(
                        text = if (selectedDate == LocalDate.now()) "Today"
                        else selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Date",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
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
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        if (selectedDate != LocalDate.now()) {
            IconButton(
                onClick = { onDateSelect(LocalDate.now()) },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Jump to today",
                    tint = TextSecondary,
                    modifier = Modifier.size(19.dp)
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
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 8.dp, end = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(40.dp).testTag("header_menu_button")
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open Navigation Menu",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            if (!supportingText.isNullOrBlank()) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
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
    canGoPreviousWeek: Boolean = true,
    canGoNextWeek: Boolean = true,
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
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousWeek,
            enabled = canGoPreviousWeek,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous week",
                tint = if (canGoPreviousWeek) TextSecondary else TextMuted.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }

        for (i in 0 until 7) {
            val date = startOfWeek.plusDays(i.toLong())
            val isSelected = date == selectedDate
            val dayName = date.format(DateTimeFormatter.ofPattern("EEE"))
            val dayNum = date.dayOfMonth.toString()

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

            val dayTextColor = when {
                isSelected && trackable -> Color.White
                !trackable -> TextMuted.copy(alpha = 0.55f)
                tracked -> TextPrimary
                else -> TextSecondary
            }
            val weekdayColor = when {
                isSelected && trackable -> TextPrimary
                !trackable -> TextMuted.copy(alpha = 0.5f)
                tracked -> TextSecondary
                else -> TextMuted
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = trackable) { onSelectDate(date) }
                    .padding(vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = weekdayColor
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isSelected && trackable) ClassicRed else Color.Transparent)
                        .drawWithContent {
                            drawContent()
                            if (showProhibitionSign) {
                                val inset = 5.dp.toPx()
                                drawLine(
                                    start = Offset(inset, inset),
                                    end = Offset(size.width - inset, size.height - inset),
                                    color = TextMuted.copy(alpha = 0.55f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayNum,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        color = dayTextColor
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                // Goal markers: green for protein, blue for water, red when
                // neither goal was reached.
                val waterTracked = (summary?.totalWaterMl ?: 0) > 0
                val waterGoal = summary?.waterGoalMl ?: 0
                val waterHit = waterTracked && waterGoal > 0 && (summary?.totalWaterMl ?: 0) >= waterGoal
                val goalDotColors = when {
                    proteinHit && waterHit -> listOf(SuccessGreen, WaterCyan)
                    proteinHit -> listOf(SuccessGreen)
                    waterHit -> listOf(WaterCyan)
                    tracked || waterTracked -> listOf(ClassicRed)
                    else -> emptyList()
                }
                Row(
                    modifier = Modifier.height(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    goalDotColors.forEach { dotColor ->
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onNextWeek,
            enabled = canGoNextWeek,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next week",
                tint = if (canGoNextWeek) TextSecondary else TextMuted.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
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
    val consumed = summary.totalCalories.toInt()
    val target = summary.caloriesGoal.toInt()
    val remaining = target - consumed
    val isOver = remaining < 0
    val progress = summary.caloriesProgress

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("screenshot_calories_macros_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CALORIES",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = TextMuted,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Target ${target} kcal",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "$consumed",
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 7.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isOver) "Over target" else "Remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "${kotlin.math.abs(remaining)} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isOver) ClassicRed else TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ThinProgressBar(
            progress = progress,
            height = 7.dp,
            color = ClassicRed,
            trackColor = SurfaceBorder.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            MacroColumnItem(
                label = "Protein",
                consumed = summary.totalProtein,
                target = summary.proteinGoal,
                progress = summary.proteinProgress,
                accent = ClassicRed,
                modifier = Modifier.weight(1f)
            )
            MacroColumnItem(
                label = "Carbs",
                consumed = summary.totalCarbs,
                target = summary.carbsGoal,
                progress = summary.carbsProgress,
                accent = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            MacroColumnItem(
                label = "Fat",
                consumed = summary.totalFat,
                target = summary.fatGoal,
                progress = summary.fatProgress,
                accent = TextMuted,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MacroColumnItem(
    label: String,
    consumed: Double,
    target: Double,
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${consumed.toInt()}",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = "/${target.toInt()}g",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        ThinProgressBar(
            progress = progress,
            height = 4.dp,
            color = accent,
            trackColor = SurfaceBorder.copy(alpha = 0.45f)
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

    val timeFormatted = remember(logs.first().timestamp) {
        val instant = Instant.ofEpochMilli(logs.first().timestamp)
        val time = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        time.format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("screenshot_food_group_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = logs.first().mealType.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = TextMuted
                )
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = TextMuted
                )
            }
            Text(
                text = "${totalCal.toInt()} kcal",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary
            )
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Meal options",
                        tint = TextMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete all in meal", color = Color(0xFFEF4444)) },
                        onClick = {
                            logs.forEach { onDeleteLog(it) }
                            menuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TrackableDivider()

        logs.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditLog(item) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.foodNameSnapshot,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = buildString {
                            append(formatFoodQuantity(item.quantity))
                            append(" ")
                            append(item.servingUnit)
                            if (item.brandSnapshot.isNotBlank()) {
                                append(" · ")
                                append(item.brandSnapshot)
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    MacroSummaryLine(
                        protein = item.protein,
                        carbs = item.carbohydrates,
                        fat = item.fat
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${item.calories.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "kcal",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit ${item.foodNameSnapshot}",
                    tint = TextMuted.copy(alpha = 0.55f),
                    modifier = Modifier
                        .padding(start = 8.dp, top = 3.dp)
                        .size(14.dp)
                )
            }
            if (index < logs.lastIndex) {
                TrackableDivider(color = SurfaceBorder.copy(alpha = 0.45f))
            }
        }
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
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceCardHigh)
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

            // Plus / Send Icon. There is deliberately no loading spinner here:
            // progress is shown by the animated skeleton in the dashboard log.
            IconButton(
                onClick = {
                    if (text.isNotBlank() && !isLoading) {
                        onSendText(text)
                        text = ""
                    }
                },
                enabled = text.isNotBlank() && !isLoading,
                modifier = Modifier.size(36.dp).testTag("send_log_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Send/Add",
                    tint = if (text.isNotBlank() && !isLoading) ClassicRed else TextPrimary.copy(alpha = 0.45f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
