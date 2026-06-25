package com.tutorai.app.ui.player

/**
 * Wrap raw SVG markup in an HTML page exposing two JS functions the player calls:
 *   highlight(ids)   — glow + slightly scale the elements with those ids
 *   clearHighlight() — remove all highlights
 *
 * Highlighting by drop-shadow + scale works on any SVG shape without altering its
 * geometry, and dims nothing so the diagram stays readable.
 */
fun buildSvgHtml(svg: String): String {
    // Android WebView does NOT give an inline <svg> an intrinsic height, so
    // height:auto / height:100% both resolve to zero and the diagram never paints
    // (desktop Chrome derives the height from the viewBox, which is why it looked
    // fine there). Reserve the height from the element's definite width using the
    // viewBox aspect ratio (the padding-bottom trick) and absolutely-position the
    // SVG to fill that box — no reliance on intrinsic SVG sizing at all.
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
  html, body { margin: 0; padding: 0; background: #ffffff; }
  #frame { position: relative; width: 100%; height: 0; padding-bottom: ${ratioPct}%; }
  #frame svg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
  .tutor-hl {
    filter: drop-shadow(0 0 4px #ff9800) drop-shadow(0 0 10px #ffb74d);
    transform: scale(1.04);
    transform-box: fill-box;
    transform-origin: center;
    transition: filter .2s ease, transform .2s ease;
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
