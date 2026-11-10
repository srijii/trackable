package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.dashboard.AiLoggedSummary
import com.example.ui.screens.dashboard.AiProcessingStage
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WaterCyan

/**
 * The loading state is deliberately not a summary card. It is shaped like the
 * food rows that will replace it, so the dashboard does not jump when the AI
 * response arrives.
 */
@Composable
fun DashboardAiAnalysingCard(
    stage: AiProcessingStage,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brush = rememberShimmerBrush()
    val reducedMotion = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "ai_pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_pulse_alpha"
    )
    val pulseAlpha = if (reducedMotion) 0.8f else pulse

    val status = when (stage) {
        AiProcessingStage.ANALYSING -> "Identifying food and portions"
        AiProcessingStage.SAVING -> "Saving your meal"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark.copy(alpha = 0.62f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .alpha(pulseAlpha)
                    .background(ClassicRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "AI",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                ),
                color = ClassicRed
            )
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel AI request",
                    tint = TextMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Two rows, same geometry as real logged foods.
        FoodEntrySkeletonRow(brush = brush, showDivider = true)
        FoodEntrySkeletonRow(brush = brush, showDivider = false)
    }
}

/**
 * The response writes itself into the same food-row layout used by the rest of
 * the dashboard. Every field in an item uses one shared progress value, so the
 * name, quantity, calories and macros appear simultaneously rather than one
 * string being revealed after another.
 */
@Composable
fun DashboardAiLoggedItems(
    summary: AiLoggedSummary,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reveal = rememberTypewriterProgress(
        key = summary.items.firstOrNull()?.key?.let { "ai_$it" } ?: "ai_empty",
        durationMillis = 820
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = ClassicRed,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Added from your description",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${summary.items.size} item${if (summary.items.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss AI result",
                    tint = TextMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        summary.items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 11.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    TypewriterText(
                        text = item.name,
                        progress = reveal,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    TypewriterText(
                        text = buildString {
                            append(item.quantity)
                            append(" ")
                            append(item.servingUnit)
                            if (item.brand.isNotBlank()) {
                                append(" · ")
                                append(item.brand)
                            }
                            if (item.mealType.isNotBlank()) {
                                append(" · ")
                                append(item.mealType)
                            }
                        },
                        progress = reveal,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TypewriterText(
                            text = "P ${formatMacroValue(item.protein)}g",
                            progress = reveal,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        TypewriterText(
                            text = "C ${formatMacroValue(item.carbs)}g",
                            progress = reveal,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        TypewriterText(
                            text = "F ${formatMacroValue(item.fat)}g",
                            progress = reveal,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(horizontalAlignment = Alignment.End) {
                    TypewriterText(
                        text = "${item.calories.toInt()}",
                        progress = reveal,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    TypewriterText(
                        text = "kcal",
                        progress = reveal,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
            if (index < summary.items.lastIndex) {
                TrackableDivider()
            }
        }

        if (summary.skippedNames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            TypewriterText(
                text = "Not in inventory: ${summary.skippedNames.joinToString(", ")}",
                progress = reveal,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        if (summary.waterMl > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            TypewriterText(
                text = "+${summary.waterMl} ml water",
                progress = reveal,
                style = MaterialTheme.typography.labelMedium,
                color = WaterCyan
            )
        }
    }
}
