package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Shared spacing scale. Keeping these values in one place is what makes the
 * screens feel like one product instead of separate layouts.
 */
object TrackSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp

    /** Standard horizontal gutter used by every screen. */
    val screenGutter: Dp = 16.dp

    /** Space between major screen sections. */
    val sectionGap: Dp = 24.dp
}

/**
 * A quiet section heading with an optional supporting line and trailing action.
 * Content is aligned to the screen gutter, not wrapped in a card.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            if (!supportingText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(TrackSpacing.sm))
            trailing()
        }
    }
}

@Composable
fun TrackableDivider(
    modifier: Modifier = Modifier,
    color: Color = SurfaceBorder.copy(alpha = 0.7f),
    thickness: Dp = 0.5.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}

/**
 * Thin, animated progress indicator used for calories, macros and water.
 * No pill container: the bar itself carries the meaning.
 */
@Composable
fun ThinProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = ClassicRed,
    trackColor: Color = SurfaceBorder.copy(alpha = 0.55f),
    height: Dp = 6.dp,
    animate: Boolean = true
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "thin_progress"
    )
    val shown = if (animate) animated else target
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        if (shown > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(shown)
                    .clip(RoundedCornerShape(height * 0.5f))
                    .background(color)
            )
        }
    }
}

/**
 * A statistic with a quiet label. Used directly on the background rather than
 * inside its own tile.
 */
@Composable
fun MetricBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
    supporting: String? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            fontWeight = FontWeight.SemiBold
        )
        if (!supporting.isNullOrBlank()) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TrackSpacing.lg, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextMuted.copy(alpha = 0.65f),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(12.dp))
            action()
        }
    }
}

/**
 * A compact tappable row used for settings entries, backup actions and other
 * one-line controls. Separators are owned by the caller so groups stay clean.
 */
@Composable
fun CompactActionRow(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onClick)
                } else Modifier
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(TrackSpacing.md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            if (!supportingText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(TrackSpacing.sm))
            trailing()
        }
    }
}

/** Formats a quantity without a trailing ".0". */
fun formatFoodQuantity(quantity: Double): String =
    if (quantity % 1.0 == 0.0) quantity.toInt().toString()
    else String.format(java.util.Locale.US, "%.1f", quantity)

/** Formats macro values compactly (one decimal only when required). */
fun formatMacroValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(java.util.Locale.US, "%.1f", value)

/** Shared macro sentence used in food rows so the same wording appears everywhere. */
@Composable
fun MacroSummaryLine(
    protein: Double,
    carbs: Double,
    fat: Double,
    modifier: Modifier = Modifier,
    color: Color = TextMuted
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TrackSpacing.md)
    ) {
        Text(
            text = "P ${formatMacroValue(protein)}g",
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
        Text(
            text = "C ${formatMacroValue(carbs)}g",
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
        Text(
            text = "F ${formatMacroValue(fat)}g",
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
fun InlineLabel(text: String, modifier: Modifier = Modifier, color: Color = TextMuted) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
        color = color
    )
}
