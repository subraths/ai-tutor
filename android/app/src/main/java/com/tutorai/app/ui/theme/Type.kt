package com.tutorai.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type system — editorial warmth.
 *
 *  • Display & headlines → a serif voice. Gives the app its scholarly, magazine
 *    feel on titles, lesson names, captions.
 *  • Everything functional (titles, body, labels, buttons) → a humanist sans.
 *
 * To keep the build hermetic and offline-safe, the families resolve to the
 * platform serif/sans (Noto Serif / the system sans on Android), so the
 * serif↔sans hierarchy that drives the design works with zero assets, zero new
 * dependencies, and no Play-Services font provider.
 *
 * UPGRADE PATH — exact "Newsreader" + "Hanken Grotesk" via Google Downloadable
 * Fonts (matches the prototype 1:1):
 *   1. build.gradle(:app):
 *        implementation("androidx.compose.ui:ui-text-google-fonts:<compose-ui-version>")
 *   2. add res/values/font_certs.xml (Android Studio: res ▸ New ▸ Downloadable font)
 *   3. replace the two families below with:
 *        private val provider = GoogleFont.Provider(
 *            "com.google.android.gms.fonts", "com.google.android.gms",
 *            R.array.com_google_android_gms_fonts_certs,
 *        )
 *        val DisplaySerif = FontFamily(Font(GoogleFont("Newsreader"), provider, FontWeight.W600), …)
 *        val BodySans     = FontFamily(Font(GoogleFont("Hanken Grotesk"), provider, FontWeight.W400), …)
 * Nothing else in this file changes.
 */
val DisplaySerif: FontFamily = FontFamily.Serif
val BodySans: FontFamily = FontFamily.SansSerif

/**
 * M3 type scale. Display/headline use the serif; title/body/label use the sans.
 * Letter-spacing tightened on the large serif sizes for an editorial feel.
 */
val ScholarTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplaySerif, fontWeight = FontWeight.W600, fontSize = 52.sp, lineHeight = 56.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontFamily = DisplaySerif, fontWeight = FontWeight.W600, fontSize = 42.sp, lineHeight = 48.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontFamily = DisplaySerif, fontWeight = FontWeight.W600, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.4).sp),

    headlineLarge = TextStyle(fontFamily = DisplaySerif, fontWeight = FontWeight.W600, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontFamily = DisplaySerif, fontWeight = FontWeight.W600, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = DisplaySerif, fontWeight = FontWeight.W600, fontSize = 22.sp, lineHeight = 28.sp),

    titleLarge = TextStyle(fontFamily = BodySans, fontWeight = FontWeight.W700, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = BodySans, fontWeight = FontWeight.W700, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = BodySans, fontWeight = FontWeight.W600, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),

    bodyLarge = TextStyle(fontFamily = BodySans, fontWeight = FontWeight.W400, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp),
    bodyMedium = TextStyle(fontFamily = BodySans, fontWeight = FontWeight.W400, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    bodySmall = TextStyle(fontFamily = BodySans, fontWeight = FontWeight.W400, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),

    labelLarge = TextStyle(fontFamily = BodySans, fontWeight = FontWeight.W700, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = BodySans, fontWeight = FontWeight.W700, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = BodySans, fontWeight = FontWeight.W700, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)
