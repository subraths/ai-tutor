package com.tutorai.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Segment scrubber — one pill per narration clip. Past segments are full, the
 * current one fills with [segmentProgress] (0f..1f) in real time, future ones
 * are track-coloured. Tap a pill to seek.
 *
 * Pass ExoPlayer's `currentMediaItemIndex` as [currentIndex] and the in-clip
 * fraction (position / duration) as [segmentProgress].
 */
@Composable
fun SegmentedProgress(
    segmentCount: Int,
    currentIndex: Int,
    segmentProgress: Float,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val track: Color = MaterialTheme.colorScheme.surfaceContainerHighest
    val fill: Color = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        for (i in 0 until segmentCount) {
            val target = when {
                i < currentIndex -> 1f
                i == currentIndex -> segmentProgress.coerceIn(0f, 1f)
                else -> 0f
            }
            val animated by animateFloatAsState(
                targetValue = target,
                animationSpec = tween(220),
                label = "segment$i",
            )
            val interaction = remember { MutableInteractionSource() }
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(18.dp)               // 18dp row keeps the touch target tall
                    .semantics { contentDescription = "Go to segment ${i + 1}" }
                    .clickable(
                        interactionSource = interaction,
                        indication = null,       // no ripple on a 6dp bar
                        onClick = { onSeek(i) },
                    ),
            ) {
                val barHeight = 6.dp.toPx()
                val top = (size.height - barHeight) / 2f
                val r = barHeight / 2f
                drawRoundRect(
                    color = track,
                    topLeft = Offset(0f, top),
                    size = size.copy(height = barHeight),
                    cornerRadius = CornerRadius(r, r),
                )
                if (animated > 0f) {
                    drawRoundRect(
                        color = fill,
                        topLeft = Offset(0f, top),
                        size = size.copy(width = size.width * animated, height = barHeight),
                        cornerRadius = CornerRadius(r, r),
                    )
                }
            }
        }
    }
}
