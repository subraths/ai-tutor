package com.tutorai.app.ui.components

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tutorai.app.ui.theme.LocalSpacing

/**
 * Generation progress — a determinate ring + a staged checklist with a shimmer
 * on the active row. Turns dead wait-time into reassuring, legible progress.
 *
 * @param percent      0..100. Pass -1 for an indeterminate spinner ring.
 * @param stageIndex   which checklist row is active (rows above = done).
 * @param stages       ordered stage labels (see DefaultGenerationStages).
 * @param topic        echoed in the sub-label ("building 'Photosynthesis'").
 */
@Composable
fun GenerationProgress(
    percent: Int,
    stageIndex: Int,
    stages: List<String>,
    topic: String,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProgressRing(percent = percent, reducedMotion = reducedMotion)

        Text(
            text = stages.getOrElse(stageIndex.coerceIn(0, stages.lastIndex)) { "Working…" },
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = spacing.l),
        )
        Text(
            text = if (topic.isBlank()) "Hang tight — building your lesson." else "Hang tight — building “$topic”.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xs),
        )

        Column(
            modifier = Modifier.padding(top = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.s),
        ) {
            stages.forEachIndexed { i, label ->
                StageRow(
                    label = label,
                    done = i < stageIndex,
                    active = i == stageIndex,
                    reducedMotion = reducedMotion,
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(percent: Int, reducedMotion: Boolean) {
    val ringTrack = MaterialTheme.colorScheme.surfaceContainerHigh
    val ringColor = MaterialTheme.colorScheme.primary
    val indeterminate = percent < 0

    val sweep by if (indeterminate) {
        val t = rememberInfiniteTransition(label = "ring")
        t.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
            label = "sweep",
        )
    } else {
        animateFloatAsState(targetValue = 360f * (percent.coerceIn(0, 100) / 100f), animationSpec = tween(400), label = "sweep")
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2f
            drawArc(
                color = ringTrack, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = ringColor,
                startAngle = if (indeterminate) sweep else -90f,
                sweepAngle = if (indeterminate) 90f else sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        if (!indeterminate) {
            Text(
                text = "${percent.coerceIn(0, 100)}%",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun StageRow(label: String, done: Boolean, active: Boolean, reducedMotion: Boolean) {
    val cs = MaterialTheme.colorScheme
    val bg = when {
        active -> cs.primaryContainer
        done -> cs.surfaceContainerLow
        else -> Color.Transparent
    }
    val border = if (active) cs.primaryContainer else cs.outlineVariant
    val fg = when {
        active -> cs.onPrimaryContainer
        done -> cs.onSurface
        else -> cs.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        // status dot
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    when {
                        done -> cs.tertiary
                        active -> cs.primary
                        else -> Color.Transparent
                    },
                ),
        ) {
            when {
                done -> Text("✓", color = cs.onTertiary, fontWeight = FontWeight.Bold)
                active -> Box(
                    Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(cs.onPrimary),
                )
                else -> Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(cs.onSurfaceVariant.copy(alpha = 0.5f)),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = fg,
            modifier = Modifier.weight(1f),
        )
        if (active) {
            ShimmerBar(reducedMotion = reducedMotion)
        }
    }
}

@Composable
private fun ShimmerBar(reducedMotion: Boolean) {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val hi = MaterialTheme.colorScheme.primary
    val x by if (reducedMotion) {
        remember { mutableFloatStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "shimmer").animateFloat(
            initialValue = -1f, targetValue = 2f,
            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
            label = "x",
        )
    }
    Box(
        Modifier
            .size(width = 54.dp, height = 7.dp)
            .clip(RoundedCornerShape(50))
            .background(base)
            .clearAndSetSemantics { },
    ) {
        if (!reducedMotion) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.5f to hi,
                            1f to Color.Transparent,
                            startX = x * 54f - 54f,
                            endX = x * 54f,
                        ),
                    ),
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(7.dp)
                    .background(hi),
            )
        }
    }
}
