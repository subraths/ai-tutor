package com.tutorai.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ClosedCaptionOff
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Single icon seam for the whole app. Backed by `material-icons-extended` — the
 * only dependency this redesign adds. It's the standard Compose icon set; in
 * release builds R8/Proguard strips every icon object you don't reference, so
 * the APK only carries these ~19. (Prefer zero deps? Replace each val with a
 * hand-built ImageVector — nothing else in the codebase changes.)
 */
object TutorIcons {
    val Back: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack
    val Forward: ImageVector = Icons.AutoMirrored.Rounded.ArrowForward
    val Home: ImageVector = Icons.Rounded.Home
    val History: ImageVector = Icons.Rounded.History
    val Settings: ImageVector = Icons.Rounded.Settings
    val Search: ImageVector = Icons.Rounded.Search
    val Fullscreen: ImageVector = Icons.Rounded.Fullscreen
    val FullscreenExit: ImageVector = Icons.Rounded.FullscreenExit
    val Caption: ImageVector = Icons.Rounded.ClosedCaption
    val CaptionOff: ImageVector = Icons.Rounded.ClosedCaptionOff
    val ThemeLight: ImageVector = Icons.Rounded.LightMode
    val ThemeDark: ImageVector = Icons.Rounded.DarkMode
    val ThemeAuto: ImageVector = Icons.Rounded.BrightnessAuto
    val Play: ImageVector = Icons.Rounded.PlayArrow
    val Pause: ImageVector = Icons.Rounded.Pause
    val Next: ImageVector = Icons.Rounded.SkipNext
    val Previous: ImageVector = Icons.Rounded.SkipPrevious
    val Replay: ImageVector = Icons.Rounded.Replay
    val Save: ImageVector = Icons.Rounded.DownloadForOffline
    val Saved: ImageVector = Icons.Rounded.DownloadDone
    val Check: ImageVector = Icons.Rounded.Check
    val CloudDone: ImageVector = Icons.Rounded.CloudDone
    val CloudOff: ImageVector = Icons.Rounded.CloudOff
    val Delete: ImageVector = Icons.Rounded.DeleteOutline
    val Diagram: ImageVector = Icons.Rounded.Hub
    val Error: ImageVector = Icons.Rounded.ErrorOutline
    val Segments: ImageVector = Icons.Rounded.Layers
    val Clock: ImageVector = Icons.Rounded.Schedule
}
