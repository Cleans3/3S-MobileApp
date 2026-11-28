"""The app as first cloned (UI baseline) next to the app now, one pair per screen and theme: "before | after".

usage: python baseline_pairs.py <audit run dir with light/ and dark/> <out dir> [--baseline-commit c4280b8]

Before: the baseline measurement of 2026-11-23 kept in git history (`optimization/ui-ux/audit/A/<theme>/<screen>.png` at the commit that
added it; the app code of that commit's measurement is the tag `ui-baseline`, identical to what was cloned: `git diff af2c9cf ui-baseline
-- android/app/src/main` is empty). Those images were drawn in the process from the view tree (`View.draw`), at 720 x 1280 px, 280 dpi.
After: the same screens from `run_audit.ps1 -Configs light,dark` on the same emulator size, drawn by the GPU renderer (`*-hw.png`).
The two do not have the same screens in the same state: the baseline had a five-tab bar and no menu, so "Danh mục" before is the catalog
with its tab bar and "Danh mục" after is the home with the banner.
"""
import io
import os
import subprocess
import sys

from PIL import Image, ImageDraw

SCREENS = ['catalog', 'detail', 'loans', 'reservations', 'account', 'checkout', 'login']


def git_show(commit, path):
    r = subprocess.run(['git', 'show', '%s:%s' % (commit, path)], capture_output=True)
    return Image.open(io.BytesIO(r.stdout)).convert('RGB') if r.returncode == 0 and r.stdout else None


def main():
    args = sys.argv[1:]
    commit = 'c4280b8'
    if '--baseline-commit' in args:
        i = args.index('--baseline-commit'); commit = args[i + 1]; del args[i:i + 2]
    if len(args) != 2:
        raise SystemExit(__doc__)
    audit, out = args
    os.makedirs(out, exist_ok=True)
    made = []
    for theme in ('light', 'dark'):
        for screen in SCREENS:
            before = git_show(commit, 'optimization/ui-ux/audit/A/%s/%s.png' % (theme, screen))
            path = os.path.join(audit, theme, '%s-hw.png' % screen)
            after = Image.open(path).convert('RGB') if os.path.exists(path) else None
            if before is None or after is None:
                print('missing', theme, screen, 'before' if before is None else '', 'after' if after is None else '')
                continue
            h = 1100
            b = before.resize((round(before.width * h / before.height), h), Image.LANCZOS)
            a = after.resize((round(after.width * h / after.height), h), Image.LANCZOS)
            sheet = Image.new('RGB', (b.width + a.width + 30, h + 40), (120, 120, 120))
            d = ImageDraw.Draw(sheet)
            d.text((8, 12), 'before: first clone (%s, %s)' % (screen, theme), fill=(255, 255, 255))
            d.text((b.width + 38, 12), 'after: now (%s, %s)' % (screen, theme), fill=(255, 255, 255))
            sheet.paste(b, (0, 40)); sheet.paste(a, (b.width + 30, 40))
            name = os.path.join(out, '%s-%s.jpg' % (theme, screen))
            sheet.save(name, quality=82)
            made.append(name)
    print('pairs:', len(made))


if __name__ == '__main__':
    main()
