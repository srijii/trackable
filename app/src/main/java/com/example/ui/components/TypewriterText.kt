package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.roundToInt

/**
 * Shared reveal progress for a group of fields. Every field that reads this
 * value starts typing at the same moment and finishes together, which is what
 * makes an AI food card feel like it is being written rather than assembled.
 *
 * The progress is saveable, so navigating away and back does not replay the
 * animation for the same result.
 */
@Composable
fun rememberTypewriterProgress(
    key: Any?,
    durationMillis: Int = 760
): Float {
    val reducedMotion = rememberReducedMotion()
    var progress by rememberSaveable(key) {
        mutableStateOf(if (reducedMotion) 1f else 0f)
    }

    LaunchedEffect(key, reducedMotion) {
        if (reducedMotion) {
            progress = 1f
            return@LaunchedEffect
        }
        if (progress >= 1f) return@LaunchedEffect

        val startProgress = progress
        var startFrame = 0L
        withFrameNanos { startFrame = it }
        while (progress < 1f) {
            withFrameNanos { now ->
                val elapsedMs = (now - startFrame) / 1_000_000f
                progress = (startProgress + elapsedMs / durationMillis)
                    .coerceIn(0f, 1f)
            }
        }
    }

    return progress
}

/**
 * Reveals [text] according to [progress]. The full string is laid out invisibly
 * underneath so the row does not jump around while characters appear.
 */
@Composable
fun TypewriterText(
    text: String,
    progress: Float,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val visibleCount = remember(text, progress) {
        (text.length * progress.coerceIn(0f, 1f)).roundToInt().coerceIn(0, text.length)
    }
    Box(modifier = modifier) {
        Text(
            text = text,
            style = style,
            color = Color.Transparent,
            maxLines = maxLines,
            overflow = overflow
        )
        Text(
            text = text.take(visibleCount),
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}
