package com.example.ui.screens.history

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FoodLogCard
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CarbsOrange
import com.example.ui.theme.FatPink
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.ProteinPurple
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WaterCyan
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val summary by viewModel.dailySummary.collectAsStateWithLifecycle()

    val dateFormatted = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
    }

    // Group logs by meal type
    val groupedLogs = remember(summary.foodLogs) {
        summary.foodLogs.groupBy { it.mealType }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen"),
        containerColor = BackgroundDark
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Text(
                    text = "HISTORY LOG",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen,
                        letterSpacing = 1.2.sp
                    )
                )
                Text(
                    text = "Detailed Day Logs",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            // Date Selector Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousDay() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day", tint = TextPrimary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        if (selectedDate == LocalDate.now()) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall.copy(color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.nextDay() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day", tint = TextPrimary)
                    }
                }
            }

            // Day Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TOTAL CALORIES", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                                Text(
                                    text = "${summary.totalCalories.toInt()} kcal",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("WATER", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                                Text(
                                    text = "${summary.totalWaterMl} ml",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = WaterCyan)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Protein: ${summary.totalProtein}g", color = ProteinPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Carbs: ${summary.totalCarbs}g", color = CarbsOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Fat: ${summary.totalFat}g", color = FatPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Grouped Meal Entries
            if (summary.foodLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No entries recorded for this date", color = TextMuted)
                        }
                    }
                }
            } else {
                listOf("Breakfast", "Lunch", "Dinner", "Snack").forEach { mealCategory ->
                    val mealItems = groupedLogs[mealCategory] ?: emptyList()
                    if (mealItems.isNotEmpty()) {
                        item {
                            val mealCalories = mealItems.sumOf { it.calories }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mealCategory.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Text(
                                    text = "${mealCalories.toInt()} kcal",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = PrimaryGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        items(mealItems, key = { it.id }) { log ->
                            FoodLogCard(
                                log = log,
                                onEdit = { /* can edit if needed */ },
                                onDelete = { viewModel.deleteLog(log) }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
