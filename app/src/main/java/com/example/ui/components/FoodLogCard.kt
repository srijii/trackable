package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.FoodLogEntity
import com.example.ui.theme.CarbsOrange
import com.example.ui.theme.FatPink
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.ProteinPurple
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun FoodLogCard(
    log: FoodLogEntity,
    onEdit: (FoodLogEntity) -> Unit,
    onDelete: (FoodLogEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatted = rememberFormattedTime(log.timestamp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("food_log_card_${log.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.foodNameSnapshot,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        val qtyDisplay = if (log.quantity % 1.0 == 0.0) "${log.quantity.toInt()}" else "${log.quantity}"
                        Text(
                            text = "$qtyDisplay ${log.servingUnit}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        if (log.brandSnapshot.isNotBlank()) {
                            Text(
                                text = " • ${log.brandSnapshot}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted
                                )
                            )
                        }
                        Text(
                            text = " • $timeFormatted",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onEdit(log) },
                        modifier = Modifier.size(36.dp).testTag("edit_log_${log.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit entry",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDelete(log) },
                        modifier = Modifier.size(36.dp).testTag("delete_log_${log.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete entry",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nutritional Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NutrientBadge(
                    label = "Calories",
                    value = "${log.calories.toInt()} kcal",
                    color = PrimaryGreen,
                    modifier = Modifier.weight(1.2f)
                )
                NutrientBadge(
                    label = "Protein",
                    value = "${log.protein}g",
                    color = ProteinPurple,
                    modifier = Modifier.weight(1f)
                )
                NutrientBadge(
                    label = "Carbs",
                    value = "${log.carbohydrates}g",
                    color = CarbsOrange,
                    modifier = Modifier.weight(1f)
                )
                NutrientBadge(
                    label = "Fat",
                    value = "${log.fat}g",
                    color = FatPink,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NutrientBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF222732))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 12.sp
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMuted,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun rememberFormattedTime(timestamp: Long): String {
    return try {
        val instant = Instant.ofEpochMilli(timestamp)
        val zone = ZoneId.systemDefault()
        val time = instant.atZone(zone).toLocalTime()
        time.format(DateTimeFormatter.ofPattern("h:mm a"))
    } catch (e: Exception) {
        ""
    }
}
