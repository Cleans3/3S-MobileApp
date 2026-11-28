"""Puts the two recordings (app as first cloned, app now) side by side, with the frame statistics of each pass.

usage: python video_compare.py <video dir> [--before BASE] [--after NOW] [--name compare] [--title <text>]

Reads <video dir>/<label>/effects.mp4 and gfxinfo.txt (written by record_effects.ps1) and writes, in <video dir>:
  compare.html      two <video> elements that start together, with play, pause and speed controls, and the table below
  gfx-summary.md    the table: frames drawn in the recorded pass, janky frames, percentiles of the frame time, missed deadlines
No video encoder is needed: a browser plays both files at once.
The numbers are those of `dumpsys gfxinfo <package>` for the recorded pass only (the test resets the counters when the recording starts).
Both passes are made on the same emulator, the same window size, the same debug-signed build type, after a warm-up pass, with the same
kind of sequence (scroll, open a book and go back, switch screens); the screens themselves differ, because the UI differs.
"""
import os
import re
import sys

FIELDS = [
    ('Total frames rendered', r'Total frames rendered:\s*(\d+)'),
    ('Janky frames', r'Janky frames:\s*(\d+)'),
    ('Janky frames (%)', r'Janky frames:\s*\d+\s*\(([\d.]+)%\)'),
    ('Janky frames, legacy', r'Janky frames \(legacy\):\s*(\d+)'),
    ('50th percentile (ms)', r'50th percentile:\s*(\d+)ms'),
    ('90th percentile (ms)', r'90th percentile:\s*(\d+)ms'),
    ('95th percentile (ms)', r'95th percentile:\s*(\d+)ms'),
    ('99th percentile (ms)', r'99th percentile:\s*(\d+)ms'),
    ('Missed Vsync', r'Number Missed Vsync:\s*(\d+)'),
    ('High input latency', r'Number High input latency:\s*(\d+)'),
    ('Slow UI thread', r'Number Slow UI thread:\s*(\d+)'),
    ('Slow bitmap uploads', r'Number Slow bitmap uploads:\s*(\d+)'),
    ('Slow issue draw commands', r'Number Slow issue draw commands:\s*(\d+)'),
    ('Frame deadline missed', r'Number Frame deadline missed:\s*(\d+)'),
]


def read(folder, label):
    p = os.path.join(folder, label, 'gfxinfo.txt')
    if not os.path.exists(p):
        return {}
    txt = open(p, encoding='utf-8', errors='replace').read()
    out = {}
    for name, pat in FIELDS:
        m = re.search(pat, txt)
        if m:
            out[name] = m.group(1)
    return out


def main():
    args = sys.argv[1:]
    before, after = 'BASE', 'NOW'
    out_name, title = 'compare', 'The app as first cloned and now, the same kind of sequence'
    if '--name' in args:
        i = args.index('--name'); out_name = args[i + 1]; del args[i:i + 2]
    if '--title' in args:
        i = args.index('--title'); title = args[i + 1]; del args[i:i + 2]
    if '--before' in args:
        i = args.index('--before'); before = args[i + 1]; del args[i:i + 2]
    if '--after' in args:
        i = args.index('--after'); after = args[i + 1]; del args[i:i + 2]
    if len(args) != 1:
        raise SystemExit(__doc__)
    folder = args[0]
    a, b = read(folder, before), read(folder, after)
    rows = ['| Frame statistics of the recorded pass | First clone (%s) | Now (%s) |' % (before, after), '|---|---|---|']
    for name, _ in FIELDS:
        if name in a or name in b:
            rows.append('| %s | %s | %s |' % (name, a.get(name, 'n/a'), b.get(name, 'n/a')))
    table = '\n'.join(rows)
    open(os.path.join(folder, ('gfx-summary' if out_name == 'compare' else out_name + '-gfx') + '.md'), 'w', encoding='utf-8').write(table + '\n')
    html_rows = ''.join('<tr>%s</tr>' % ''.join('<td>%s</td>' % c.strip() for c in r.strip('|').split('|')) for r in rows[2:])
    page = """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>ShelfTap: the app as first cloned and now, side by side</title>
<style>
body{font:15px/1.5 system-ui,sans-serif;margin:0;padding:16px;background:#1c1b1a;color:#f2eee9}
h1{font-size:20px;margin:0 0 4px}p{margin:4px 0 12px;max-width:70ch;color:#d3cbc1}
.pair{display:flex;gap:16px;flex-wrap:wrap;align-items:flex-start}
figure{margin:0}figcaption{margin:0 0 6px;font-weight:600}
video{height:76vh;max-height:900px;border-radius:12px;background:#000}
.bar{margin:12px 0;display:flex;gap:8px;flex-wrap:wrap}
button{font:inherit;padding:8px 16px;border-radius:999px;border:1px solid #6b655e;background:#2a2723;color:#f2eee9;cursor:pointer}
table{border-collapse:collapse;margin-top:16px}td,th{border:1px solid #4a453f;padding:6px 12px;text-align:left}
</style></head><body>
<h1>%s</h1>
<p>Scroll the home, open a book and go back, switch screens. Recorded on the same emulator and window size, debug builds, after a warm-up pass.
The screens differ because the interface differs: the first clone has a five-tab bar and no menu.</p>
<div class="bar"><button id="play">Play both</button><button id="pause">Pause</button><button id="restart">Restart</button>
<button id="slow">Speed 0.25</button><button id="normal">Speed 1</button></div>
<div class="pair">
<figure><figcaption>Before: first clone</figcaption><video id="a" src="%s/effects.mp4" muted loop playsinline controls></video></figure>
<figure><figcaption>After: now</figcaption><video id="b" src="%s/effects.mp4" muted loop playsinline controls></video></figure>
</div>
<table><thead><tr><th>Frame statistics of the recorded pass</th><th>First clone</th><th>Now</th></tr></thead><tbody>%s</tbody></table>
<script>
var a=document.getElementById('a'),b=document.getElementById('b');
function all(f){[a,b].forEach(f)}
document.getElementById('play').onclick=function(){all(function(v){v.currentTime=Math.min(a.currentTime,b.currentTime);v.play()})};
document.getElementById('pause').onclick=function(){all(function(v){v.pause()})};
document.getElementById('restart').onclick=function(){all(function(v){v.currentTime=0;v.play()})};
document.getElementById('slow').onclick=function(){all(function(v){v.playbackRate=0.25})};
document.getElementById('normal').onclick=function(){all(function(v){v.playbackRate=1})};
</script></body></html>
""" % (title, before, after, html_rows)
    open(os.path.join(folder, out_name + '.html'), 'w', encoding='utf-8').write(page)
    print(table)


if __name__ == '__main__':
    main()
