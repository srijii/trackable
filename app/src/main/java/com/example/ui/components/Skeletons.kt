package com.example.ui.components

import android.provider.Settings
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCardHigh

/**
 * Respect Android's "remove animations" accessibility setting.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}

/**
 * One shared shimmer brush per skeleton surface. Reusing a single animation
 * phase keeps the placeholders in sync and avoids many independent transitions.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    if (rememberReducedMotion()) {
        return remember { SolidColor(SurfaceCardHigh) }
    }
    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val offset by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeleton_offset"
    )
    return Brush.linearGradient(
        colors = listOf(
            SurfaceCardHigh,
            SurfaceBorder.copy(alpha = 0.85f),
            SurfaceCardHigh
        ),
        start = Offset(offset - 420f, 0f),
        end = Offset(offset, 420f)
    )
}

@Composable
fun SkeletonBar(
    widthFraction: Float,
    height: Dp,
    brush: Brush,
    modifier: Modifier = Modifier,
    radius: Dp = height * 0.5f
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction.coerceIn(0.02f, 1f))
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(brush)
    )
}

@Composable
fun SkeletonBarFixed(
    width: Dp,
    height: Dp,
    brush: Brush,
    modifier: Modifier = Modifier,
    radius: Dp = height * 0.5f
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(brush)
    )
}

/**
 * A placeholder shaped like one real food row: name, serving, macros and calories.
 */
@Composable
fun FoodEntrySkeletonRow(
    brush: Brush,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SkeletonBar(widthFraction = 0.54f, height = 14.dp, brush = brush)
                Spacer(modifier = Modifier.height(7.dp))
                SkeletonBar(widthFraction = 0.33f, height = 10.dp, brush = brush)
                Spacer(modifier = Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SkeletonBarFixed(width = 36.dp, height = 9.dp, brush = brush)
                    SkeletonBarFixed(width = 36.dp, height = 9.dp, brush = brush)
                    SkeletonBarFixed(width = 36.dp, height = 9.dp, brush = brush)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                SkeletonBarFixed(width = 58.dp, height = 14.dp, brush = brush)
                Spacer(modifier = Modifier.height(7.dp))
                SkeletonBarFixed(width = 36.dp, height = 9.dp, brush = brush)
            }
        }
        if (showDivider) {
            TrackableDivider(color = SurfaceBorder.copy(alpha = 0.4f))
        }
    }
}

/** Placeholder for the dashboard calories + macros block. */
@Composable
fun DashboardStatsSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth()) {
        SkeletonBar(widthFraction = 0.22f, height = 10.dp, brush = brush)
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            SkeletonBarFixed(width = 112.dp, height = 38.dp, brush = brush, radius = 10.dp)
            Spacer(modifier = Modifier.width(10.dp))
            SkeletonBarFixed(width = 76.dp, height = 14.dp, brush = brush)
        }
        Spacer(modifier = Modifier.height(16.dp))
        SkeletonBar(widthFraction = 1f, height = 7.dp, brush = brush)
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            repeat(3) {
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBarFixed(width = 52.dp, height = 10.dp, brush = brush)
                    Spacer(modifier = Modifier.height(8.dp))
                    SkeletonBar(widthFraction = 0.8f, height = 15.dp, brush = brush)
                    Spacer(modifier = Modifier.height(8.dp))
                    SkeletonBar(widthFraction = 1f, height = 4.dp, brush = brush)
                }
            }
        }
    }
}

/** Placeholder for the dashboard food timeline. */
@Composable
fun DashboardFoodSkeleton(rows: Int = 2, modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBarFixed(width = 54.dp, height = 10.dp, brush = brush)
            Spacer(modifier = Modifier.weight(1f))
            SkeletonBarFixed(width = 44.dp, height = 10.dp, brush = brush)
        }
        Spacer(modifier = Modifier.height(6.dp))
        repeat(rows) {
            FoodEntrySkeletonRow(brush = brush)
        }
    }
}

