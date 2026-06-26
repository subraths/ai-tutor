package com.tutorai.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tutorai.app.ui.theme.extended

/**
 * TutorAI mascot — a friendly "book that's eager to learn" with a sprout.
 *
 * PLACEHOLDER ART. Drawn in Canvas so the redesign ships with personality and
 * zero assets. For final art, replace with the real illustration: a
 * VectorDrawable (`Image(painterResource(R.drawable.mascot))`) or a Lottie for a
 * subtle idle loop. Keep the gentle float — it's the friendly beat of the empty
 * state. Colours come from the theme, so it adapts to light/dark and Material You.
 */
@Composable
fun TutorAiMascot(
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    val sprout = cs.tertiary
    val sproutLeaf = cs.tertiaryContainer
    val body = cs.primary
    val face = cs.primaryContainer
    val eyes = cs.onPrimaryContainer
    val cheek = cs.secondary
    val amber = MaterialTheme.extended.spotlightGlow
    val amberContainer = MaterialTheme.extended.spotlightContainer

    val bob by if (animate) {
        rememberInfiniteTransition(label = "mascot").animateFloat(
            initialValue = -6f, targetValue = 6f,
            animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
            label = "bob",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Canvas(
        modifier = modifier.semantics { contentDescription = "TutorAI mascot — a cheerful book ready to teach" },
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val y = bob

        // soft ground shadow
        drawOval(
            color = cs.onSurface.copy(alpha = 0.06f),
            topLeft = Offset(cx - w * 0.30f, h * 0.86f),
            size = Size(w * 0.60f, h * 0.07f),
        )

        // sprout (leaf + stem) on top
        val stemTop = h * 0.10f + y
        val sproutPath = Path().apply {
            moveTo(cx, h * 0.22f + y)
            cubicTo(cx + w * 0.04f, h * 0.16f + y, cx + w * 0.14f, h * 0.14f + y, cx + w * 0.17f, h * 0.10f + y)
            cubicTo(cx + w * 0.10f, h * 0.085f + y, cx + w * 0.02f, h * 0.135f + y, cx, h * 0.22f + y)
        }
        drawPath(sproutPath, color = sprout)
        val leftLeaf = Path().apply {
            moveTo(cx, h * 0.22f + y)
            cubicTo(cx - w * 0.03f, h * 0.15f + y, cx - w * 0.10f, h * 0.13f + y, cx - w * 0.12f, stemTop)
            cubicTo(cx - w * 0.05f, h * 0.12f + y, cx + w * 0.005f, h * 0.17f + y, cx, h * 0.22f + y)
        }
        drawPath(leftLeaf, color = sproutLeaf)

        // body (rounded book)
        val bodyRect = RoundRect(
            Rect(Offset(w * 0.18f, h * 0.22f + y), Size(w * 0.64f, h * 0.62f)),
            CornerRadius(w * 0.16f, w * 0.16f),
        )
        drawPath(Path().apply { addRoundRect(bodyRect) }, color = body)

        // face panel
        val faceRect = RoundRect(
            Rect(Offset(w * 0.27f, h * 0.31f + y), Size(w * 0.46f, h * 0.44f)),
            CornerRadius(w * 0.10f, w * 0.10f),
        )
        drawPath(Path().apply { addRoundRect(faceRect) }, color = face)

        // eyes
        val eyeY = h * 0.50f + y
        drawCircle(eyes, radius = w * 0.05f, center = Offset(cx - w * 0.085f, eyeY))
        drawCircle(eyes, radius = w * 0.05f, center = Offset(cx + w * 0.085f, eyeY))
        drawCircle(Color.White, radius = w * 0.018f, center = Offset(cx - w * 0.07f, eyeY - h * 0.012f))
        drawCircle(Color.White, radius = w * 0.018f, center = Offset(cx + w * 0.10f, eyeY - h * 0.012f))

        // cheeks
        drawCircle(cheek.copy(alpha = 0.5f), radius = w * 0.035f, center = Offset(cx - w * 0.17f, h * 0.585f + y))
        drawCircle(cheek.copy(alpha = 0.5f), radius = w * 0.035f, center = Offset(cx + w * 0.17f, h * 0.585f + y))

        // smile
        val smile = Path().apply {
            moveTo(cx - w * 0.085f, h * 0.61f + y)
            cubicTo(cx - w * 0.03f, h * 0.66f + y, cx + w * 0.03f, h * 0.66f + y, cx + w * 0.085f, h * 0.61f + y)
        }
        drawPath(smile, color = eyes, style = Stroke(width = w * 0.022f, cap = StrokeCap.Round))

        // little "idea" spark, top-right
        val sx = w * 0.84f
        val sy = h * 0.20f + y
        drawCircle(amberContainer, radius = w * 0.075f, center = Offset(sx, sy))
        drawArc(
            color = amber,
            startAngle = -90f, sweepAngle = 270f, useCenter = false,
            topLeft = Offset(sx - w * 0.03f, sy - w * 0.03f),
            size = Size(w * 0.06f, w * 0.06f),
            style = Stroke(width = w * 0.016f, cap = StrokeCap.Round),
        )
    }
}
