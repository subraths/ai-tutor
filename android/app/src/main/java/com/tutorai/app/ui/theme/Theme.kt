package com.tutorai.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Extra semantic colors that have no slot in [androidx.compose.material3.ColorScheme]:
 * the amber "spotlight" (diagram highlight) and a success green. Exposed through
 * a CompositionLocal so screens read them like any theme value:
 *
 *     val amber = LocalExtendedColors.current.spotlightGlow
 */
@Immutable
data class ExtendedColors(
    val spotlight: Color,
    val spotlightGlow: Color,
    val spotlightContainer: Color,
    val success: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        spotlight = SpotlightAmber_L,
        spotlightGlow = SpotlightAmberGlow_L,
        spotlightContainer = SpotlightAmberContainer_L,
        success = Success_L,
    )
}

/** Shorthand: `MaterialTheme.extended.spotlightGlow`. */
val MaterialTheme.extended: ExtendedColors
    @Composable get() = LocalExtendedColors.current

/**
 * App theme.
 *
 * @param darkTheme    follow the system by default.
 * @param dynamicColor Material You — on by default on Android 12+. Falls back to
 *                     the warm "Scholar" brand palette below that, or when off.
 *
 * NOTE on edge-to-edge: call `enableEdgeToEdge()` in the Activity (see MainActivity).
 * We deliberately do NOT set window.statusBarColor here — that API is deprecated
 * on Android 15 (compileSdk 35) and edge-to-edge handles it.
 */
@Composable
fun TutorAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> ScholarDarkColors
        else -> ScholarLightColors
    }

    val extended = if (darkTheme) {
        ExtendedColors(SpotlightAmber_D, SpotlightAmberGlow_D, SpotlightAmberContainer_D, Success_D)
    } else {
        ExtendedColors(SpotlightAmber_L, SpotlightAmberGlow_L, SpotlightAmberContainer_L, Success_L)
    }

    CompositionLocalProvider(
        LocalExtendedColors provides extended,
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ScholarTypography,
            shapes = ScholarShapes,
            content = content,
        )
    }
}
