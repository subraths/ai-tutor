package com.tutorai.app.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tutorai.app.domain.model.Lesson
import com.tutorai.app.ui.components.DiagramWebView
import com.tutorai.app.ui.components.SegmentedProgress
import com.tutorai.app.ui.components.TutorIcons
import com.tutorai.app.ui.theme.LocalSpacing
import com.tutorai.app.ui.theme.ScholarShapeTokens
import kotlinx.coroutines.delay

/**
 * Player / Explanation. Diagram-as-hero with the WebView highlight bridge,
 * subtitle-style captions, a segment scrubber, and a large play control.
 *
 * Adaptive: portrait stacks (diagram → scrubber → transport); landscape splits
 * into diagram | controls. The diagram has two overlay controls (top-right):
 *   • caption toggle — captions are OFF by default; tap to show/hide subtitles.
 *   • fullscreen toggle — locks to landscape and hides every chrome (top bar,
 *     scrubber, transport) for a distraction-free, full-bleed diagram.
 */
@Composable
fun PlayerScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val savedOffline by viewModel.savedOffline.collectAsState()

    when (val s = state) {
        is PlayerUiState.Loading -> CenteredStatus(text = "Loading lesson…", showSpinner = true, onBack = onBack)
        is PlayerUiState.Error -> CenteredStatus(text = s.message, showSpinner = false, onBack = onBack, isError = true)
        is PlayerUiState.Ready -> PlayerContent(
            lesson = s.lesson,
            svg = s.svg,
            savedOffline = savedOffline,
            onBack = onBack,
            onSaveOffline = viewModel::saveOffline,
        )
    }
}

@Composable
private fun PlayerContent(
    lesson: Lesson,
    svg: String,
    savedOffline: Boolean,
    onBack: () -> Unit,
    onSaveOffline: () -> Unit,
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var captionsVisible by remember { mutableStateOf(false) }   // captions OFF by default
    var fullscreen by remember { mutableStateOf(false) }

    // Lock to landscape + hide system bars while in fullscreen; restore on exit.
    FullscreenEffect(active = fullscreen)

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItems(lesson.segments.map { MediaItem.fromUri(it.audioUrl) })
            prepare()
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = exoPlayer.currentMediaItemIndex
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Poll position so the scrubber fills the active segment in real time.
    LaunchedEffect(Unit) {
        while (true) {
            positionMs = exoPlayer.currentPosition
            delay(50)
        }
    }

    val index = currentIndex.coerceIn(0, lesson.segments.lastIndex.coerceAtLeast(0))
    val current = lesson.segments.getOrNull(index)
    val clipDurationMs = exoPlayer.duration.takeIf { it > 0 } ?: (current?.durationMs?.toLong() ?: 0L)
    val segmentProgress = if (clipDurationMs > 0) (positionMs.toFloat() / clipDurationMs).coerceIn(0f, 1f) else 0f

    val elapsedMs = lesson.segments.take(index).sumOf { it.durationMs.toLong() } + positionMs
    val totalMs = lesson.totalDurationMs.toLong()

    PlayerBody(
        title = lesson.title,
        svg = svg,
        caption = current?.text.orEmpty(),
        highlightIds = current?.svgElementIds.orEmpty(),
        segmentCount = lesson.segments.size,
        currentIndex = index,
        isPlaying = isPlaying,
        segmentProgress = segmentProgress,
        savedOffline = savedOffline,
        captionsVisible = captionsVisible,
        fullscreen = fullscreen,
        segmentLabel = "Segment ${index + 1}",
        elapsedLabel = formatTime(elapsedMs),
        totalLabel = if (totalMs > 0) formatTime(totalMs) else "",
        onBack = onBack,
        onTogglePlay = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
        onNext = { exoPlayer.seekTo((index + 1).coerceAtMost(lesson.segments.lastIndex), 0) },
        onPrevious = { exoPlayer.seekTo((index - 1).coerceAtLeast(0), 0) },
        onReplay = { exoPlayer.seekTo(0, 0); exoPlayer.play() },
        onSeek = { i -> exoPlayer.seekTo(i, 0) },
        onSaveOffline = onSaveOffline,
        onToggleCaptions = { captionsVisible = !captionsVisible },
        onToggleFullscreen = { fullscreen = !fullscreen },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerBody(
    title: String,
    svg: String,
    caption: String,
    highlightIds: List<String>,
    segmentCount: Int,
    currentIndex: Int,
    isPlaying: Boolean,
    segmentProgress: Float,
    savedOffline: Boolean,
    captionsVisible: Boolean,
    fullscreen: Boolean,
    segmentLabel: String,
    elapsedLabel: String,
    totalLabel: String,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReplay: () -> Unit,
    onSeek: (Int) -> Unit,
    onSaveOffline: () -> Unit,
    onToggleCaptions: () -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    // Fullscreen: diagram only, every other control hidden.
    if (fullscreen) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            DiagramHero(
                svg = svg,
                caption = caption,
                highlightIds = highlightIds,
                captionsVisible = captionsVisible,
                fullscreen = true,
                onToggleCaptions = onToggleCaptions,
                onToggleFullscreen = onToggleFullscreen,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(TutorIcons.Back, contentDescription = "Back") }
                },
                title = {
                    Column {
                        Text(
                            "NOW EXPLAINING",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = MaterialTheme.typography.headlineSmall.fontFamily,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    SegmentCounter(current = currentIndex + 1, total = segmentCount)
                    Spacer(Modifier.width(LocalSpacing.current.s))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { inner ->
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(inner)) {
                DiagramHero(
                    svg = svg,
                    caption = caption,
                    highlightIds = highlightIds,
                    captionsVisible = captionsVisible,
                    fullscreen = false,
                    onToggleCaptions = onToggleCaptions,
                    onToggleFullscreen = onToggleFullscreen,
                    modifier = Modifier.weight(1.5f).fillMaxHeight().padding(start = 14.dp, bottom = 14.dp),
                )
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    ProgressBlock(segmentCount, currentIndex, segmentProgress, segmentLabel, elapsedLabel, totalLabel, onSeek)
                    Spacer(Modifier.height(18.dp))
                    TransportControls(isPlaying, savedOffline, onTogglePlay, onNext, onPrevious, onReplay, onSaveOffline)
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(inner)) {
                DiagramHero(
                    svg = svg,
                    caption = caption,
                    highlightIds = highlightIds,
                    captionsVisible = captionsVisible,
                    fullscreen = false,
                    onToggleCaptions = onToggleCaptions,
                    onToggleFullscreen = onToggleFullscreen,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
                )
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    ProgressBlock(segmentCount, currentIndex, segmentProgress, segmentLabel, elapsedLabel, totalLabel, onSeek)
                }
                TransportControls(
                    isPlaying, savedOffline, onTogglePlay, onNext, onPrevious, onReplay, onSaveOffline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SegmentCounter(current: Int, total: Int) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            "$current / $total",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}

/** Diagram surface (WebView) + overlay controls + optional caption subtitle. */
@Composable
private fun DiagramHero(
    svg: String,
    caption: String,
    highlightIds: List<String>,
    captionsVisible: Boolean,
    fullscreen: Boolean,
    onToggleCaptions: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        DiagramWebView(
            svg = svg,
            highlightIds = highlightIds,
            modifier = Modifier.fillMaxSize(),
        )

        // Cross-fading subtitle — only when captions are enabled.
        if (captionsVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(ScholarShapeTokens.DiagramSurface)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.55f),
                        ),
                    )
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Crossfade(targetState = caption, label = "caption") { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Caption: $text" },
                    )
                }
            }
        }

        // Top-right overlay: caption toggle + fullscreen toggle.
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DiagramOverlayButton(
                icon = if (captionsVisible) TutorIcons.Caption else TutorIcons.CaptionOff,
                desc = if (captionsVisible) "Hide captions" else "Show captions",
                onClick = onToggleCaptions,
            )
            DiagramOverlayButton(
                icon = if (fullscreen) TutorIcons.FullscreenExit else TutorIcons.Fullscreen,
                desc = if (fullscreen) "Exit fullscreen" else "Fullscreen",
                onClick = onToggleFullscreen,
            )
        }
    }
}

