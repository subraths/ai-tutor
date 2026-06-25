package com.tutorai.app.ui.player

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tutorai.app.domain.model.Lesson

@Composable
fun PlayerScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is PlayerUiState.Loading -> CenteredMessage("Loading lesson…")
            is PlayerUiState.Error -> CenteredMessage("Error: ${s.message}")
            is PlayerUiState.Ready -> PlayerContent(s.lesson, s.svg, onBack)
        }
    }
}

@Composable
private fun PlayerContent(lesson: Lesson, svg: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var pageLoaded by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

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

    val html = remember(svg) { buildSvgHtml(svg) }

    // The sync loop: whenever the page is ready or the current segment changes,
    // tell the WebView which SVG ids to highlight.
    LaunchedEffect(pageLoaded, currentIndex) {
        if (pageLoaded) {
            val ids = lesson.segments.getOrNull(currentIndex)?.svgElementIds.orEmpty()
            val arg = ids.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
            webView?.evaluateJavascript("highlight($arg)", null)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("← Back") }
            Text(
                lesson.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            pageLoaded = true
                        }
                    }
                    loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                    webView = this
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )

        Text(
            text = lesson.segments.getOrNull(currentIndex)?.text.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { exoPlayer.seekTo((currentIndex - 1).coerceAtLeast(0), 0) }) {
                Text("Prev")
            }
            Button(onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }) {
                Text(if (isPlaying) "Pause" else "Play")
            }
            Button(onClick = {
                exoPlayer.seekTo((currentIndex + 1).coerceAtMost(lesson.segments.lastIndex), 0)
            }) {
                Text("Next")
            }
            Button(onClick = {
                exoPlayer.seekTo(0, 0)
                exoPlayer.play()
            }) {
                Text("Replay")
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
