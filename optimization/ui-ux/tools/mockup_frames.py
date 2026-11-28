"""Frames of a mockup board's animation at exact times, so the mockup's effect can be put next to the app's.

usage: python mockup_frames.py <dir of `node tools/render-static.js` output> <board name, e.g. Detail> <out dir> [times in ms, comma separated]

The page of the board is loaded with every CSS animation paused and set to the given time (`document.getAnimations()`), then screenshot by
headless Chrome at the board's size, 2x. Output: <out dir>/<board>-<ms>-hw.png (the name `frames_to_anim.py` reads).
Needs Chrome (CHROME env var or the default Windows path).
"""
import html
import os
import re
import subprocess
import sys

from PIL import Image

args = sys.argv[1:]
if len(args) < 3:
    raise SystemExit(__doc__)
rendered, board, out = args[:3]
times = [int(x) for x in (args[3] if len(args) > 3 else '0,20,40,60,80,100,130,160,200,260,320').split(',')]
chrome = os.environ.get('CHROME', r'C:\Program Files\Google\Chrome\Application\chrome.exe')
src = open(os.path.join(rendered, 'base.html'), encoding='utf-8').read()
figs = re.findall(r'<figure data-board="([^"]+)\.dc\.html"[^>]*>.*?<iframe srcdoc="(.*?)"[^>]*></iframe>', src, flags=re.S)
boards = {name: html.unescape(doc) for name, doc in figs}
if board not in boards:
    raise SystemExit('no board %s; have: %s' % (board, ', '.join(sorted(boards))))
os.makedirs(out, exist_ok=True)
m = re.search(r'width: (\d+)px; height: (\d+)px', boards[board])
w, h = (int(m.group(1)), int(m.group(2))) if m else (390, 844)
for t in times:
    seek = ('<script>window.addEventListener("load",function(){setTimeout(function(){document.getAnimations().forEach(function(a){a.pause();a.currentTime=%d;});},400);});</script>' % t)
    page = os.path.join(rendered, '%s_t%d.html' % (board, t))
    open(page, 'w', encoding='utf-8').write(re.sub(r'(class="st-root"\s+data-theme=")[a-z]*"', r'\g<1>light"', boards[board]) + seek)
    png = os.path.join(out, '%s-%d-hw.png' % (board, t))
    subprocess.run([chrome, '--headless=new', '--disable-gpu', '--hide-scrollbars', '--window-size=%d,%d' % (w, h), '--force-device-scale-factor=2',
                    '--virtual-time-budget=3000', '--screenshot=' + png, 'file:///' + page.replace('\\', '/')], capture_output=True, timeout=120)
    os.remove(page)
    print(t, 'ms', 'ok' if os.path.exists(png) else 'missing')