/** Small circular scrim button, legible over any diagram. */
@Composable
private fun DiagramOverlayButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.36f),
        contentColor = Color.White,
        modifier = Modifier
            .size(40.dp)
            .semantics { contentDescription = desc },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ProgressBlock(
    segmentCount: Int,
    currentIndex: Int,
    segmentProgress: Float,
    segmentLabel: String,
    elapsedLabel: String,
    totalLabel: String,
    onSeek: (Int) -> Unit,
) {
    SegmentedProgress(
        segmentCount = segmentCount,
        currentIndex = currentIndex,
        segmentProgress = segmentProgress,
        onSeek = onSeek,
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(segmentLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (totalLabel.isNotEmpty()) "$elapsedLabel / $totalLabel" else "",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    savedOffline: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReplay: () -> Unit,
    onSaveOffline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleControl(icon = TutorIcons.Replay, desc = "Replay from start", onClick = onReplay, size = 48.dp, tonal = false)
        CircleControl(icon = TutorIcons.Previous, desc = "Previous segment", onClick = onPrevious, size = 54.dp, tonal = true)
        PlayPauseFab(isPlaying = isPlaying, onClick = onTogglePlay)
        CircleControl(icon = TutorIcons.Next, desc = "Next segment", onClick = onNext, size = 54.dp, tonal = true)
        CircleControl(
            icon = if (savedOffline) TutorIcons.Saved else TutorIcons.Save,
            desc = if (savedOffline) "Saved offline" else "Save offline",
            onClick = onSaveOffline, size = 48.dp, tonal = false,
        )
    }
}

@Composable
private fun PlayPauseFab(isPlaying: Boolean, onClick: () -> Unit) {
    val spacing = LocalSpacing.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 8.dp,
        modifier = Modifier
            .size(spacing.playFab)
            .semantics { contentDescription = if (isPlaying) "Pause" else "Play" },
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                label = "playpause",
            ) { playing ->
                Icon(
                    imageVector = if (playing) TutorIcons.Pause else TutorIcons.Play,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
    }
}

@Composable
private fun CircleControl(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit,
    size: Dp,
    tonal: Boolean,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (tonal) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
        contentColor = if (tonal) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(size)
            .semantics { contentDescription = desc },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(size * 0.46f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CenteredStatus(
    text: String,
    showSpinner: Boolean,
    onBack: () -> Unit,
    isError: Boolean = false,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(TutorIcons.Back, contentDescription = "Back") }
                },
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (showSpinner) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
            } else if (isError) {
                Icon(
                    TutorIcons.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.height(16.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isError) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * While [active], lock the activity to landscape and hide the system bars for an
 * immersive diagram. Restores the previous orientation and bars when fullscreen
 * is dismissed or the player leaves composition. The activity's `configChanges`
 * means the rotation doesn't recreate it, so playback continues uninterrupted.
 */
@Composable
private fun FullscreenEffect(active: Boolean) {
    val context = LocalContext.current
    DisposableEffect(active) {
        val activity = context.findActivity()
        val window = activity?.window
        if (active && activity != null && window != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (active && activity != null && window != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
