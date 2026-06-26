package com.tutorai.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing & sizing tokens. Use these instead of magic numbers so density stays
 * consistent. Access via `LocalSpacing.current` or the `Spacing` default.
 *
 * Base unit = 4dp. Touch targets never below 48dp (accessibility).
 */
@Immutable
data class Spacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val huge: Dp = 48.dp,
    // screen gutters
    val screenH: Dp = 22.dp,
    // minimum interactive target
    val touchTarget: Dp = 48.dp,
    // the one expressive control
    val playFab: Dp = 78.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
