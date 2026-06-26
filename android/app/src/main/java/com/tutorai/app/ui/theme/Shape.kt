package com.tutorai.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape scale — "rounded, calm". Consistent soft corners across the system,
 * with `CircleShape` reserved for the one expressive moment: the player's
 * play/pause FAB and avatar-style icon buttons.
 *
 *  extraSmall  8dp   chips inner, small toggles
 *  small      12dp   text fields, dense buttons
 *  medium     16dp   standard buttons, list rows
 *  large      20dp   cards
 *  extraLarge 28dp   hero cards, the diagram surface, bottom sheets
 */
val ScholarShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Convenience shapes that don't map onto the 5-slot M3 scale. */
object ScholarShapeTokens {
    val Field = RoundedCornerShape(18.dp)
    val LessonCard = RoundedCornerShape(26.dp)
    val DiagramSurface = RoundedCornerShape(28.dp)
    val Thumbnail = RoundedCornerShape(16.dp)
    val Pill = RoundedCornerShape(percent = 50)
}
