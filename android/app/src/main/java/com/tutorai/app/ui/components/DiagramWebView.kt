package com.tutorai.app.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tutorai.app.ui.theme.ScholarShapeTokens
import com.tutorai.app.ui.theme.extended
import android.graphics.Color as AndroidColor
import org.json.JSONArray

/**
 * The diagram. The WebView + JS highlight bridge is preserved exactly — this
 * composable restyles the container (rounded surface, hairline border) and
 * forwards highlight ids to the existing JS API.
 *
 * Contract with the page: it defines `highlight(ids)` and `clearHighlight()` in
 * JS; we call them via [WebView.evaluateJavascript]. Sync usage in the player:
 *   DiagramWebView(svg = lesson.svg, highlightIds = currentSegment.svgElementIds)
 * driven from ExoPlayer's currentMediaItemIndex.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DiagramWebView(
    svg: String,
    highlightIds: List<String>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val glowHex = colorHex(MaterialTheme.extended.spotlightGlow.toArgb())
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    var loaded by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.domStorageEnabled = true
            setBackgroundColor(AndroidColor.TRANSPARENT)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    loaded = true
                }
            }
        }
    }

    // Re-load only when the SVG markup (or glow) itself changes.
    LaunchedEffect(svg, glowHex) {
        loaded = false
        // A non-null https base URL renders SVG more reliably than null in WebView.
        webView.loadDataWithBaseURL(
            "https://appassets.androidplatform.net/",
            buildSvgHtml(svg, glowHex),
            "text/html",
            "utf-8",
            null,
        )
    }

    // Apply the current highlight once the page is ready, and on every change.
    LaunchedEffect(loaded, highlightIds) {
        if (loaded) webView.postHighlight(highlightIds)
    }

    DisposableEffect(Unit) {
        onDispose { webView.destroy() }
    }

    Box(
        modifier = modifier
            .clip(ScholarShapeTokens.DiagramSurface)
            .background(surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ScholarShapeTokens.DiagramSurface),
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize().padding(2.dp),
        )
    }
}

private fun WebView.postHighlight(ids: List<String>) {
    val arg = JSONArray(ids).toString()       // safe JSON array, e.g. ["sun","ray1"]
    val js = if (ids.isEmpty()) "clearHighlight();" else "highlight($arg);"
    evaluateJavascript(js, null)
}

private fun colorHex(argb: Int): String = String.format("#%06X", 0xFFFFFF and argb)

/**
 * Wrap raw SVG markup in a self-contained HTML page exposing `highlight(ids)` /
 * `clearHighlight()`.
 *
 * Android WebView does NOT give an inline `<svg>` an intrinsic height, so
 * height:auto/100% resolve to zero and the diagram never paints (desktop Chrome
 * derives the height from the viewBox, which is why it looks fine there). We
 * reserve the height from the element's definite width using the viewBox aspect
 * ratio (the padding-bottom trick) and absolutely-position the SVG to fill that
 * box — no reliance on intrinsic SVG sizing at all.
 *
 * @param glowHex the theme amber so the highlight glow matches light/dark.
 */
private fun buildSvgHtml(svg: String, glowHex: String): String {
    val ratioPct = Regex(
        """viewBox\s*=\s*["']\s*[-\d.]+\s+[-\d.]+\s+([-\d.]+)\s+([-\d.]+)""",
    ).find(svg)?.let { m ->
        val w = m.groupValues[1].toFloatOrNull()
        val h = m.groupValues[2].toFloatOrNull()
        if (w != null && h != null && w > 0f) (h / w * 100f) else null
    } ?: 60f // sensible default (1000x600) when no viewBox is present

    return """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
<style>
  html, body { margin: 0; padding: 0; background: transparent; }
  #frame { position: relative; width: 100%; height: 0; padding-bottom: ${ratioPct}%; }
  #frame svg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
  .tutor-hl {
    filter: drop-shadow(0 0 5px $glowHex) drop-shadow(0 0 12px $glowHex);
    transform: scale(1.04);
    transform-box: fill-box;
    transform-origin: center;
    transition: filter .4s ease, transform .4s ease;
  }
</style>
</head>
<body>
<div id="frame">$svg</div>
<script>
  function clearHighlight() {
    document.querySelectorAll('.tutor-hl').forEach(function (e) { e.classList.remove('tutor-hl'); });
  }
  function highlight(ids) {
    clearHighlight();
    (ids || []).forEach(function (id) {
      var el = document.getElementById(id);
      if (el) el.classList.add('tutor-hl');
    });
  }
</script>
</body>
</html>
""".trimIndent()
}
