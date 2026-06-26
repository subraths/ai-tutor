package com.tutorai.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * TutorAI — "Scholar" brand fallback palette.
 *
 * Used when Material You dynamic color is unavailable (API < 31) or disabled.
 * Editorial-warm direction: deep indigo "ink" primary, warm clay secondary,
 * leaf-green tertiary, on warm-paper neutrals. The amber "spotlight" used to
 * glow the diagram lives in [ExtendedColors] (it is not an M3 role).
 *
 * All values are hand-tuned for AA contrast in both schemes.
 */

// ---- Brand seeds (reference only) ----
// Primary  indigo ink   #4B4EA6
// Secondary warm clay    #8E5236
// Tertiary leaf green    #4C662B
// Spotlight amber        #F2B33C

// ---------- LIGHT ----------
private val Primary_L = Color(0xFF4B4EA6)
private val OnPrimary_L = Color(0xFFFFFFFF)
private val PrimaryContainer_L = Color(0xFFE1E0FF)
private val OnPrimaryContainer_L = Color(0xFF0B0C56)

private val Secondary_L = Color(0xFF8E5236)
private val OnSecondary_L = Color(0xFFFFFFFF)
private val SecondaryContainer_L = Color(0xFFFFDBCC)
private val OnSecondaryContainer_L = Color(0xFF341100)

private val Tertiary_L = Color(0xFF4C662B)
private val OnTertiary_L = Color(0xFFFFFFFF)
private val TertiaryContainer_L = Color(0xFFCDEDA3)
private val OnTertiaryContainer_L = Color(0xFF0F2000)

private val Error_L = Color(0xFFBA1A1A)
private val OnError_L = Color(0xFFFFFFFF)
private val ErrorContainer_L = Color(0xFFFFDAD6)
private val OnErrorContainer_L = Color(0xFF410002)

private val Background_L = Color(0xFFFBF8F2)
private val OnBackground_L = Color(0xFF201C15)
private val Surface_L = Color(0xFFFBF8F2)
private val OnSurface_L = Color(0xFF201C15)
private val SurfaceVariant_L = Color(0xFFE8E0D0)
private val OnSurfaceVariant_L = Color(0xFF4E463A)
private val Outline_L = Color(0xFF827969)
private val OutlineVariant_L = Color(0xFFD4CAB7)

private val SurfaceDim_L = Color(0xFFE3DCCE)
private val SurfaceBright_L = Color(0xFFFBF8F2)
private val SurfaceContainerLowest_L = Color(0xFFFFFFFF)
private val SurfaceContainerLow_L = Color(0xFFF4EEE3)
private val SurfaceContainer_L = Color(0xFFEFE8DB)
private val SurfaceContainerHigh_L = Color(0xFFE9E1D1)
private val SurfaceContainerHighest_L = Color(0xFFE2DAC7)

private val InverseSurface_L = Color(0xFF362F27)
private val InverseOnSurface_L = Color(0xFFFBEFE0)
private val InversePrimary_L = Color(0xFFC3C2FF)

// ---------- DARK ----------
private val Primary_D = Color(0xFFC3C2FF)
private val OnPrimary_D = Color(0xFF1A1C71)
private val PrimaryContainer_D = Color(0xFF33358A)
private val OnPrimaryContainer_D = Color(0xFFE1E0FF)

private val Secondary_D = Color(0xFFFFB596)
private val OnSecondary_D = Color(0xFF552003)
private val SecondaryContainer_D = Color(0xFF71361C)
private val OnSecondaryContainer_D = Color(0xFFFFDBCC)

private val Tertiary_D = Color(0xFFB1D188)
private val OnTertiary_D = Color(0xFF1F3700)
private val TertiaryContainer_D = Color(0xFF354E15)
private val OnTertiaryContainer_D = Color(0xFFCDEDA3)

private val Error_D = Color(0xFFFFB4AB)
private val OnError_D = Color(0xFF690005)
private val ErrorContainer_D = Color(0xFF93000A)
private val OnErrorContainer_D = Color(0xFFFFDAD6)

private val Background_D = Color(0xFF15120B)
private val OnBackground_D = Color(0xFFECE1CF)
private val Surface_D = Color(0xFF15120B)
private val OnSurface_D = Color(0xFFECE1CF)
private val SurfaceVariant_D = Color(0xFF4F4737)
private val OnSurfaceVariant_D = Color(0xFFD4C9B4)
private val Outline_D = Color(0xFF9D9280)
private val OutlineVariant_D = Color(0xFF4F4737)