/** Placeholder for a list of inventory foods. */
@Composable
fun InventorySkeleton(modifier: Modifier = Modifier, rows: Int = 7) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 13.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBar(widthFraction = 0.52f, height = 14.dp, brush = brush)
                    Spacer(modifier = Modifier.height(7.dp))
                    SkeletonBar(widthFraction = 0.38f, height = 10.dp, brush = brush)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        SkeletonBarFixed(width = 44.dp, height = 9.dp, brush = brush)
                        SkeletonBarFixed(width = 44.dp, height = 9.dp, brush = brush)
                        SkeletonBarFixed(width = 44.dp, height = 9.dp, brush = brush)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    SkeletonBarFixed(width = 50.dp, height = 14.dp, brush = brush)
                    Spacer(modifier = Modifier.height(7.dp))
                    SkeletonBarFixed(width = 30.dp, height = 10.dp, brush = brush)
                }
            }
            TrackableDivider(color = SurfaceBorder.copy(alpha = 0.4f))
        }
    }
}

/** Placeholder for the current weight heading. */
@Composable
fun WeightSummarySkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth()) {
        SkeletonBarFixed(width = 88.dp, height = 10.dp, brush = brush)
        Spacer(modifier = Modifier.height(10.dp))
        SkeletonBarFixed(width = 138.dp, height = 44.dp, brush = brush, radius = 10.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            repeat(3) {
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBarFixed(width = 42.dp, height = 9.dp, brush = brush)
                    Spacer(modifier = Modifier.height(7.dp))
                    SkeletonBarFixed(width = 64.dp, height = 14.dp, brush = brush)
                }
            }
        }
    }
}

/** Placeholder for the weight history timeline. */
@Composable
fun WeightHistorySkeleton(modifier: Modifier = Modifier, rows: Int = 5) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBarFixed(width = 104.dp, height = 13.dp, brush = brush)
                    Spacer(modifier = Modifier.height(7.dp))
                    SkeletonBarFixed(width = 70.dp, height = 9.dp, brush = brush)
                }
                SkeletonBarFixed(width = 62.dp, height = 15.dp, brush = brush)
            }
            TrackableDivider(color = SurfaceBorder.copy(alpha = 0.4f))
        }
    }
}

/** Placeholder for the history day summary and grouped meals. */
@Composable
fun HistoryDaySkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                SkeletonBarFixed(width = 40.dp, height = 9.dp, brush = brush)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBarFixed(width = 104.dp, height = 26.dp, brush = brush, radius = 8.dp)
            }
            Column(horizontalAlignment = Alignment.End) {
                SkeletonBarFixed(width = 34.dp, height = 9.dp, brush = brush)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBarFixed(width = 66.dp, height = 18.dp, brush = brush)
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        TrackableDivider()
        Spacer(modifier = Modifier.height(12.dp))
        repeat(3) {
            SkeletonBarFixed(width = 58.dp, height = 10.dp, brush = brush)
            Spacer(modifier = Modifier.height(6.dp))
            FoodEntrySkeletonRow(brush = brush)
        }
    }
}

/** Placeholder for the dashboard water card. */
@Composable
fun WaterCardSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBarFixed(width = 92.dp, height = 13.dp, brush = brush)
            Spacer(Modifier.weight(1f))
            SkeletonBarFixed(width = 54.dp, height = 11.dp, brush = brush)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(brush)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBarFixed(width = 34.dp, height = 34.dp, brush = brush, radius = 17.dp)
            SkeletonBarFixed(width = 124.dp, height = 10.dp, brush = brush)
            SkeletonBarFixed(width = 116.dp, height = 32.dp, brush = brush, radius = 9.dp)
        }
    }
}

/** Placeholder for chart cards while analytics data is being computed. */
@Composable
fun ChartSkeleton(modifier: Modifier = Modifier, height: Dp = 180.dp) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth()) {
        SkeletonBarFixed(width = 128.dp, height = 14.dp, brush = brush)
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBarFixed(width = 90.dp, height = 9.dp, brush = brush)
        Spacer(modifier = Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(10.dp))
                .background(brush)
        )
    }
}
