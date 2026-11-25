"""The boards of the mockup next to the app, one pair per screen and theme (plan 8.5, step 5c). See README.md in this folder.

usage: python make_compare.py <dir of `node tools/render-static.js` output> <audit run dir with ref/ and refdark/> <out dir>

It splits the boards of <rendered>/base.html into one page each (light and dark), screenshots them with headless Chrome at 390 x 844 @3x,
and pairs each with the app's screenshot of the same screen (the audit configurations `ref` and `refdark`: 390 x 844 dp at 480 dpi):
<out>/<theme>/pair-<screen>.jpg is "board | app".
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

# app screen (the audit's name for it) -> board
PAIRS = {
    'welcome': 'Main', 'splash': 'Splash', 'signedout-token_reused': 'SignedOut', 'login': 'Login', 'login-card': 'CardLogin',
    'guest': 'Guest', 'catalog': 'Catalog', 'list': 'List', 'detail': 'Detail', 'loans': 'Loans', 'checkout': 'Checkout',
    'reservations': 'Reservations', 'history': 'History', 'account': 'Account', 'password-first': 'ChangePassword', 'lock': 'Lock', 'offline': 'Offline',
}

src = open(os.path.join(rendered, 'base.html'), encoding='utf-8').read()
figs = re.findall(r'<figure data-board="([^"]+)\.dc\.html"[^>]*>.*?<iframe srcdoc="(.*?)"[^>]*></iframe>', src, flags=re.S)
boards = {name: html.unescape(doc) for name, doc in figs}
print('boards:', len(boards))

tmp = os.path.join(rendered, 'one')
os.makedirs(tmp, exist_ok=True)
for theme in ('light', 'dark'):
    os.makedirs(os.path.join(out, theme), exist_ok=True)
    for screen, board in PAIRS.items():
        if board not in boards:
            print('no board', board)
            continue
        # the page sits next to fonts/ of the rendered folder, so the Nunito files load
        page = os.path.join(rendered, '%s_%s.html' % (board, theme))
        open(page, 'w', encoding='utf-8').write(re.sub(r'data-theme="[a-z]*"', 'data-theme="%s"' % theme, boards[board]))
        png = os.path.join(tmp, '%s_%s.png' % (board, theme))
        subprocess.run([chrome, '--headless=new', '--disable-gpu', '--hide-scrollbars', '--window-size=390,844', '--force-device-scale-factor=3',
                        '--virtual-time-budget=5000', '--screenshot=' + png, 'file:///' + page.replace('\\', '/')], capture_output=True, timeout=120)
        cfg = 'ref' if theme == 'light' else 'refdark'
        a = os.path.join(audit, cfg, '%s-hw.png' % screen)
        if not (os.path.exists(a) and os.path.exists(png)):
            print('missing', theme, screen, os.path.exists(a), os.path.exists(png))
            continue
        ia = Image.open(a).convert('RGB').resize((585, 1266))
        ib = Image.open(png).convert('RGB').resize((585, 1266))
        w = Image.new('RGB', (585 * 2 + 30, 1266 + 40), (120, 120, 120))
        w.paste(ib, (0, 40))
        w.paste(ia, (615, 40))
        d = ImageDraw.Draw(w)
        d.text((8, 12), 'board %s (%s)' % (board, theme), fill=(255, 255, 255))
        d.text((623, 12), 'app %s (%s)' % (screen, theme), fill=(255, 255, 255))
        w.save(os.path.join(out, theme, 'pair-%s.jpg' % screen), quality=82)
print('done')