private val SurfaceDim_D = Color(0xFF15120B)
private val SurfaceBright_D = Color(0xFF3C362D)
private val SurfaceContainerLowest_D = Color(0xFF100D07)
private val SurfaceContainerLow_D = Color(0xFF1D190F)
private val SurfaceContainer_D = Color(0xFF221E13)
private val SurfaceContainerHigh_D = Color(0xFF2C271B)
private val SurfaceContainerHighest_D = Color(0xFF383125)

private val InverseSurface_D = Color(0xFFECE1CF)
private val InverseOnSurface_D = Color(0xFF362F27)
private val InversePrimary_D = Color(0xFF4B4EA6)

val ScholarLightColors = lightColorScheme(
    primary = Primary_L, onPrimary = OnPrimary_L,
    primaryContainer = PrimaryContainer_L, onPrimaryContainer = OnPrimaryContainer_L,
    secondary = Secondary_L, onSecondary = OnSecondary_L,
    secondaryContainer = SecondaryContainer_L, onSecondaryContainer = OnSecondaryContainer_L,
    tertiary = Tertiary_L, onTertiary = OnTertiary_L,
    tertiaryContainer = TertiaryContainer_L, onTertiaryContainer = OnTertiaryContainer_L,
    error = Error_L, onError = OnError_L,
    errorContainer = ErrorContainer_L, onErrorContainer = OnErrorContainer_L,
    background = Background_L, onBackground = OnBackground_L,
    surface = Surface_L, onSurface = OnSurface_L,
    surfaceVariant = SurfaceVariant_L, onSurfaceVariant = OnSurfaceVariant_L,
    outline = Outline_L, outlineVariant = OutlineVariant_L,
    surfaceDim = SurfaceDim_L, surfaceBright = SurfaceBright_L,
    surfaceContainerLowest = SurfaceContainerLowest_L, surfaceContainerLow = SurfaceContainerLow_L,
    surfaceContainer = SurfaceContainer_L, surfaceContainerHigh = SurfaceContainerHigh_L,
    surfaceContainerHighest = SurfaceContainerHighest_L,
    inverseSurface = InverseSurface_L, inverseOnSurface = InverseOnSurface_L,
    inversePrimary = InversePrimary_L,
)

val ScholarDarkColors = darkColorScheme(
    primary = Primary_D, onPrimary = OnPrimary_D,
    primaryContainer = PrimaryContainer_D, onPrimaryContainer = OnPrimaryContainer_D,
    secondary = Secondary_D, onSecondary = OnSecondary_D,
    secondaryContainer = SecondaryContainer_D, onSecondaryContainer = OnSecondaryContainer_D,
    tertiary = Tertiary_D, onTertiary = OnTertiary_D,
    tertiaryContainer = TertiaryContainer_D, onTertiaryContainer = OnTertiaryContainer_D,
    error = Error_D, onError = OnError_D,
    errorContainer = ErrorContainer_D, onErrorContainer = OnErrorContainer_D,
    background = Background_D, onBackground = OnBackground_D,
    surface = Surface_D, onSurface = OnSurface_D,
    surfaceVariant = SurfaceVariant_D, onSurfaceVariant = OnSurfaceVariant_D,
    outline = Outline_D, outlineVariant = OutlineVariant_D,
    surfaceDim = SurfaceDim_D, surfaceBright = SurfaceBright_D,
    surfaceContainerLowest = SurfaceContainerLowest_D, surfaceContainerLow = SurfaceContainerLow_D,
    surfaceContainer = SurfaceContainer_D, surfaceContainerHigh = SurfaceContainerHigh_D,
    surfaceContainerHighest = SurfaceContainerHighest_D,
    inverseSurface = InverseSurface_D, inverseOnSurface = InverseOnSurface_D,
    inversePrimary = InversePrimary_D,
)

/** Amber spotlight + success accents that aren't part of the standard M3 role set. */
val SpotlightAmber_L = Color(0xFFA9700F)
val SpotlightAmberGlow_L = Color(0xFFF2B33C)
val SpotlightAmberContainer_L = Color(0xFFFBE6C0)
val Success_L = Color(0xFF3F6B4E)

val SpotlightAmber_D = Color(0xFFF0BE6E)
val SpotlightAmberGlow_D = Color(0xFFF3C977)
val SpotlightAmberContainer_D = Color(0xFF5A3F12)
val Success_D = Color(0xFFA6D6B0)
