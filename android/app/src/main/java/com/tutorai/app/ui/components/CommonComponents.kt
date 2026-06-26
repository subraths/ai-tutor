package com.tutorai.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tutorai.app.ui.theme.ScholarShapeTokens

/** Small clay eyebrow above section/headlines — part of the editorial voice. */
@Composable
fun SectionEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier,
    )
}

/** Outlined suggestion chip with a clay dot. */
@Composable
fun SuggestionChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.heightIn(min = 40.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
            )
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Small "Offline" badge used on saved lessons. */
@Composable
fun OfflineBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        ) {
            Icon(TutorIcons.CloudOff, contentDescription = null, modifier = Modifier.size(14.dp))
            Text("Offline", style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Lesson diagram thumbnail.
 *
 * Uses a branded placeholder tile, NOT a live WebView — rendering N WebViews in
 * a scrolling list is expensive. For real thumbnails, raster the SVG to a Bitmap
 * once when the lesson is saved and show it here with `Image(...)`.
 */
@Composable
fun DiagramThumbnail(modifier: Modifier = Modifier, icon: ImageVector = TutorIcons.Diagram) {
    Box(
        modifier = modifier
            .clip(ScholarShapeTokens.Thumbnail)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ),
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ScholarShapeTokens.Thumbnail),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
    }
}
