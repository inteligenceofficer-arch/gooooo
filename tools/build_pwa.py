"""Build the installable PWA in docs/ from app/index.html (the single source of the quiz).

Run after editing app/index.html:  python3 tools/build_pwa.py
Bumping the cache version is automatic (hash of the page), so installed apps update themselves.
"""
import hashlib, pathlib, re

root = pathlib.Path(__file__).resolve().parent.parent
src = (root / "app" / "index.html").read_text(encoding="utf-8")
src = src.replace('<meta name="claude:viewport-fit-sides" content="cover">\n', "")

head = """<!doctype html>
<html lang="ko">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="theme-color" content="#0b6e99">
<meta name="color-scheme" content="light dark">
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-title" content="TOEIC">
<meta name="apple-mobile-web-app-status-bar-style" content="default">
<link rel="manifest" href="manifest.webmanifest">
<link rel="icon" href="icons/icon-192.png">
<link rel="apple-touch-icon" href="icons/icon-192.png">
"""
# the artifact skeleton supplies the reset below; a standalone page needs it itself
reset = "<style>html{padding-top:env(safe-area-inset-top,0px);padding-bottom:env(safe-area-inset-bottom,0px)}body{margin:0}[hidden]{display:none!important}</style>\n"
title = re.search(r"<title>.*?</title>\n", src).group(0)
src = src.replace(title, "")
body = src
sw_reg = """<script>
if ('serviceWorker' in navigator) {
  addEventListener('load', () => navigator.serviceWorker.register('sw.js').catch(() => {}));
}
</script>
"""
page = head + title + reset + "</head>\n<body>\n" + body + sw_reg + "</body>\n</html>\n"
out = root / "docs"
(out / "index.html").write_text(page, encoding="utf-8")

ver = hashlib.sha1(page.encode()).hexdigest()[:10]
sw = (root / "tools" / "sw.template.js").read_text().replace("__VERSION__", ver)
(out / "sw.js").write_text(sw)
print("built docs/ — cache version", ver)
