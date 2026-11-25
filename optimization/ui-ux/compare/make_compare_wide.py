"""The window-size boards (Mid 720 x 840, Wide 960 x 720) next to the app, one pair per screen (plan 8.5, step 5c; the phone boards are in make_compare.py).

usage: python make_compare_wide.py <dir of `node tools/render-static.js` output> <audit run dir with medium/ and expanded/> <out dir>

The boards are screenshotted with headless Chrome at their own size (light theme) and paired with the app's screenshot of the same screen from the
audit configurations `medium` (1260 x 1470 px at 280 dpi = 720 x 840 dp) and `expanded` (1680 x 1260 px = 960 x 720 dp):
<out>/<size>/pair-<screen>.jpg is "board | app", both scaled to the same size.
"""
import html
import os
import re
import subprocess
import sys

from PIL import Image, ImageDraw

if len(sys.argv) != 4:
    raise SystemExit(__doc__)
rendered, audit, out = sys.argv[1:4]
chrome = os.environ.get('CHROME', r'C:\Program Files\Google\Chrome\Application\chrome.exe')

# size -> (audit configuration, board width x height in dp, {app screen (the audit's name): board})
SIZES = {
    'mid': ('medium', (720, 840), {
        'catalog': 'RailCatalog', 'list': 'MidCatalog', 'guest': 'MidGuest', 'loans': 'MidLoans', 'reservations': 'MidReservations',
        'history': 'MidHistory', 'checkout': 'MidCheckout', 'account': 'MidAccount', 'login': 'MidSignIn',
    }),
    'wide': ('expanded', (960, 720), {
        'catalog': 'WideCatalog', 'guest': 'WideGuest', 'loans': 'WideLoans', 'reservations': 'WideReservations',
        'history': 'WideHistory', 'checkout': 'WideCheckout', 'account': 'WideAccount', 'login': 'WideSignIn',
    }),
}

src = open(os.path.join(rendered, 'base.html'), encoding='utf-8').read()
figs = re.findall(r'<figure data-board="([^"]+)\.dc\.html"[^>]*>.*?<iframe srcdoc="(.*?)"[^>]*></iframe>', src, flags=re.S)
boards = {name: html.unescape(doc) for name, doc in figs}
print('boards:', len(boards))

tmp = os.path.join(rendered, 'one')
os.makedirs(tmp, exist_ok=True)
for size, (cfg, (bw, bh), pairs) in SIZES.items():
    os.makedirs(os.path.join(out, size), exist_ok=True)
    for screen, board in pairs.items():
        if board not in boards:
            print('no board', board)
            continue
        page = os.path.join(rendered, '%s_light.html' % board)
        open(page, 'w', encoding='utf-8').write(re.sub(r'(class="st-root"\s+data-theme=")[a-z]*"', r'\g<1>light"', boards[board]))
        png = os.path.join(tmp, '%s_light.png' % board)
        subprocess.run([chrome, '--headless=new', '--disable-gpu', '--hide-scrollbars', '--window-size=%d,%d' % (bw, bh), '--force-device-scale-factor=2',
                        '--blink-settings=preferredColorScheme=0', '--virtual-time-budget=5000', '--screenshot=' + png, 'file:///' + page.replace('\\', '/')], capture_output=True, timeout=120)
        a = os.path.join(audit, cfg, '%s-hw.png' % screen)
        if not (os.path.exists(a) and os.path.exists(png)):
            print('missing', size, screen, os.path.exists(a), os.path.exists(png))
            continue
        k = 0.5 if bw > 800 else 0.6
        sz = (int(bw * 2 * k), int(bh * 2 * k))
        ia = Image.open(a).convert('RGB').resize(sz)
        ib = Image.open(png).convert('RGB').resize(sz)
        w = Image.new('RGB', (sz[0] * 2 + 30, sz[1] + 40), (120, 120, 120))
        w.paste(ib, (0, 40))
        w.paste(ia, (sz[0] + 30, 40))
        d = ImageDraw.Draw(w)
        d.text((8, 12), 'board %s' % board, fill=(255, 255, 255))
        d.text((sz[0] + 38, 12), 'app %s (%s)' % (screen, cfg), fill=(255, 255, 255))
        w.save(os.path.join(out, size, 'pair-%s.jpg' % screen), quality=82)
print('done')
