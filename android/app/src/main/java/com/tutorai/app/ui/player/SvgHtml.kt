package com.tutorai.app.ui.player

/**
 * Wrap raw SVG markup in an HTML page exposing two JS functions the player calls:
 *   highlight(ids)   — glow + slightly scale the elements with those ids
 *   clearHighlight() — remove all highlights
 *
 * Highlighting by drop-shadow + scale works on any SVG shape without altering its
 * geometry, and dims nothing so the diagram stays readable.
 */
fun buildSvgHtml(svg: String): String = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
<style>
  html, body { margin: 0; padding: 0; height: 100%; background: #ffffff; }
  #wrap { display: flex; align-items: center; justify-content: center; height: 100%; padding: 8px; box-sizing: border-box; }
  #wrap svg { max-width: 100%; max-height: 100%; height: auto; }
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
<div id="wrap">$svg</div>
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
