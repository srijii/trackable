package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.sin

@Composable
fun AnimatedWaterCard(
    waterConsumedMl: Int,
    waterGoalMl: Int = 3000,
    cupSize1Ml: Int = 250,
    cupSize2Ml: Int = 500,
    onAddCup1: () -> Unit = {},
    onRemoveCup: () -> Unit = {},
    onAddCup2: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val progress = if (waterGoalMl > 0) {
        (waterConsumedMl.toFloat() / waterGoalMl.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "water_fill_anim"
    )

    // Infinite wave oscillation
    val infiniteTransition = rememberInfiniteTransition(label = "wave_transition")
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase_1"
    )

    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase_2"
    )

    val waterRemainingMl = (waterGoalMl - waterConsumedMl).coerceAtLeast(0)
    val litersConsumed = String.format("%.1f", waterConsumedMl / 1000.0)
    val litersGoal = String.format("%.1f", waterGoalMl / 1000.0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("animated_water_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
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
                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Water",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Water Intake",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Text(
                    text = "${litersConsumed}L / ${litersGoal}L",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // -------------------------------------------------------------
            // ANIMATED WATER WAVE PROGRESS BAR
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF10141C))
                    .border(1.dp, Color(0xFF263345), RoundedCornerShape(16.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    if (animatedProgress > 0.01f) {
                        val baseWaterY = height * (1f - animatedProgress)
                        val waveAmplitude = 4.dp.toPx()

                        // Layer 1: Back deeper wave
                        val backPath = Path().apply {
                            moveTo(0f, height)
                            lineTo(0f, baseWaterY)
                            var x = 0f
                            while (x <= width) {
                                val y = baseWaterY + waveAmplitude * sin((x / width * 2 * Math.PI + wavePhase2).toDouble()).toFloat()
                                lineTo(x, y)
                                x += 10f
                            }
                            lineTo(width, height)
                            close()
                        }
                        drawPath(
                            path = backPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0x800284C7), Color(0x600369A1)),
                                startY = baseWaterY,
                                endY = height
                            )
                        )

                        // Layer 2: Front glowing wave
                        val frontPath = Path().apply {
                            moveTo(0f, height)
                            lineTo(0f, baseWaterY)
                            var x = 0f
                            while (x <= width) {
                                val y = baseWaterY + waveAmplitude * sin((x / width * 2 * Math.PI + wavePhase1).toDouble()).toFloat()
                                lineTo(x, y)
                                x += 10f
                            }
                            lineTo(width, height)
                            close()
                        }
                        drawPath(
                            path = frontPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xDD38BDF8), Color(0xAA0284C7)),
                                startY = baseWaterY,
                                endY = height
                            )
                        )

                        // Specular crest line highlight
                        val crestPath = Path().apply {
                            var first = true
                            var x = 0f
                            while (x <= width) {
                                val y = baseWaterY + waveAmplitude * sin((x / width * 2 * Math.PI + wavePhase1).toDouble()).toFloat()
                                if (first) {
                                    moveTo(x, y)
                                    first = false
                                } else {
                                    lineTo(x, y)
                                }
                                x += 10f
                            }
                        }
                        drawPath(
                            path = crestPath,
                            color = Color(0xFFBAE6FD).copy(alpha = 0.8f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // Overlay Content: Status & Percentage inside the vessel
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$waterConsumedMl ml",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "consumed",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF111722).copy(alpha = 0.8f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        )
                    }
                }
            }

            // Quick Add / Remove Controls Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minus Cup
                IconButton(
                    onClick = onRemoveCup,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E232B))
                        .testTag("remove_water_cup_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Remove Cup",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Cups Remaining Label
                Text(
                    text = if (waterRemainingMl > 0) "$waterRemainingMl ml Remaining" else "Daily Target Reached! 🎉",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (waterRemainingMl > 0) TextSecondary else Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                )

                // Quick Increment Action Buttons (the 2 configured cup sizes)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Cup 1
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E232B))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                            .clickable { onAddCup1() }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("add_cup_1_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$cupSize1Ml ml",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Cup 2
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E232B))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                            .clickable { onAddCup2() }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("add_cup_2_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$cupSize2Ml ml",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
