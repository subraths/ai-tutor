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
    // Always route through highlight(): it both highlights and zooms the viewBox to
    // the target section (empty list → zoom back out to the whole diagram).
    val arg = JSONArray(ids).toString()       // safe JSON array, e.g. ["sun","ray1"]
    evaluateJavascript("highlight($arg);", null)
}

private fun colorHex(argb: Int): String = String.format("#%06X", 0xFFFFFF and argb)

/**
 * Wrap raw SVG markup in a self-contained HTML page exposing `highlight(ids)` /
 * `clearHighlight()`, plus a viewBox-driven "zoom to the highlighted section".
 *
 * The SVG element is sized in JS to the WebView's ACTUAL pixel width/height
 * (`window.innerWidth/innerHeight`) — so it adapts to each device and orientation
 * rather than to a fixed aspect ratio — and uses `preserveAspectRatio="xMidYMid
 * meet"`, so the WHOLE diagram always fits inside that box: it fills nicely in
 * landscape and fits (zoomed out) in portrait, never clipped. Explicit pixel
 * dimensions are used because Android WebView gives an inline <svg> no intrinsic
 * height (a height:100% chain collapses to zero). Re-measured on resize/rotation.
 *
 * @param glowHex the theme amber so the highlight glow matches light/dark.
 */
private fun buildSvgHtml(svg: String, glowHex: String): String {
    return """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
<style>
  html, body { margin: 0; padding: 0; background: transparent; }
  #frame svg { display: block; }   /* width/height set in JS to the WebView size */
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
  var svgEl = null, ORIG = null, animId = null, lastIds = [];

  function getSvg() {
    if (!svgEl) svgEl = document.querySelector('#frame svg');
    return svgEl;
  }

  function parseVB(s) {
    if (!s) return null;
    var p = s.trim().split(/[\s,]+/).map(Number);
    return (p.length === 4 && p[2] > 0 && p[3] > 0) ? p : null;
  }

  // Size the SVG element to the WebView's real pixel box (per device/orientation).
  // With preserveAspectRatio="xMidYMid meet" the whole diagram fits inside it.
  function sizeToViewport() {
    var s = getSvg(); if (!s) return;
    var w = window.innerWidth || document.documentElement.clientWidth || 0;
    var h = window.innerHeight || document.documentElement.clientHeight || 0;
    if (w > 0) s.setAttribute('width', w);
    if (h > 0) s.setAttribute('height', h);
  }

  // Capture the diagram's full extent once (its viewBox), then size to the viewport.
  function ensureInit() {
    var s = getSvg();
    if (!s) return;
    if (!ORIG) {
      ORIG = parseVB(s.getAttribute('viewBox'));
      if (!ORIG) {
        try { var b = s.getBBox(); if (b && b.width > 0 && b.height > 0) ORIG = [b.x, b.y, b.width, b.height]; } catch (e) {}
      }
      if (!ORIG) ORIG = [0, 0, parseFloat(s.getAttribute('width')) || 1000, parseFloat(s.getAttribute('height')) || 600];
      s.setAttribute('viewBox', ORIG.join(' '));
      s.setAttribute('preserveAspectRatio', 'xMidYMid meet');
    }
    sizeToViewport();
  }

  // Union bounding box (in SVG user units) of the given elements, via screen CTM
  // so element/group transforms are handled correctly.
  function unionBox(els) {
    var s = getSvg(); if (!s || !s.getScreenCTM) return null;
    var inv = s.getScreenCTM().inverse();
    var minx = Infinity, miny = Infinity, maxx = -Infinity, maxy = -Infinity, found = false;
    els.forEach(function (el) {
      if (!el || !el.getBoundingClientRect) return;
      var r = el.getBoundingClientRect();
      if (!r || (r.width === 0 && r.height === 0)) return;
      [[r.left, r.top], [r.right, r.top], [r.left, r.bottom], [r.right, r.bottom]].forEach(function (c) {
        var pt = s.createSVGPoint(); pt.x = c[0]; pt.y = c[1];
        var u = pt.matrixTransform(inv);
        if (u.x < minx) minx = u.x;
        if (u.y < miny) miny = u.y;
        if (u.x > maxx) maxx = u.x;
        if (u.y > maxy) maxy = u.y;
        found = true;
      });
    });
    return found ? [minx, miny, maxx - minx, maxy - miny] : null;
  }

  function animateVB(target, dur) {
    var s = getSvg(); if (!s || !target) return;
    var start = parseVB(s.getAttribute('viewBox')) || ORIG;
    var t0 = (window.performance && performance.now) ? performance.now() : Date.now();
    if (animId) cancelAnimationFrame(animId);
    function frame(now) {
      var t = Math.min(1, (now - t0) / dur);
      var e = t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t; // easeInOutQuad
      var vb = [0, 1, 2, 3].map(function (i) { return start[i] + (target[i] - start[i]) * e; });
      s.setAttribute('viewBox', vb.join(' '));
      if (t < 1) animId = requestAnimationFrame(frame);
    }
    animId = requestAnimationFrame(frame);
  }

  // Zoom + pan so the highlighted section is centered and fills the WebView.
  function focusOn(ids) {
    ensureInit();
    var s = getSvg(); if (!s || !ORIG) return;
    if (!ids || !ids.length) { animateVB(ORIG, 500); return; }
    var els = ids.map(function (id) { return document.getElementById(id); }).filter(Boolean);
    var box = unionBox(els);
    if (!box) { animateVB(ORIG, 500); return; }

    // breathing room around the section
    var px = box[2] * 0.14, py = box[3] * 0.14;
    box = [box[0] - px, box[1] - py, box[2] + 2 * px, box[3] + 2 * py];

    // don't over-zoom on a tiny target
    var minW = ORIG[2] * 0.25, minH = ORIG[3] * 0.25;
    if (box[2] < minW) { box[0] -= (minW - box[2]) / 2; box[2] = minW; }
    if (box[3] < minH) { box[1] -= (minH - box[3]) / 2; box[3] = minH; }

    // expand to the WebView's aspect ratio (around the section's centre) so the
    // section is centred with no letterboxing
    var rect = s.getBoundingClientRect();
    var aspect = (rect.height > 0) ? rect.width / rect.height : ORIG[2] / ORIG[3];
    var cx = box[0] + box[2] / 2, cy = box[1] + box[3] / 2, w = box[2], h = box[3];
    if (w / h < aspect) w = h * aspect; else h = w / aspect;
    animateVB([cx - w / 2, cy - h / 2, w, h], 500);
  }

  function clearHighlight() {
    document.querySelectorAll('.tutor-hl').forEach(function (e) { e.classList.remove('tutor-hl'); });
  }
  function highlight(ids) {
    lastIds = ids || [];
    clearHighlight();
    lastIds.forEach(function (id) {
      var el = document.getElementById(id);
      if (el) el.classList.add('tutor-hl');
    });
    focusOn(lastIds);
  }

  // Re-measure and re-fit when the WebView resizes (e.g. portrait <-> landscape).
  function relayout() { ensureInit(); focusOn(lastIds); }
  window.addEventListener('resize', relayout);
  window.addEventListener('orientationchange', relayout);
  window.addEventListener('load', relayout);

  ensureInit();
</script>
</body>
</html>
""".trimIndent()
}
