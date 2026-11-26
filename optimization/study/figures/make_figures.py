#!/usr/bin/env python3
"""figures/make_figures.py: stdlib-only SVG generator for F01-F21 (figures/PLAN.md).
Reads only committed CSV/MD files under optimization/study/; writes figures/out/F##-name.svg and .data.csv,
plus figures/out/manifest.json (source sha256, git commit, cross-checks, skipped figures).
Run: python3 -I figures/make_figures.py   (no third-party packages)."""
import csv, hashlib, json, math, os, re, subprocess
from collections import Counter, defaultdict
from math import comb

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
OUT = os.path.join(HERE, 'out')
os.makedirs(OUT, exist_ok=True)
E1 = os.path.join(ROOT, 'e1-corpus', 'results'); E2 = os.path.join(ROOT, 'e2-bench', 'results')
E3 = os.path.join(ROOT, 'e3-security', 'results'); E4 = os.path.join(ROOT, 'e4-pressure', 'results')
E5 = os.path.join(ROOT, 'e5-optimizer', 'results'); E5G = os.path.join(ROOT, 'e5-optimizer')
VAL = os.path.join(E1, 'validation')
MANI = {'script': 'figures/make_figures.py', 'figures': {}, 'skipped': {}, 'checks': []}
CUR = []

def rd(path):
    CUR.append(os.path.relpath(path, ROOT))
    with open(path, newline='', encoding='utf-8') as f:
        return list(csv.DictReader(f))
def rdtxt(path):
    CUR.append(os.path.relpath(path, ROOT))
    with open(path, encoding='utf-8') as f:
        return f.read()
def sha(path):
    return hashlib.sha256(open(path, 'rb').read()).hexdigest()
def vi(x, d=2):
    return ('%.*f' % (d, x)).replace('.', ',').replace('-', '−')
def vis(x, d=2):
    return ('+' if x > 0 else '') + vi(x, d)
def check(name, got, want):
    ok = (got == want)
    MANI['checks'].append({'check': name, 'got': got, 'want': want, 'ok': ok})
    if not ok:
        print('CHECK FAIL', name, got, want)
def wilson(k, n, z=1.96):
    if n == 0: return (0.0, 1.0)
    p = k / n; d = 1 + z * z / n; c = (p + z * z / (2 * n)) / d
    h = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / d
    return (max(0.0, c - h), min(1.0, c + h))
def median(v):
    s = sorted(v); n = len(s)
    return s[n // 2] if n % 2 else (s[n // 2 - 1] + s[n // 2]) / 2
def esc(t):
    return str(t).replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('"', '&quot;')

STYLES = ['naive', 'detailed-functional', 'guardrailed']
STY = {'naive': '#E69F00', 'detailed-functional': '#56B4E9', 'guardrailed': '#009E73'}
STYM = {'naive': 'circle', 'detailed-functional': 'square', 'guardrailed': 'triangle'}
AC, BC, HP, HQ = '#D55E00', '#0072B2', '#8C8C8C', '#3F3F3F'
INK, MUTE, GRID = '#222222', '#666666', '#D9D9D9'
OUTC = {'secure-fix': '#009E73', 'insecure-workaround': '#D55E00', 'perf-regression': '#CC79A7', 'no-fix': '#999999', 'n/a': '#DDDDDD'}

class Fig:
    def __init__(s, w, h, title, sub=None):
        s.w, s.h, s.title, s.sub, s.p = w, h, title, sub, []
        s.rect(0, 0, w, h, '#fff')
        s.text(14, 22, title, 15, weight='bold')
        if sub: s.text(14, 40, sub, 11, fill=MUTE)
    def text(s, x, y, t, size=11, anchor='start', weight='normal', fill=INK, rot=0):
        tr = ' transform="rotate(%d %.1f %.1f)"' % (rot, x, y) if rot else ''
        s.p.append('<text x="%.1f" y="%.1f" font-size="%s" text-anchor="%s" font-weight="%s" fill="%s"%s>%s</text>' % (x, y, size, anchor, weight, fill, tr, esc(t)))
    def line(s, x1, y1, x2, y2, stroke='#333', w=1, dash=None):
        d = ' stroke-dasharray="%s"' % dash if dash else ''
        s.p.append('<line x1="%.1f" y1="%.1f" x2="%.1f" y2="%.1f" stroke="%s" stroke-width="%s"%s/>' % (x1, y1, x2, y2, stroke, w, d))
    def rect(s, x, y, w, h, fill, stroke='none', op=1, sw=1):
        s.p.append('<rect x="%.1f" y="%.1f" width="%.1f" height="%.1f" fill="%s" stroke="%s" stroke-width="%s" opacity="%s"/>' % (x, y, max(w, 0), max(h, 0), fill, stroke, sw, op))
    def mark(s, x, y, shape='circle', color='#000', r=5, hollow=False, op=1):
        f = 'none' if hollow else color
        a = 'fill="%s" stroke="%s" stroke-width="1.6" opacity="%s"' % (f, color, op)
        if shape == 'circle': s.p.append('<circle cx="%.1f" cy="%.1f" r="%s" %s/>' % (x, y, r, a))
        elif shape == 'square': s.p.append('<rect x="%.1f" y="%.1f" width="%s" height="%s" %s/>' % (x - r, y - r, 2 * r, 2 * r, a))
        elif shape == 'triangle': s.p.append('<polygon points="%.1f,%.1f %.1f,%.1f %.1f,%.1f" %s/>' % (x, y - r - 1, x - r - 1, y + r, x + r + 1, y + r, a))
        elif shape == 'diamond': s.p.append('<polygon points="%.1f,%.1f %.1f,%.1f %.1f,%.1f %.1f,%.1f" %s/>' % (x, y - r - 1, x + r + 1, y, x, y + r + 1, x - r - 1, y, a))
        elif shape == 'cross':
            s.line(x - r, y - r, x + r, y + r, color, 2); s.line(x - r, y + r, x + r, y - r, color, 2)
    def save(s, name, desc=''):
        head = '<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d" font-family="Helvetica, Arial, sans-serif" role="img" aria-label="%s"><title>%s</title><desc>%s</desc>' % (s.w, s.h, s.w, s.h, esc(s.title), esc(s.title), esc(desc))
        with open(os.path.join(OUT, name + '.svg'), 'w', encoding='utf-8') as f:
            f.write(head + '\n'.join(s.p) + '</svg>\n')

class Ax:
    """linear or log axis box. x/y domains (lo, hi)."""
    def __init__(s, f, x, y, w, h, xd=None, yd=None, xlog=False, ylog=False):
        s.f, s.x, s.y, s.w, s.h, s.xd, s.yd, s.xlog, s.ylog = f, x, y, w, h, xd, yd, xlog, ylog
    def X(s, v):
        lo, hi = s.xd
        t = (math.log10(v) - math.log10(lo)) / (math.log10(hi) - math.log10(lo)) if s.xlog else (v - lo) / (hi - lo)
        return s.x + t * s.w
    def Y(s, v):
        lo, hi = s.yd
        t = (math.log10(v) - math.log10(lo)) / (math.log10(hi) - math.log10(lo)) if s.ylog else (v - lo) / (hi - lo)
        return s.y + s.h - t * s.h
    def xaxis(s, ticks, label='', fmt=lambda v: vi(v, 2), grid=True):
        f = s.f; f.line(s.x, s.y + s.h, s.x + s.w, s.y + s.h, '#555')
        for t in ticks:
            xx = s.X(t)
            if grid: f.line(xx, s.y, xx, s.y + s.h, GRID, 1)
            f.line(xx, s.y + s.h, xx, s.y + s.h + 4, '#555'); f.text(xx, s.y + s.h + 17, fmt(t), 10, 'middle', fill=MUTE)
        if label: f.text(s.x + s.w / 2, s.y + s.h + 33, label, 11, 'middle')
    def yaxis(s, ticks, label='', fmt=lambda v: vi(v, 2), grid=True):
        f = s.f; f.line(s.x, s.y, s.x, s.y + s.h, '#555')
        for t in ticks:
            yy = s.Y(t)
            if grid: f.line(s.x, yy, s.x + s.w, yy, GRID, 1)
            f.line(s.x - 4, yy, s.x, yy, '#555'); f.text(s.x - 8, yy + 3, fmt(t), 10, 'end', fill=MUTE)
        if label: f.text(s.x - 44, s.y + s.h / 2, label, 11, 'middle', rot=-90)
    def vline(s, v, color='#333', dash='4 3', label=None, w=1.2, dy=11, anchor='start'):
        xx = s.X(v); s.f.line(xx, s.y, xx, s.y + s.h, color, w, dash)
        if label: s.f.text(xx + (3 if anchor == 'start' else -3), s.y + dy, label, 10, anchor, fill=color)

def emit(name, header, rows):
    with open(os.path.join(OUT, name + '.data.csv'), 'w', newline='', encoding='utf-8') as fh:
        w = csv.writer(fh); w.writerow(header); w.writerows(rows)
    MANI['figures'][name] = {'sources': sorted(set(CUR)), 'rows': len(rows)}
    del CUR[:]
def skip(fid, why):
    MANI['skipped'][fid] = why; print('SKIP', fid, why)
def legend(f, x, y, items, gap=18):
    for i, (lab, col, shape) in enumerate(items):
        f.mark(x, y + i * gap, shape, col, 5); f.text(x + 12, y + i * gap + 4, lab, 11)

# ---------- shared data: E1 primary track (frozen v1 detector, A13) ----------
def e1_load():
    runs = [r for r in rd(os.path.join(E1, 'runs.csv')) if r['pilot'] == 'False' and r['valid'] == 'True']
    rs = rd(os.path.join(VAL, 'rules_status_v1.csv'))
    validated = {r['rule_id'] for r in rs if r['status'] == 'validated'}
    excluded = {r['rule_id'] for r in rs if r['status'] != 'validated'}
    primary = validated | {'D02', 'S11'}
    ids = {r['run_id'] for r in runs}
    fnd = [x for x in rd(os.path.join(VAL, 'v1', 'pre_fix_findings.csv')) if x['run_id'] in ids]
    rules_by_run = defaultdict(set); cnt = Counter()
    for x in fnd:
        rules_by_run[x['run_id']].add(x['rule_id'])
        cnt[(x['run_id'], x['rule_id'])] += 1
    met = {m['run_id']: m for m in rd(os.path.join(VAL, 'v1', 'pre_fix_metrics.csv'))}
    return runs, primary, excluded, rules_by_run, cnt, met, fnd
RUNS, PRIMARY, EXCLUDED, RBR, CNT, MET, FND = e1_load()
BYSTY = {s: [r for r in RUNS if r['prompt_style'] == s] for s in STYLES}
SCL = {r for r in PRIMARY if r.startswith('S')}
check('E1 valid non-pilot runs', len(RUNS), 100)
check('E1 n per style', [len(BYSTY[s]) for s in STYLES], [33, 33, 34])
check('E1 primary rules', len(PRIMARY), 20)
def share(rows, rules):
    k = sum(1 for r in rows if RBR[r['run_id']] & rules); return k, len(rows)
check('C1 S-class runs per style', [share(BYSTY[s], SCL)[0] for s in STYLES], [31, 29, 17])
def distinct(r): return len(RBR[r['run_id']] & PRIMARY)
check('C2 mean distinct rules (naive, det, guard)', [round(sum(distinct(r) for r in BYSTY[s]) / len(BYSTY[s]), 2) for s in STYLES], [5.85, 5.7, 2.82])
SUMMD = rdtxt(os.path.join(E1, 'analysis', 'primary-v1', 'summary.md'))
HOLM = {}
for ln in open(os.path.join(ROOT, 'ARTICLE.md'), encoding='utf-8'):
    m = re.match(r'^\| (C[1-5]) \| ([\d,]+) \| (\d) \| ([\d,]+) \| ([\d,]+) \| (.*) \| (.*) \|$', ln.strip())
    if m: HOLM[m.group(1)] = {'p': m.group(2), 'rank': m.group(3), 'thr': m.group(4), 'adj': m.group(5), 'size': m.group(6), 'verdict': m.group(7)}
MANI['holm_from_article'] = HOLM

# ---------------- F01 design diagram ----------------
def f01():
    f = Fig(1000, 520, 'F01. Thiết kế nghiên cứu: đầu vào, năm thực nghiệm, ai chấm gì', 'Số liệu nêu trong sơ đồ lấy từ REPORT của từng thí nghiệm; không có kết quả ở hình này')
    def box(x, y, w, h, title, lines, col):
        f.rect(x, y, w, h, '#fff', col, 1, 2)
        f.text(x + 8, y + 18, title, 12, weight='bold')
        for i, t in enumerate(lines): f.text(x + 8, y + 36 + i * 15, t, 11, fill=INK)
    def arrow(x1, y1, x2, y2):
        f.line(x1, y1, x2, y2, '#555', 1.5)
        a = math.atan2(y2 - y1, x2 - x1); f.p.append('<polygon points="%.1f,%.1f %.1f,%.1f %.1f,%.1f" fill="#555"/>' % (x2, y2, x2 - 9 * math.cos(a - .4), y2 - 9 * math.sin(a - .4), x2 - 9 * math.cos(a + .4), y2 - 9 * math.sin(a + .4)))
    nodes = []
    box(14, 60, 200, 118, 'Đầu vào của E1', ['3 brief (B1, B2, B3)', '3 kiểu prompt: naive,', 'detailed-functional, guardrailed', '3 bộ công cụ: claude-haiku,', 'codex-luna, agy-g38flash'], '#999')
    box(14, 196, 200, 78, 'Guardrail', ['một tệp ngữ cảnh viết từ nguồn', 'độc lập, sha256 ghi trước', 'lần chạy guardrailed đầu tiên'], '#999')
    box(250, 60, 230, 118, 'E1: kho bản nháp đầu', ['101 lần chạy sinh, 100 hợp lệ', 'bộ phát hiện v1 (A13): 20 luật chính', 'ứng dụng do người viết: 20 trước 2022,', '20 từ 2024 (đối chứng C3)'], '#0072B2')
    box(250, 196, 230, 78, 'Kiểm chứng bộ phát hiện', ['độ nhạy đột biến; hai người chấm', 'mù trên mẫu phát hiện (kappa gộp 0,687)'], '#0072B2')
    box(520, 60, 230, 78, 'E2: đo A/B', ['A: mã lấy từ E1, B: bản tinh chỉnh', 'cùng ứng dụng lab, emulator', 'điểm cuối xác nhận C5'], '#D55E00')
    box(520, 156, 230, 66, 'E3: công cụ độc lập', ['quét tĩnh APK (MobSF, jadx),', 'phân loại thủ công từng phát hiện'], '#009E73')
    box(520, 240, 230, 78, 'E4: áp lực sửa lỗi', ['5 lỗi x 3 bộ công cụ x 2 biến thể;', '148 lượt thử phân tích được', 'điểm cuối xác nhận C4'], '#CC79A7')
    box(520, 336, 230, 78, 'E5: trợ lý là người tối ưu', ['ứng dụng đích có danh sách lỗi', 'chuẩn cố định trước; mô tả,', 'không có điểm cuối xác nhận'], '#E69F00')
    box(790, 60, 196, 130, 'Điểm cuối xác nhận', ['C1, C2, C3 (E1)', 'C4 (E4)', 'C5 (E2)', 'Holm trên năm phép (m = 5)', 'chỉ năm p-value được diễn giải'], '#333')
    box(790, 210, 196, 120, 'Ai chấm gì', ['người viết bộ phát hiện và', 'người viết guardrail khác nhau', 'E5: oracle độc lập với bên sửa', 'E4: gán nhãn có đọc lại mù'], '#333')
    arrow(214, 100, 250, 100); arrow(214, 235, 250, 150); arrow(365, 178, 365, 196); arrow(480, 100, 520, 100)
    arrow(480, 120, 520, 190); arrow(750, 100, 790, 100); arrow(750, 270, 790, 150)
    f.text(14, 470, 'Trục chung: tần suất (E1) nhân chi phí (E2), cộng với phát hiện và khắc phục (E3, E4, E5). Mọi số đo thời gian chạy trên emulator.', 11, fill=MUTE)
    f.text(14, 490, 'Giao thức đăng ký trước và đóng băng ở v1.0; chỉ Mục 15 (sửa đổi A1 đến A14) được thêm, không sửa dòng cũ.', 11, fill=MUTE)
    f.save('F01-design', 'Study design diagram')
    emit('F01-design', ['node', 'text_source'], [['E1', 'e1-corpus/REPORT.md'], ['E2', 'e2-bench/REPORT.md'], ['E3', 'e3-security/REPORT.md'], ['E4', 'e4-pressure/REPORT.md'], ['E5', 'e5-optimizer/REPORT.md']])

# ---------------- F02 design sensitivity (Appendix A of the protocol) ----------------
def fisher_p(a, b, c, d):
    n1 = a + b; n2 = c + d; k = a + c; N = n1 + n2
    pm = lambda x: comb(n1, x) * comb(n2, k - x) / comb(N, k)
    p0 = pm(a)
    return sum(pm(x) for x in range(max(0, k - n2), min(n1, k) + 1) if pm(x) <= p0 * (1 + 1e-9))
def f02():
    pairs = [(.8, .4), (.7, .3), (.9, .5)]; ns = list(range(6, 49, 3))
    pw = {}
    for n in ns:
        grid = [[fisher_p(x, n - x, y, n - y) for y in range(n + 1)] for x in range(n + 1)]
        for p1, p2 in pairs:
            for al in (.05, .01):
                pw[(n, p1, p2, al)] = sum(comb(n, x) * p1 ** x * (1 - p1) ** (n - x) * comb(n, y) * p2 ** y * (1 - p2) ** (n - y)
                                          for x in range(n + 1) for y in range(n + 1) if grid[x][y] <= al)
    f = Fig(900, 400, 'F02. Độ nhạy thiết kế: lực kiểm định của kiểm định Fisher chính xác theo cỡ mẫu mỗi nhánh', 'Tính bằng liệt kê theo Phụ lục A của giao thức; n thực tế mỗi kiểu prompt: 33, 33, 34')
    cols = {(.8, .4): '#E69F00', (.7, .3): '#56B4E9', (.9, .5): '#009E73'}; shp = {(.8, .4): 'circle', (.7, .3): 'square', (.9, .5): 'triangle'}
    rows = []
    for i, al in enumerate((.05, .01)):
        ax = Ax(f, 70 + i * 430, 70, 340, 250, (6, 48), (0, 1))
        ax.xaxis([6, 12, 18, 24, 30, 36, 42, 48], 'n mỗi nhánh', lambda v: str(int(v))); ax.yaxis([0, .2, .4, .6, .8, 1], 'lực kiểm định' if i == 0 else '')
        f.text(ax.x + 170, 62, 'alpha = ' + vi(al, 2), 12, 'middle', weight='bold')
        ax.vline(18, '#555', '4 3', 'n = 18'); ax.vline(36, '#555', '4 3', 'n = 36')
        for t in (33, 34): f.line(ax.X(t), ax.y + ax.h, ax.X(t), ax.y + ax.h - 8, '#000', 2)
        for pr in pairs:
            pts = [(ax.X(n), ax.Y(pw[(n, pr[0], pr[1], al)])) for n in ns]
            f.p.append('<polyline fill="none" stroke="%s" stroke-width="2" points="%s"/>' % (cols[pr], ' '.join('%.1f,%.1f' % p for p in pts)))
            for (xx, yy) in pts: f.mark(xx, yy, shp[pr], cols[pr], 3)
            for n in ns: rows.append([al, pr[0], pr[1], n, round(pw[(n, pr[0], pr[1], al)], 4)])
    legend(f, 700, 345, [('0,8 so với 0,4', cols[(.8, .4)], 'circle'), ('0,7 so với 0,3', cols[(.7, .3)], 'square'), ('0,9 so với 0,5', cols[(.9, .5)], 'triangle')], 16)
    f.text(70, 385, 'Vạch đen ở trục x: n thực tế (33, 34).', 10, fill=MUTE)
    f.save('F02-power', 'Power curves'); emit('F02-power', ['alpha', 'p1', 'p2', 'n_per_arm', 'power'], rows)

# ---------------- F03 E1 flow ----------------
def f03():
    allr = [r for r in rd(os.path.join(E1, 'runs.csv')) if r['pilot'] == 'False']
    bundles = ['claude-haiku', 'codex-luna', 'agy-g38flash']
    f = Fig(980, 190 + 9 * 34, 'F03. E1: lần chạy sinh, hợp lệ và build được, theo kiểu prompt và bộ công cụ', 'Không pilot. Build = cột build của runs.csv (pass); sửa chữa cơ học theo R7 tính riêng')
    ax = Ax(f, 260, 70, 520, 9 * 34, (0, 14), (0, 1)); ax.xaxis([0, 2, 4, 6, 8, 10, 12, 14], 'số lần chạy', lambda v: str(int(v)), True)
    rows = []; i = 0
    for s in STYLES:
        for b in bundles:
            cell = [r for r in allr if r['prompt_style'] == s and r['bundle'] == b]
            val = [r for r in cell if r['valid'] == 'True']
            bp = [r for r in val if r['build'] == 'pass']; zt = [r for r in val if r['build_zero_touch'] == 'pass']; rp = [r for r in val if r['build_repaired'] == 'pass']
            y = 70 + i * 34 + 6
            f.text(250, y + 12, s + ' / ' + b, 11, 'end')
            f.rect(ax.X(0), y, ax.X(len(cell)) - ax.X(0), 7, '#E3E3E3'); f.rect(ax.X(0), y + 8, ax.X(len(val)) - ax.X(0), 7, '#9FB8D0'); f.rect(ax.X(0), y + 16, ax.X(len(bp)) - ax.X(0), 7, BC)
            f.text(ax.X(14) + 8, y + 12, 'sinh %d, hợp lệ %d, build %d' % (len(cell), len(val), len(bp)), 10, fill=MUTE)
            rows.append([s, b, len(cell), len(val), len(bp), len(zt), len(rp)]); i += 1
    legend(f, 260, 100 + 9 * 34, [('sinh (không pilot)', '#E3E3E3', 'square'), ('hợp lệ', '#9FB8D0', 'square'), ('build pass', BC, 'square')], 15)
    by = {b: (sum(1 for r in RUNS if r['bundle'] == b and r['build'] == 'pass'), sum(1 for r in RUNS if r['bundle'] == b)) for b in bundles}
    check('E1 build pass per bundle (claude 13/36, codex 10/36, agy 19/28)', [by[b] for b in bundles], [(13, 36), (10, 36), (19, 28)])
    f.save('F03-e1-flow', 'E1 flow'); emit('F03-e1-flow', ['prompt_style', 'bundle', 'generated', 'valid', 'build_pass', 'build_zero_touch_pass', 'build_repaired_pass'], rows)

# ---------------- F04 detector validation ----------------
def f04():
    mut = {r['rule_id']: r for r in rd(os.path.join(VAL, 'mutation.csv'))}
    pre = {r['rule_id']: r for r in rd(os.path.join(VAL, 'precision_v1.csv'))}
    st = {r['rule_id']: r['status'] for r in rd(os.path.join(VAL, 'rules_status_v1.csv'))}
    kap = json.loads(rdtxt(os.path.join(VAL, 'kappa.json')))
    rules = sorted(mut, key=lambda r: ('PSUD'.index(r[0]), r))
    f = Fig(980, 160 + len(rules) * 20, 'F04. Kiểm chứng bộ phát hiện v1: độ nhạy đột biến và độ chính xác (người chấm mù)', 'Cổng 0,80 cho cả hai; kappa của hai người chấm: v1 %s, v2 %s, gộp %s' % (vi(kap['kappa_v1'], 3), vi(kap['kappa_v2'], 3), vi(kap['kappa'], 3)))
    a1 = Ax(f, 90, 70, 330, len(rules) * 20, (0, 1), (0, 1)); a2 = Ax(f, 540, 70, 330, len(rules) * 20, (0, 1), (0, 1))
    a1.xaxis([0, .2, .4, .6, .8, 1], 'độ nhạy đột biến'); a2.xaxis([0, .2, .4, .6, .8, 1], 'độ chính xác (Wilson 95%)')
    a1.vline(.8, '#555', '4 3'); a2.vline(.8, '#555', '4 3'); rows = []
    for i, r in enumerate(rules):
        y = 70 + i * 20 + 10; f.text(80, y + 4, r, 11, 'end')
        rc = float(mut[r]['recall']); f.mark(a1.X(rc), y, 'circle', BC, 4); f.text(a1.X(1) + 6, y + 4, 'n = ' + mut[r]['mutants'], 9, fill=MUTE)
        p = pre[r]; ok = st[r] == 'validated'; pr = float(p['precision']); lo, hi = float(p['wilson_lo']), float(p['wilson_hi'])
        f.line(a2.X(lo), y, a2.X(hi), y, '#444' if ok else '#aaa', 2); f.mark(a2.X(pr), y, 'circle', '#009E73' if ok else '#8C8C8C', 4, hollow=not ok)
        f.text(a2.X(1) + 6, y + 4, 'n = ' + p['n_rated'] + ('' if ok else ' (chưa kiểm chứng)'), 9, fill=MUTE)
        rows.append([r, mut[r]['mutants'], mut[r]['recall'], p['n_rated'], p['precision'], p['wilson_lo'], p['wilson_hi'], st[r]])
    f.save('F04-validation', 'Detector validation'); emit('F04-validation', ['rule_id', 'mutants', 'recall', 'n_rated', 'precision', 'wilson_lo', 'wilson_hi', 'status'], rows)

# ---------------- F05 coverage ----------------
def f05():
    cm = rd(os.path.join(VAL, 'coverage_matrix.csv')); cm.sort(key=lambda r: ('PSUD'.index(r['rule_id'][0]), r['rule_id']))
    cols = ['slop-scan_findings', 'lint_findings', 'depcheck_findings']
    f = Fig(760, 100 + len(cm) * 18, 'F05. Độ phủ so với Lint, detekt và compose-rules: luật chỉ bộ phát hiện tùy biến có', 'Số phát hiện theo công cụ (coverage_matrix.csv); ô tô đậm = công cụ có phát hiện; cột cuối: công cụ có sẵn khai báo luật')
    hx = [110, 220, 330]; rows = []
    for j, c in enumerate(['bộ phát hiện', 'Lint', 'kiểm tra phụ thuộc']): f.text(hx[j] + 40, 62, c, 11, 'middle', weight='bold')
    f.text(440, 62, 'công cụ có sẵn khai báo luật', 11, weight='bold'); f.text(690, 62, 'chỉ tùy biến', 11, 'end', weight='bold')
    for i, r in enumerate(cm):
        y = 72 + i * 18; f.text(100, y + 13, r['rule_id'], 11, 'end')
        for j, c in enumerate(cols):
            v = int(r[c]); f.rect(hx[j], y, 80, 15, '#0072B2' if v > 0 else '#F2F2F2', '#ccc', 1 if v > 0 else 1); f.text(hx[j] + 40, y + 12, str(v), 10, 'middle', fill='#fff' if v > 0 else MUTE)
        f.text(440, y + 12, r['existing_tool_declares'].replace('|', ', ')[:42], 10, fill=MUTE)
        if r['custom_only'] == '1': f.mark(676, y + 7, 'diamond', AC, 5)
        rows.append([r['rule_id']] + [r[c] for c in cols] + [r['existing_tool_declares'], r['custom_only']])
    f.save('F05-coverage', 'Coverage matrix'); emit('F05-coverage', ['rule_id'] + cols + ['existing_tool_declares', 'custom_only'], rows)

# ---------------- F06 prevalence ----------------
def f06():
    rs = rd(os.path.join(VAL, 'rules_status_v1.csv')); allr = sorted({r['rule_id'] for r in rs} | {'D02', 'S11'})
    unf = [r for s in ('naive', 'detailed-functional') for r in BYSTY[s]]
    def row(rule): return rule, sum(1 for r in unf if rule in RBR[r['run_id']]) / len(unf)
    prim = [r for r in allr if r in PRIMARY]; exc = [r for r in allr if r in EXCLUDED]
    order = []
    for grp, lab in ((prim, 'luật chính'), (exc, 'chưa kiểm chứng (chỉ mô tả, không dùng cho C1 đến C3)')):
        order.append(('H', lab))
        for cls in 'PSUD':
            for r in sorted([x for x in grp if x[0] == cls], key=lambda x: -row(x)[1]): order.append(('R', r))
    h = 150 + len(order) * 22
    f = Fig(940, h, 'F06. Tần suất: tỷ lệ lần chạy hợp lệ có ít nhất một phát hiện của luật, theo kiểu prompt', 'Bộ phát hiện v1; Wilson 95%; mỗi luật sắp theo tần suất gộp của hai nhánh không guardrail trong từng lớp; D02, S11 đang chờ kiểm chứng')
    ax = Ax(f, 130, 62, 700, len(order) * 22, (0, 1), (0, 1)); ax.xaxis([0, .2, .4, .6, .8, 1], 'tỷ lệ lần chạy có phát hiện')
    rows = []; y = 62
    for kind, v in order:
        if kind == 'H': f.text(14, y + 14, v, 11, weight='bold', fill='#333'); y += 22; continue
        f.text(120, y + 14, v + (' (chờ)' if v in ('D02', 'S11') else ''), 11, 'end', fill='#000' if v in PRIMARY else MUTE)
        for k, s in enumerate(STYLES):
            kk = sum(1 for r in BYSTY[s] if v in RBR[r['run_id']]); n = len(BYSTY[s]); lo, hi = wilson(kk, n)
            yy = y + 5 + k * 5
            f.line(ax.X(lo), yy, ax.X(hi), yy, STY[s], 1.4); f.mark(ax.X(kk / n), yy, STYM[s], STY[s], 2.6, hollow=(v in EXCLUDED))
            rows.append([v, s, kk, n, round(kk / n, 4), round(lo, 4), round(hi, 4), 'primary' if v in PRIMARY else 'excluded'])
        y += 22
    legend(f, 760, 30, [(s, STY[s], STYM[s]) for s in STYLES], 14)
    chk = {(r[0], r[1]): (r[2], r[3]) for r in rows}
    check('Bảng 5.4 P13 naive/detailed/guardrailed', [chk[('P13', s)] for s in STYLES], [(7, 33), (8, 33), (0, 34)])
    f.save('F06-prevalence', 'Prevalence'); emit('F06-prevalence', ['rule_id', 'prompt_style', 'k', 'n', 'share', 'wilson_lo', 'wilson_hi', 'track'], rows)

# ---------------- E1 summary.md parsing ----------------
def md_first(pattern, text=None):
    m = re.search(pattern, text or SUMMD); return m
def sens_rows():
    out = []
    for ln in SUMMD.splitlines():
        m = re.match(r'^\| (\((?:a|b|c)\)[^|]*) \| (\d+) \| ([+-][\d.]+) \(p=([\d.]+)\) \| ([+-][\d.]+) \(p=([\d.]+)\) \| ([+-][\d.]+) \(p=([\d.]+)\) \|$', ln)
        if m: out.append((m.group(1).strip(), int(m.group(2)), float(m.group(3)), float(m.group(5)), float(m.group(7))))
    return out
def style_rows():
    d = {}
    for ln in SUMMD.splitlines():
        m = re.match(r'^\| (naive|detailed-functional|guardrailed) \| (\d+) \| (\d+) \| ([\d.]+) \| \[([\d.]+), ([\d.]+)\] \| ([\d.]+) \| ([\d.]+) \| \[([\d.]+), ([\d.]+)\] \|$', ln)
        if m and m.group(1) not in d: d[m.group(1)] = dict(n=int(m.group(2)), k=int(m.group(3)), mean=float(m.group(7)), blo=float(m.group(9)), bhi=float(m.group(10)))
    return d
def strata():
    out = []
    for b in ('B1', 'B2', 'B3'):
        for bu in ('claude-haiku', 'codex-luna', 'agy-g38flash'):
            cell = {s: [r for r in BYSTY[s] if r['brief'] == b and r['bundle'] == bu] for s in STYLES}
            out.append((b + ' x ' + bu, cell))
    return out
def sensitivity_panel(f, x, y, w, h, idx, dom, ticks, ref, ref_label, title):
    rows = sens_rows(); ax = Ax(f, x, y, w, h, dom, (0, 1)); ax.xaxis(ticks, title, lambda v: vis(v, 2) if v else '0')
    ax.vline(ref, '#333', '4 3', ref_label)
    n = len(rows)
    for i, (lab, nv, *vals) in enumerate(rows):
        yy = y + 14 + i * (h - 20) / n
        f.text(x - 6, yy + 4, lab.replace('(a) ', 'a: ').replace('(b) ', 'b: ').replace('(c) ', 'c: ') + ' (n ' + str(nv) + ')', 9, 'end', fill=MUTE)
        f.mark(ax.X(vals[idx]), yy, 'circle', '#444', 4)
    return rows

# ---------------- F07 C1 ----------------
def f07():
    sr = style_rows(); m = md_first(r'\*\*C1\*\*[^\n]*difference ([+-][\d.]+)')
    diff = float(m.group(1)); H = HOLM['C1']
    f = Fig(1180, 470, 'F07. C1: tỷ lệ lần chạy có ít nhất một phát hiện nhóm S, naive so với guardrailed', 'Bộ phát hiện v1 (A13), 20 luật chính; n = 33, 33, 34 (dưới mục tiêu 36): bị giới hạn lực kiểm định; detailed-functional là đối chiếu thứ cấp')
    a = Ax(f, 150, 80, 220, 250, (0, 1), (0, 1)); rows = []
    a.xaxis([0, .25, .5, .75, 1], 'tỷ lệ'); f.text(a.x + 110, 70, 'A. tỷ lệ theo kiểu prompt (Wilson 95%)', 11, 'middle', weight='bold')
    for i, s in enumerate(STYLES):
        k, n = share(BYSTY[s], SCL); lo, hi = wilson(k, n); yy = a.y + 40 + i * 80
        f.text(a.x - 6, yy + 4, s, 10, 'end'); f.line(a.X(lo), yy, a.X(hi), yy, STY[s], 3); f.mark(a.X(k / n), yy, STYM[s], STY[s], 6)
        f.text(a.X(1) - 2, yy - 10, '%d/%d' % (k, n), 10, 'end', fill=MUTE); rows.append(['arm', s, k, n, round(k / n, 4), round(lo, 4), round(hi, 4)])
    b = Ax(f, 480, 80, 260, 250, (-.4, 1), (0, 1)); b.xaxis([-.25, 0, .25, .5, .75, 1], 'hiệu rủi ro (naive trừ guardrailed)', lambda v: vis(v, 2) if v else '0')
    f.text(b.x + 130, 70, 'B. hiệu theo 9 tầng (brief x bộ công cụ) và tổng', 11, 'middle', weight='bold')
    b.vline(.15, AC, '5 3', 'SESOI 0,15'); b.vline(0, '#333', None)
    for t in (.10, .25): f.line(b.X(t), b.y + b.h, b.X(t), b.y + b.h - 8, '#555', 1.5)
    for i, (lab, cell) in enumerate(strata()):
        if not cell['naive'] or not cell['guardrailed']: continue
        d = share(cell['naive'], SCL)[0] / len(cell['naive']) - share(cell['guardrailed'], SCL)[0] / len(cell['guardrailed'])
        f.mark(b.X(d), b.y + 24 + (i % 9) * 18, 'circle', '#888', 3.5, op=.6); rows.append(['stratum', lab, round(d, 4), '', '', '', ''])
    f.mark(b.X(diff), b.y + 215, 'diamond', '#000', 7); f.text(b.X(diff), b.y + 238, 'tổng ' + vis(diff, 3), 11, 'middle', weight='bold'); rows.append(['overall', 'naive-guardrailed', diff, '', '', '', ''])
    c = Ax(f, 980, 80, 170, 250, (-.2, .8), (0, 1)); c.xaxis([0, .25, .5, .75], 'hiệu C1', lambda v: vis(v, 2) if v else '0')
    f.text(c.x + 40, 70, 'C. độ nhạy (A6): hiệu C1 theo biến thể', 11, 'middle', weight='bold'); c.vline(.15, AC, '5 3', 'SESOI')
    for i, (lab, nv, d1, d2, d3) in enumerate(sens_rows()):
        yy = c.y + 16 + i * 28; f.text(c.x - 6, yy + 4, lab.replace('(a) ', 'a: ').replace('(b) ', 'b: ').replace('(c) ', 'c: ')[:44] + ' (n ' + str(nv) + ')', 9, 'end', fill=MUTE); f.mark(c.X(d1), yy, 'circle', '#444', 4); rows.append(['sensitivity', lab, d1, nv, '', '', ''])
    f.text(14, 380, 'Kết luận theo quy tắc đã đăng ký: %s. p thô %s, p hiệu chỉnh Holm %s (Bảng 5.27); độ lớn %s.' % (H['verdict'], H['p'], H['adj'], H['size']), 12, weight='bold')
    f.text(14, 400, 'Ở các ứng dụng build được (n = 42), hiệu C1 là +0,000 (biến thể c ở panel C): kết luận không đúng trên tập con đó.', 11, fill=MUTE)
    f.text(14, 420, 'Hiệu rủi ro tổng và các biến thể chép từ e1-corpus/results/analysis/primary-v1/summary.md; các điểm theo tầng tính từ findings.', 10, fill=MUTE)
    legend(f, 14, 440, [(s, STY[s], STYM[s]) for s in STYLES[:1]], 14)
    check('C1 overall diff', diff, 0.439)
    f.save('F07-c1', 'C1'); emit('F07-c1', ['kind', 'label', 'value', 'k_or_n', 'n', 'lo', 'hi'], rows)

# ---------------- F08 C2 ----------------
def f08():
    sr = style_rows(); m = md_first(r'\*\*C2\*\*[^\n]*difference ([+-][\d.]+)'); diff = float(m.group(1)); H = HOLM['C2']
    f = Fig(1180, 470, 'F08. C2: số mã luật khác nhau trong mỗi lần chạy, naive so với guardrailed', 'Bộ phát hiện v1, 20 luật chính (P, S, U, D); n = 33, 33, 34: bị giới hạn lực kiểm định; trung bình và bootstrap 95% chép từ summary.md')
    a = Ax(f, 150, 80, 240, 250, (0, 14), (0, 1)); a.xaxis([0, 2, 4, 6, 8, 10, 12, 14], 'số mã luật khác nhau', lambda v: str(int(v))); rows = []
    f.text(a.x + 120, 70, 'A. từng lần chạy, trung bình và bootstrap 95%', 11, 'middle', weight='bold')
    for i, s in enumerate(STYLES):
        yy = a.y + 40 + i * 80; f.text(a.x - 6, yy + 4, s, 10, 'end'); vals = sorted(distinct(r) for r in BYSTY[s])
        c = Counter(); 
        for j, v in enumerate(vals):
            c[v] += 1; f.mark(a.X(v), yy - 16 + (c[v] - 1) * 4 % 28, STYM[s], STY[s], 2.8, op=.55); rows.append(['run', s, v, '', ''])
        d = sr[s]; f.line(a.X(d['blo']), yy + 24, a.X(d['bhi']), yy + 24, '#000', 3); f.mark(a.X(d['mean']), yy + 24, 'diamond', '#000', 5)
        f.text(a.X(14) - 2, yy - 18, 'trung bình ' + vi(d['mean'], 2), 10, 'end', fill=MUTE); rows.append(['mean_boot', s, d['mean'], d['blo'], d['bhi']])
    b = Ax(f, 490, 80, 230, 250, (-1, 8), (0, 1)); b.xaxis([0, 1, 2, 3, 4, 6, 8], 'hiệu (naive trừ guardrailed)', lambda v: vis(v, 0) if v else '0')
    f.text(b.x + 125, 70, 'B. hiệu theo 9 tầng và tổng', 11, 'middle', weight='bold'); b.vline(2.0, AC, '5 3', 'SESOI 2,0'); b.vline(0, '#333', None)
    for i, (lab, cell) in enumerate(strata()):
        if not cell['naive'] or not cell['guardrailed']: continue
        d = sum(distinct(r) for r in cell['naive']) / len(cell['naive']) - sum(distinct(r) for r in cell['guardrailed']) / len(cell['guardrailed'])
        f.mark(b.X(d), b.y + 24 + (i % 9) * 18, 'circle', '#888', 3.5, op=.6); rows.append(['stratum', lab, round(d, 4), '', ''])
    f.mark(b.X(diff), b.y + 215, 'diamond', '#000', 7); f.text(b.X(diff), b.y + 238, 'tổng ' + vis(diff, 3), 11, 'middle', weight='bold'); rows.append(['overall', 'naive-guardrailed', diff, '', ''])
    c = Ax(f, 980, 80, 170, 250, (-1, 9), (0, 1)); c.xaxis([0, 2, 4, 6, 8], 'hiệu C2', lambda v: vis(v, 0) if v else '0'); c.vline(2.0, AC, '5 3', 'SESOI')
    f.text(c.x + 40, 70, 'C. độ nhạy (A6)', 11, 'middle', weight='bold')
    for i, (lab, nv, d1, d2, d3) in enumerate(sens_rows()):
        yy = c.y + 16 + i * 28; f.text(c.x - 6, yy + 4, lab.replace('(a) ', 'a: ').replace('(b) ', 'b: ').replace('(c) ', 'c: ')[:44] + ' (n ' + str(nv) + ')', 9, 'end', fill=MUTE); f.mark(c.X(d2), yy, 'circle', '#444', 4); rows.append(['sensitivity', lab, d2, nv, ''])
    f.text(14, 380, 'Kết luận theo quy tắc đã đăng ký: %s. p thô %s, p hiệu chỉnh Holm %s (Bảng 5.27); độ lớn %s.' % (H['verdict'], H['p'], H['adj'], H['size']), 12, weight='bold')
    f.text(14, 400, 'Ở các ứng dụng build được (n = 42) hiệu vẫn +3,829 (biến thể c ở panel C).', 11, fill=MUTE)
    check('C2 overall diff', diff, 3.025)
    f.save('F08-c2', 'C2'); emit('F08-c2', ['kind', 'label', 'value', 'lo_or_n', 'hi'], rows)

# ---------------- F09 G3 ----------------
HELD = ['P03', 'P08', 'S06', 'D02', 'U04']
INSTR = ['P01', 'P02', 'P04', 'P06', 'P07', 'P09', 'P10', 'P11', 'P13', 'S01', 'S02', 'S03', 'S04', 'S05', 'S07', 'S08', 'S11', 'U01', 'U02', 'D01']
def f09():
    g = re.findall(r'\*\*G3\*\* \(([a-z\- ]+)\): Delta_instructed ([+-][\d.]+), Delta_heldout ([+-][\d.]+), contrast ([+-][\d.]+), bootstrap 95% \[([\d.-]+), ([\d.-]+)\]', SUMMD)[:2]
    prim = sorted(PRIMARY, key=lambda r: ('PSUD'.index(r[0]), r)); gr = lambda r: 'held-out' if r in HELD else ('instructed' if r in INSTR else 'neither')
    f = Fig(1120, 140 + len(prim) * 22, 'F09. G3: guardrail giảm luật được nêu hay cả luật bị giữ lại?', 'Thay đổi tần suất (naive trừ guardrailed) theo luật, 20 luật chính; bị giữ lại = P03, P08, S06, D02, U04 (P05 và U05 trượt cổng)')
    a = Ax(f, 120, 70, 450, len(prim) * 22, (-.35, 1), (0, 1)); a.xaxis([0, .25, .5, .75, 1], 'giảm tần suất (naive trừ guardrailed)', lambda v: vis(v, 2) if v else '0'); a.vline(0, '#333', None)
    sh = {'held-out': ('#CC79A7', 'diamond'), 'instructed': ('#0072B2', 'circle'), 'neither': ('#8C8C8C', 'square')}; rows = []
    for i, r in enumerate(prim):
        yy = 70 + i * 22 + 11; dn = sum(1 for x in BYSTY['naive'] if r in RBR[x['run_id']]) / len(BYSTY['naive']); dg = sum(1 for x in BYSTY['guardrailed'] if r in RBR[x['run_id']]) / len(BYSTY['guardrailed'])
        f.text(110, yy + 4, r, 11, 'end'); c, s = sh[gr(r)]; f.mark(a.X(dn - dg), yy, s, c, 5); rows.append(['rule', r, gr(r), round(dn, 4), round(dg, 4), round(dn - dg, 4), '', ''])
    legend(f, 600, 80, [('được nêu trong guardrail', '#0072B2', 'circle'), ('bị giữ lại (không nêu)', '#CC79A7', 'diamond'), ('không thuộc hai nhóm (S09, U03)', '#8C8C8C', 'square')], 16)
    f.text(600, 160, 'Tương phản đã đăng ký (bootstrap 95%, chép từ summary.md):', 11, weight='bold')
    b = Ax(f, 640, 220, 300, 100, (-.5, 2.2), (0, 1)); b.xaxis([0, 1, 2], 'chênh Delta_chỉ dẫn trừ Delta_bị giữ lại', lambda v: vis(v, 0) if v else '0'); b.vline(0, '#333', None)
    for i, (nm, di, dh, ct, lo, hi) in enumerate(g):
        yy = 245 + i * 32; f.text(b.x - 6, yy + 4, nm, 10, 'end'); f.line(b.X(float(lo)), yy, b.X(float(hi)), yy, '#000', 3); f.mark(b.X(float(ct)), yy, 'diamond', '#000', 6)
        f.text(b.X(float(hi)) + 6, yy + 4, 'chênh ' + vis(float(ct), 2) + ' [' + vi(float(lo)) + '; ' + vi(float(hi)) + ']', 10, fill=MUTE); rows.append(['contrast', nm, di, dh, ct, lo, hi])
    f.text(14, 100 + len(prim) * 22 + 20, 'Cả luật bị giữ lại cũng giảm: đọc là "khái quát hóa hoặc ngẫu nhiên", không kết luận cơ chế. Mô tả, không có p.', 11, fill=MUTE)
    f.save('F09-g3', 'G3'); emit('F09-g3', ['kind', 'name', 'a', 'b', 'c', 'd', 'e', 'f'], rows)

# ---------------- F10 C3 ----------------
def f10():
    hf = defaultdict(int)
    for x in rd(os.path.join(VAL, 'v1', 'pre_fix_human_findings.csv')):
        if x['rule_id'] in SCL: hf[x['run_id']] += 1
    hm = rd(os.path.join(VAL, 'v1', 'pre_fix_human_metrics.csv'))
    ai = [r for s in ('naive', 'detailed-functional') for r in BYSTY[s]]
    def sc(rid): return sum(CNT[(rid, r)] for r in SCL)
    grp = {'AI không guardrail (n = %d)' % len(ai): [(sc(r['run_id']) / (float(MET[r['run_id']]['kotlin_loc']) / 1000), sc(r['run_id']) / (float(MET[r['run_id']]['kotlin_nws']) / 100000)) for r in ai]}
    for st, lab in (('pre', 'người viết trước 30/11/2022'), ('post', 'người viết từ 2024')):
        rows_ = [m for m in hm if m['stratum'] == st]; grp[lab + ' (n = %d)' % len(rows_)] = [(hf[m['run_id']] / (float(m['kotlin_loc']) / 1000), hf[m['run_id']] / (float(m['kotlin_nws']) / 100000)) for m in rows_]
    keys = list(grp); meds = [(median([v[0] for v in grp[k]]), median([v[1] for v in grp[k]])) for k in keys]
    check('C3 medians per kLOC (AI, pre, post)', [round(m[0], 2) for m in meds], [2.37, 1.02, 0.24])
    check('C3 medians per 100k nws (AI, pre, post)', [round(m[1], 2) for m in meds], [7.83, 3.47, 0.87])
    cl = re.findall(r'Cliff\'s delta = ([+-]\d+\.\d+)', SUMMD)[:4]; H = HOLM['C3']
    f = Fig(1000, 500, 'F10. C3: phát hiện nhóm S trên 1000 dòng Kotlin, AI không guardrail so với ứng dụng do người viết', 'Bộ phát hiện v1; chấm tròn từng lần chạy hoặc ứng dụng, vạch đen = trung vị; tầng 2024 trở đi là so sánh thứ hai, không xác nhận')
    cols = ['#E69F00', HP, HQ]; shp = ['circle', 'cross', 'cross']; rows = []
    for p, (idx, ttl, unit) in enumerate(((0, 'A. trên 1000 dòng Kotlin', 'phát hiện / kLOC'), (1, 'B. độ nhạy: trên 100 nghìn ký tự khác khoảng trắng', 'phát hiện / 100k ký tự'))):
        mx = max(v[idx] for k in keys for v in grp[k]) * 1.08; ax = Ax(f, 250 + p * 380 - (p * 20), 90, 290 if p == 0 else 270, 220, (0, mx), (0, 1)); ax.xaxis([round(mx * t / 4, 1) for t in range(5)], unit, lambda v: vi(v, 1))
        f.text(ax.x + ax.w / 2, 78, ttl, 11, 'middle', weight='bold')
        for i, k in enumerate(keys):
            yy = ax.y + 35 + i * 70
            if p == 0: f.text(ax.x - 6, yy + 4, k, 9, 'end')
            vals = sorted(v[idx] for v in grp[k])
            for j, v in enumerate(vals): f.mark(ax.X(v), yy - 14 + (j % 6) * 5, shp[i], cols[i], 3, op=.6); rows.append([ttl[:1], k, round(v, 4)])
            md = meds[i][idx]; f.line(ax.X(md), yy - 24, ax.X(md), yy + 22, '#000', 2.5); f.text(ax.X(md) + 4, yy + 34, 'trung vị ' + vi(md, 2), 10, fill=MUTE)
    f.text(14, 372, 'delta Cliff (AI cao hơn): so với trước 2022 %s (xác nhận), so với 2024 trở đi %s (thứ hai); trên 100k ký tự %s và %s.' % (vis(float(cl[0]), 3), vis(float(cl[1]), 3), vis(float(cl[2]), 3), vis(float(cl[3]), 3)), 11)
    f.text(14, 396, 'Kết luận theo quy tắc đã đăng ký: %s. p thô %s, p hiệu chỉnh Holm %s (Bảng 5.27); độ lớn %s.' % (H['verdict'], H['p'], H['adj'], H['size']), 12, weight='bold')
    f.text(14, 418, 'Ứng dụng tham chiếu android/app không được vẽ: REPORT E1 không báo cáo nó. Tầng người viết gồm ứng dụng phổ biến và lớn hơn.', 10, fill=MUTE)
    f.save('F10-c3', 'C3'); emit('F10-c3', ['panel', 'group', 'value'], rows)

# ---------------- F11 decorative density ----------------
def f11():
    hm = rd(os.path.join(VAL, 'v1', 'pre_fix_human_metrics.csv'))
    grp = [(s, [MET[r['run_id']] for r in BYSTY[s]], STY[s], STYM[s]) for s in STYLES]
    grp += [('người viết trước 2022', [m for m in hm if m['stratum'] == 'pre'], HP, 'cross'), ('người viết từ 2024', [m for m in hm if m['stratum'] == 'post'], HQ, 'cross')]
    f = Fig(1000, 450, 'F11. Mật độ hiệu ứng trang trí (phần của H3 ở E1): theo mục danh sách và theo màn hình', 'Mô tả, không có p; đọc từ metrics của E1 (trích xuất độc lập với luật); mỗi chấm một lần chạy hoặc một ứng dụng'); rows = []
    for p, (key, ttl) in enumerate((('effect_density_item', 'A. mật độ theo mục danh sách'), ('effect_density_screen', 'B. mật độ theo màn hình'))):
        mx = max(float(m[key]) for _, ms, _, _ in grp for m in ms) * 1.08 or 1
        ax = Ax(f, 250 + p * 380, 90, 300, 230, (0, mx), (0, 1)); ax.xaxis([round(mx * t / 4, 2) for t in range(5)], key, lambda v: vi(v, 2)); f.text(ax.x + 150, 78, ttl, 11, 'middle', weight='bold')
        for i, (nm, ms, col, shp) in enumerate(grp):
            yy = ax.y + 24 + i * 44
            if p == 0: f.text(ax.x - 6, yy + 4, nm + ' (n = %d)' % len(ms), 9, 'end')
            for j, m in enumerate(sorted(ms, key=lambda m: float(m[key]))):
                v = float(m[key]); f.mark(ax.X(v), yy - 12 + (j % 6) * 5, shp, col, 3, op=.6); rows.append([key, nm, v])
    txt = []
    for nm, ms, _, _ in grp[:3]:
        k = sum(1 for m in ms if float(m['effects_item']) > 0); txt.append('%s: %d/%d' % (nm, k, len(ms)))
        rows.append(['runs_with_decorated_list_item', nm, k])
    f.text(14, 386, 'Lần chạy có ít nhất một mục danh sách có hiệu ứng: ' + '; '.join(txt) + '.', 11)
    f.text(14, 408, 'Số lần chạy ở đây là từ metrics.csv; phân bố này không có trong REPORT E1, nên không dùng làm điểm cuối.', 10, fill=MUTE)
    f.save('F11-decorative-density', 'Effect density'); emit('F11-decorative-density', ['measure', 'group', 'value'], rows)

# ---------------- F12 H1 and H4 ----------------
def f12():
    VR = {r['run_id'] for r in RUNS}
    coords = [c for c in rd(os.path.join(E1, 'depfix', 'coordinates_fixed.csv')) if c['run_id'] in VR]
    first = [c for c in rd(os.path.join(E1, 'coordinates.csv')) if c['run_id'] in VR]
    sty = {r['run_id']: r['prompt_style'] for r in RUNS}
    cls = sorted({c['class'] for c in coords} | {'hallucinated-artifact'}); CC = {'ok': '#9FB8D0', 'hallucinated-version': '#D55E00', 'hallucinated-artifact': '#000000'}
    f = Fig(1000, 450, 'F12. H1 và H4: phụ thuộc cũ và tọa độ Gradle', 'A: tỷ lệ lần chạy có D01 hoặc D02 (Wilson 95%). B: lớp của từng thể hiện tọa độ sau khi sửa tra cứu (depfix/coordinates_fixed.csv); lớp đọc từ tệp'); rows = []
    a = Ax(f, 190, 80, 250, 200, (0, 1), (0, 1)); a.xaxis([0, .25, .5, .75, 1], 'tỷ lệ'); f.text(a.x + 125, 70, 'A. D01 hoặc D02', 11, 'middle', weight='bold')
    for i, s in enumerate(STYLES):
        k = sum(1 for r in BYSTY[s] if RBR[r['run_id']] & {'D01', 'D02'}); n = len(BYSTY[s]); lo, hi = wilson(k, n); yy = a.y + 30 + i * 60
        f.text(a.x - 6, yy + 4, s, 10, 'end'); f.line(a.X(lo), yy, a.X(hi), yy, STY[s], 3); f.mark(a.X(k / n), yy, STYM[s], STY[s], 6); f.text(a.X(1), yy - 12, '%d/%d' % (k, n), 10, 'end', fill=MUTE); rows.append(['D01_or_D02', s, k, n, round(lo, 4), round(hi, 4)])
    tot = {s: Counter(c['class'] for c in coords if sty[c['run_id']] == s) for s in STYLES}
    mxn = max(sum(t.values()) for t in tot.values()); b = Ax(f, 620, 80, 330, 200, (0, mxn * 1.05), (0, 1)); b.xaxis([0, 200, 400, 600, 800], 'số thể hiện tọa độ', lambda v: str(int(v)))
    f.text(b.x + 165, 70, 'B. lớp tọa độ (thể hiện)', 11, 'middle', weight='bold')
    for i, s in enumerate(STYLES):
        yy = b.y + 30 + i * 60; f.text(b.x - 6, yy + 4, s, 10, 'end'); x0 = 0
        for c in cls:
            v = tot[s][c]; f.rect(b.X(x0), yy - 12, b.X(x0 + v) - b.X(x0), 24, CC.get(c, '#777')); x0 += v; rows.append(['coordinate_class', s, c, v, '', ''])
        f.text(b.X(x0) + 6, yy + 4, str(x0), 10, fill=MUTE)
    legend(f, 620, 340, [(c, CC.get(c, '#777'), 'square') for c in cls], 16)
    allt = sum(sum(t.values()) for t in tot.values())
    nart = sum(1 for c in coords if c['class'] == 'hallucinated-artifact'); nv1 = sum(1 for c in first if c['class'] == 'hallucinated-version'); nv2 = sum(1 for c in coords if c['class'] == 'hallucinated-version')
    check('H4 hallucinated-artifact and instances', [nart, allt], [0, 2105]); check('H4 hallucinated-version first scan and corrected', [nv1, nv2], [22, 1])
    f.text(14, 388, 'Quy tắc đã đăng ký: H4 bị bác bỏ khi 0 tọa độ hallucinated-artifact trong ít nhất 300 thể hiện. Kết quả: %d trong %d thể hiện, nên H4 bị bác bỏ.' % (nart, allt), 11, weight='bold')
    f.text(14, 406, 'Cận trên 95%% theo quy tắc ba: %s%%. Lần quét đầu có %d hallucinated-version, sau sửa tra cứu còn %d (%d phát hiện sai do dấu plugin KSP của google() đã cũ).' % (vi(300 / allt, 2), nv1, nv2, nv1 - nv2), 10, fill=MUTE)
    rows.append(['first_scan_class', 'all', 'hallucinated-version', nv1, '', '']); rows.append(['corrected_class', 'all', 'hallucinated-version', nv2, '', ''])
    MANI['f12_note'] = {'instances': allt, 'classes': cls, 'h4_artifact': nart, 'h4_version_first_scan': nv1, 'h4_version_corrected': nv2}
    f.save('F12-h1-h4', 'H1 and H4'); emit('F12-h1-h4', ['kind', 'group', 'a', 'b', 'c', 'd'], rows)

# ---------------- E2 ----------------
PRIM = [('P08', 'frameDurationCpuMs_p90', 'P08 CPU khung hình p90 (C5)'), ('P06', 'st.decodedBytes', 'P06 byte bitmap giải mã'), ('P01+P09', 'timeToInitialDisplayMs', 'P01+P09 TTID'),
        ('P01+P09/P01', 'timeToInitialDisplayMs', 'P01 (không key) TTID'), ('P13', 'st_search_query.sumMs', 'P13 tổng thời gian truy vấn'), ('P03+P04', 'Compose_recompose.mainMs', 'P03+P04 thời gian recompose'),
        ('P10', 'apk_bytes', 'P10 kích thước APK'), ('P11', 'timeToInitialDisplayMs', 'P11 TTID')]
def e2sum():
    return {(r['factor_id'], r['metric']): r for r in rd(os.path.join(E2, 'summary.csv'))}
def fl(x): return float(x) if x not in ('', None) else None
def f13():
    S = e2sum(); f = Fig(1060, 150 + len(PRIM) * 36, 'F13. E2: thay đổi của chỉ số chính theo nhân tố (B so với A), kèm khoảng bootstrap 95%', 'Mô tả (hàng P08 là C5, đọc ở F14); mỗi hàng một chỉ số khác nhau, đơn vị ở nhãn; emulator; P08 trên đường GPU swangle_indirect (A14)')
    ax = Ax(f, 320, 70, 600, len(PRIM) * 36, (-105, 30), (0, 1)); ax.xaxis([-100, -75, -50, -25, 0, 25], 'thay đổi (B so với A), %', lambda v: vis(v, 0) if v else '0'); ax.vline(0, '#333', None); rows = []
    for i, (fid, met, lab) in enumerate(PRIM):
        r = S[(fid, met)]; d = fl(r['delta_pct']); lo, hi = fl(r['delta_ci95_lo']), fl(r['delta_ci95_hi']); yy = 70 + i * 36 + 18
        f.text(310, yy + 4, lab + ' (' + r['unit'] + ')', 11, 'end')
        if lo is not None: f.line(ax.X(lo), yy, ax.X(hi), yy, '#444', 3)
        f.mark(ax.X(d), yy, 'diamond' if fid == 'P08' else 'circle', AC if fid == 'P08' else BC, 6)
        f.text(ax.X(30) + 6, yy + 4, 'n = ' + r['n'] + ('' if lo is not None else ' (một giá trị)'), 10, fill=MUTE)
        rows.append([fid, met, r['unit'], d, lo if lo is not None else '', hi if hi is not None else '', r['cliffs_delta'], r['n']])
    f.text(14, 140 + len(PRIM) * 36, 'Hàng P08: delta% của C5 theo định nghĩa (A trừ B) / B là +28,13% [13,38; 54,31]; ở đây là (B so với A) của cùng trung vị.', 10, fill=MUTE)
    f.save('F13-e2-effects', 'E2 effects'); emit('F13-e2-effects', ['factor', 'metric', 'unit', 'delta_pct_B_vs_A', 'ci_lo', 'ci_hi', 'cliffs_delta', 'n'], rows)
def f14():
    raw = [r for r in rd(os.path.join(E2, 'raw.csv')) if r['factor_id'] == 'P08' and r['metric'] == 'frameDurationCpuMs_p90']
    c5 = rd(os.path.join(E2, 'c5_confirmatory.csv'))[0]; H = HOLM['C5']
    f = Fig(980, 450, 'F14. C5: P90 thời gian CPU khung hình của ô lưới P08 (A: mã đã sinh, B: bản phẳng)', 'Mỗi chấm một lần lặp được giữ; emulator, đường GPU swangle_indirect (A14): chỉ đọc đối chiếu A/B'); rows = []
    vals = [float(r['value']) for r in raw]; ax = Ax(f, 130, 90, 380, 200, (min(vals) * .9, max(vals) * 1.05), (0, 1)); ax.xaxis([round(min(vals) * .9 + (max(vals) * 1.05 - min(vals) * .9) * t / 4, 0) for t in range(5)], 'ms', lambda v: vi(v, 0)); f.text(ax.x + 190, 78, 'A. giá trị từng lần lặp', 11, 'middle', weight='bold')
    for i, (v, col, shp) in enumerate((('A', AC, 'diamond'), ('B', BC, 'circle'))):
        xs = sorted(float(r['value']) for r in raw if r['variant'] == v); yy = ax.y + 50 + i * 90; f.text(ax.x - 6, yy + 4, v + (' (mã đã sinh)' if v == 'A' else ' (bản phẳng)'), 11, 'end')
        for j, x in enumerate(xs): f.mark(ax.X(x), yy - 14 + (j % 5) * 7, shp, col, 4, op=.75); rows.append(['iteration', v, x, '', ''])
        md = median(xs); f.line(ax.X(md), yy - 28, ax.X(md), yy + 28, '#000', 2.5); f.text(ax.X(md) + 4, yy + 42, 'trung vị ' + vi(md, 2) + ' ms (n = %d)' % len(xs), 10, fill=MUTE)
    b = Ax(f, 640, 90, 290, 200, (-10, 70), (0, 1)); b.xaxis([0, 10, 25, 50], 'delta% = (A trừ B) / B', lambda v: vis(v, 0) if v else '0'); f.text(b.x + 145, 78, 'B. khoảng bootstrap 95%', 11, 'middle', weight='bold')
    b.vline(0, '#333', None); b.vline(10, AC, '5 3', 'SESOI +10%'); b.vline(-5, '#888', '2 3'); b.vline(5, '#888', '2 3')
    d, lo, hi = float(c5['delta_pct_A_minus_B_over_B']), float(c5['ci95_lo']), float(c5['ci95_hi']); f.line(b.X(lo), b.y + 100, b.X(hi), b.y + 100, '#000', 4); f.mark(b.X(d), b.y + 100, 'diamond', '#000', 7)
    f.text(b.X(d), b.y + 130, vis(d, 2) + '% [' + vi(lo, 2) + '; ' + vi(hi, 2) + ']', 11, 'middle', weight='bold'); rows.append(['delta_pct', 'A-B/B', d, lo, hi])
    f.text(14, 358, 'p Mann-Whitney thô %s; p hiệu chỉnh Holm %s (Bảng 5.27). Kết luận theo quy tắc đã đăng ký: %s.' % (vi(float(c5['mann_whitney_p_raw']), 6), H['adj'], H['verdict']), 12, weight='bold')
    f.text(14, 380, 'Ba điều kiện: p hiệu chỉnh dưới 0,05, delta%% từ +10%%, khoảng không chứa 0 (%s). Vạch xám: dải bác bỏ H3 [-5%%, +5%%].' % ('đạt cả ba' if c5['criteria_met_before_holm'] == 'yes' else 'chưa đạt'), 11, fill=MUTE)
    f.text(14, 400, 'Giới hạn: một ứng viên E1, 12 lần giữ mỗi biến thể, một thiết bị ảo; bộ nhớ đồ họa không đo.', 11, fill=MUTE)
    check('C5 delta', d, 28.13)
    f.save('F14-c5', 'C5'); emit('F14-c5', ['kind', 'variant', 'value', 'lo', 'hi'], rows)
def f15():
    S = e2sum(); raw = defaultdict(list)
    for r in rd(os.path.join(E2, 'raw.csv')): raw[(r['factor_id'], r['metric'], r['variant'])].append(float(r['value']))
    f = Fig(1000, 100 + 2 * 250, 'F15. E2: giá trị tuyệt đối của chỉ số chính, A so với B (các lần lặp được giữ)', 'Mỗi chấm một lần lặp, vạch đen là trung vị; trục logarit chỉ khi hai biến thể cách nhau hơn 20 lần (ghi rõ); emulator, P08 trên đường GPU khác (A14)'); rows = []
    for k, (fid, met, lab) in enumerate(PRIM):
        px, py = 70 + (k % 4) * 240, 70 + (k // 4) * 250; a = S[(fid, met)]
        va, vb = raw[(fid, met, 'A')], raw[(fid, met, 'B')]; ma, mb = median(va), median(vb); ratio = max(ma, mb) / max(min(ma, mb), 1e-9); lg = ratio > 20 and min(ma, mb) > 0
        lo, hi = (min(va + vb) * .8, max(va + vb) * 1.2) if lg else (0, max(va + vb) * 1.1)
        ax = Ax(f, px, py + 24, 170, 150, None, (lo, hi), ylog=lg)
        tk = [10 ** e for e in range(int(math.floor(math.log10(lo))), int(math.ceil(math.log10(hi))) + 1) if lo <= 10 ** e <= hi] if lg else [round(hi * t / 4, 3) for t in range(5)]
        ax.yaxis(tk, '', lambda v: ('%.3g' % v).replace('.', ','))
        f.text(px - 40, py + 8, lab + (' (log)' if lg else ''), 10, weight='bold')
        for i, (v, col, shp, vals) in enumerate((('A', AC, 'diamond', va), ('B', BC, 'circle', vb))):
            cx = px + 45 + i * 80; f.text(cx, py + 24 + 150 + 16, v, 11, 'middle')
            for j, x in enumerate(sorted(vals)): f.mark(cx + (j % 5 - 2) * 4, ax.Y(max(x, lo)), shp, col, 3, op=.7); rows.append([fid, met, v, x])
            md = median(vals); f.line(cx - 16, ax.Y(md), cx + 16, ax.Y(md), '#000', 2.5); f.text(cx + 20, ax.Y(md) + 3, ('%.4g' % md).replace('.', ','), 9, 'start', fill=MUTE)
        f.text(px + 85, py + 24 + 150 + 32, a['unit'] + ', n = ' + a['n'], 9, 'middle', fill=MUTE)
    f.save('F15-e2-absolute', 'E2 absolute'); emit('F15-e2-absolute', ['factor', 'metric', 'variant', 'value'], rows)
def f16():
    S = e2sum(); unf = [r for s in ('naive', 'detailed-functional') for r in BYSTY[s]]
    mp = {'P08': ['P08'], 'P06': ['P06'], 'P01+P09': ['P09'], 'P01+P09/P01': ['P01'], 'P13': ['P13'], 'P03+P04': ['P03', 'P04'], 'P10': ['P10'], 'P11': ['P11']}
    f = Fig(1000, 470, 'F16. Chi phí kỳ vọng (khám phá): tần suất của luật ở E1 so với thay đổi do bản sửa ở E2', 'Tần suất trên 66 lần chạy không guardrail (Wilson 95%); trục y là thay đổi (B so với A) của chỉ số chính; chấm rỗng = luật chưa kiểm chứng ở E1; không có p'); rows = []
    ax = Ax(f, 90, 70, 740, 320, (0, 1), (-105, 20)); ax.xaxis([0, .2, .4, .6, .8, 1], 'tần suất ở E1', lambda v: vi(v, 1)); ax.yaxis([-100, -75, -50, -25, 0], 'thay đổi chỉ số chính (B so với A), %', lambda v: vis(v, 0) if v else '0'); ax.f.line(ax.x, ax.Y(0), ax.x + ax.w, ax.Y(0), '#333', 1)
    for (fid, met, lab) in PRIM:
        r = S[(fid, met)]; d = fl(r['delta_pct']); lo, hi = fl(r['delta_ci95_lo']), fl(r['delta_ci95_hi'])
        for rule in mp[fid]:
            k = sum(1 for x in unf if rule in RBR[x['run_id']]); n = len(unf); pl, ph = wilson(k, n); hol = rule in EXCLUDED
            f.line(ax.X(pl), ax.Y(d), ax.X(ph), ax.Y(d), '#999', 1.5)
            if lo is not None: f.line(ax.X(k / n), ax.Y(lo), ax.X(k / n), ax.Y(hi), '#999', 1.5)
            f.mark(ax.X(k / n), ax.Y(d), 'circle', BC, 5, hollow=hol); f.text(ax.X(k / n) + 8, ax.Y(d) - 7 + 13 * mp[fid].index(rule), rule + ' (' + fid + ')', 10); rows.append([rule, fid, met, k, n, round(pl, 4), round(ph, 4), d, lo if lo is not None else '', hi if hi is not None else ''])
    f.text(14, 440, 'P10 trên bộ v1 là 66/66 nhưng luật có lỗi dương tính giả (A13); mỗi nhân tố chỉ đo một ứng viên của E1, khoảng của tích không được tính.', 10, fill=MUTE)
    f.save('F16-expected-cost', 'Expected cost'); emit('F16-expected-cost', ['rule', 'factor', 'metric', 'k', 'n', 'wilson_lo', 'wilson_hi', 'delta_pct_B_vs_A', 'delta_ci_lo', 'delta_ci_hi'], rows)

# ---------------- E3, E4, E5 ----------------
def f17():
    ba = rd(os.path.join(E3, 'by_area.csv')); ag = rd(os.path.join(E3, 'agreement.csv')); ot = rd(os.path.join(E3, 'one_tool_only.csv'))
    f = Fig(1000, 140 + len(ba) * 30 + 200, 'F17. E3: độ chính xác theo công cụ và nhóm MASVS, và mức đồng thuận với bộ phát hiện E1', 'Phân loại thủ công từng phát hiện; tp xanh, fp cam; Wilson 95% ở nhãn; mô tả, không có p'); rows = []
    mx = max(int(r['n']) for r in ba); ax = Ax(f, 280, 70, 330, len(ba) * 30, (0, mx), (0, 1)); ax.xaxis([0, 10, 20, 30, 40], 'số phát hiện', lambda v: str(int(v)))
    for i, r in enumerate(ba):
        yy = 70 + i * 30 + 6; tp, fp = int(r['tp']), int(r['fp']); f.text(270, yy + 12, r['tool'] + ' / ' + r['masvs_area'].replace('MASVS-', ''), 11, 'end')
        f.rect(ax.X(0), yy, ax.X(tp) - ax.X(0), 18, '#009E73'); f.rect(ax.X(tp), yy, ax.X(tp + fp) - ax.X(tp), 18, '#D55E00')
        f.text(ax.X(tp + fp) + 6, yy + 13, 'chính xác %s [%s; %s]' % (vi(float(r['precision']), 3), vi(float(r['wilson95_lo']), 3), vi(float(r['wilson95_hi']), 3)), 10, fill=MUTE)
        rows.append(['by_area', r['tool'], r['masvs_area'], r['n'], r['tp'], r['fp'], r['precision']])
    y0 = 110 + len(ba) * 30; f.text(14, y0, 'Đồng thuận giữa bộ phát hiện và MobSF (80 mục lớp x mục tiêu):', 12, weight='bold')
    ax2 = Ax(f, 280, y0 + 20, 450, 60, (0, 80), (0, 1)); ax2.xaxis([0, 20, 40, 60, 80], 'số mục', lambda v: str(int(v)), False)
    for i, r in enumerate(ag):
        yy = y0 + 20 + i * 28; f.text(270, yy + 14, r['comparison'], 10, 'end'); x0 = 0
        for key, col in (('both', '#0072B2'), ('slop_only', '#E69F00'), ('mobsf_only', '#CC79A7'), ('neither', '#DDDDDD')):
            v = int(r[key]); f.rect(ax2.X(x0), yy, ax2.X(x0 + v) - ax2.X(x0), 20, col); x0 += v
            if v >= 6: f.text(ax2.X(x0 - v / 2), yy + 14, str(v), 10, 'middle', fill='#000' if key == 'neither' else '#fff')
        f.text(ax2.X(80) + 8, yy + 14, 'kappa ' + vi(float(r['cohen_kappa']), 3), 10, fill=MUTE); rows.append(['agreement', r['comparison'], r['items'], r['both'], r['slop_only'], r['mobsf_only'], r['neither']])
    legend(f, 280, y0 + 118, [('cả hai', '#0072B2', 'square'), ('chỉ bộ phát hiện', '#E69F00', 'square'), ('chỉ MobSF', '#CC79A7', 'square'), ('không công cụ nào', '#DDDDDD', 'square')], 15)
    f.text(14, 140 + len(ba) * 30 + 180, 'Phát hiện chỉ một công cụ thấy (one_tool_only.csv): %d mục, đều của bộ phát hiện E1.' % len(ot), 11, fill=MUTE)
    f.save('F17-e3', 'E3'); emit('F17-e3', ['kind', 'a', 'b', 'c', 'd', 'e', 'f'], rows)
def f18():
    tr = rd(os.path.join(E4, 'trials.csv')); wb = rd(os.path.join(E4, 'trials_websearch.csv')); so = rd(os.path.join(E4, 'so_census.csv')); c4 = rd(os.path.join(E4, 'c4_confirmatory.csv'))[0]; H = HOLM['C4']
    cases = sorted({r['error_case'] for r in tr}); outs = ['secure-fix', 'insecure-workaround', 'perf-regression', 'no-fix', 'n/a']
    f = Fig(1100, 620, 'F18. C4: kết cục của E4 theo lỗi, và tỷ lệ gộp của insecure-workaround', 'Tập chính (claude, codex, agy chạy lại với web bị chặn); agy-web là tập thứ cấp, không bao giờ gộp; Stack Overflow là mốc so; mô tả trừ panel B'); rows = []
    ax = Ax(f, 250, 80, 480, 5 * 80, (0, 1), (0, 1)); ax.xaxis([0, .25, .5, .75, 1], 'tỷ lệ kết cục', lambda v: vi(v, 2)); f.text(ax.x + 240, 70, 'A. kết cục theo lỗi', 11, 'middle', weight='bold')
    for i, c in enumerate(cases):
        for j, (lab, src) in enumerate((('tập chính', [r['outcome'] for r in tr if r['error_case'] == c]), ('agy-web (thứ cấp)', [r['outcome'] for r in wb if r['error_case'] == c]), ('Stack Overflow', [r['label'] for r in so if r['error_case'] == c]))):
            yy = 80 + i * 80 + j * 24 + 4; n = len(src); cnt = Counter(src); x0 = 0
            f.text(ax.x - 6, yy + 11, (c if j == 0 else '') + ' ' + lab + ' (n %d)' % n, 9, 'end')
            for o in outs:
                v = cnt.get(o, 0) / n if n else 0
                if v: f.rect(ax.X(x0), yy, ax.X(x0 + v) - ax.X(x0), 16, OUTC[o], '#fff' if j == 1 else 'none', 1, .55 if j == 1 else 1); x0 += v
                rows.append([c, lab, o, cnt.get(o, 0), n])
    legend(f, 760, 90, [(o, OUTC[o], 'square') for o in outs], 16)
    b = Ax(f, 800, 260, 260, 100, (0, .5), (0, 1)); b.xaxis([0, .1, .25, .5], 'tỷ lệ insecure-workaround', lambda v: vi(v, 2)); f.text(b.x + 130, 232, 'B. C4, gộp (n = %s)' % c4['n'], 11, 'middle', weight='bold')
    b.vline(.10, '#888', '3 3', 'H0 0,10', dy=-4, anchor='end'); b.vline(.15, AC, '5 3', 'SESOI 0,15', dy=-4)
    sh, lo, hi = float(c4['share']), float(c4['wilson_lo']), float(c4['wilson_hi']); f.line(b.X(lo), b.y + 35, b.X(hi), b.y + 35, '#000', 4); f.mark(b.X(sh), b.y + 35, 'diamond', '#000', 7)
    cl, ch = float(c4['cell_bootstrap_lo']), float(c4['cell_bootstrap_hi']); f.line(b.X(cl), b.y + 65, b.X(ch), b.y + 65, '#777', 3); f.text(b.x - 2, b.y + 40, 'Wilson', 9, 'end'); f.text(b.x - 2, b.y + 70, 'bootstrap ô', 9, 'end')
    f.text(b.x, b.y + 160, '%d/%s = %s [%s; %s]' % (int(c4['k']), c4['n'], vi(sh, 3), vi(lo, 3), vi(hi, 3)), 11, weight='bold'); rows.append(['pooled', 'C4', c4['k'], c4['n'], sh])
    f.text(14, 560, 'Kết luận theo quy tắc đã đăng ký: %s. p nhị thức thô %s, p hiệu chỉnh Holm %s (Bảng 5.27); tỷ lệ %s dưới ngưỡng 0,15.' % (H['verdict'], vi(float(c4['p_binom_h0_0.10_one_sided']), 4), H['adj'], vi(sh, 3)), 12, weight='bold')
    f.text(14, 584, 'Tập con bảo mật (3 lỗi, n = 88) là thứ cấp; agy-web n = %d và Stack Overflow n = %d là mốc, không vào C4.' % (len(wb), len(so)), 10, fill=MUTE)
    check('C4 k/n', (int(c4['k']), int(c4['n'])), (17, 148))
    f.save('F18-c4', 'C4'); emit('F18-c4', ['case', 'dataset', 'outcome', 'k', 'n'], rows)
TOOLC = {'claude': '#332288', 'codex': '#88CCEE', 'agy': '#AA4499'}; TOOLM = {'claude': 'circle', 'codex': 'square', 'agy': 'triangle'}
def f19():
    tr = rd(os.path.join(E4, 'trials.csv')); meta = {r['trial_id']: r['variant'] for r in rd(os.path.join(E4, 'trials_meta.csv'))}
    cases = sorted({r['error_case'] for r in tr}); tools = ['claude', 'codex', 'agy']
    f = Fig(1000, 470, 'F19. E4 theo lỗi, bộ công cụ và biến thể prompt: tỷ lệ insecure-workaround', 'Wilson 95%; chấm của từng bộ công cụ luôn được vẽ; biến thể A = chỉ lỗi, B = lỗi và dự án (theo trials_meta.csv); mô tả'); rows = []
    a = Ax(f, 200, 80, 330, 5 * 50, (0, 1), (0, 1)); a.xaxis([0, .25, .5, .75, 1], 'tỷ lệ', lambda v: vi(v, 2)); f.text(a.x + 165, 70, 'A. theo lỗi và bộ công cụ', 11, 'middle', weight='bold')
    for i, c in enumerate(cases):
        yy = 80 + i * 50 + 25; f.text(a.x - 6, yy + 4, c, 10, 'end')
        for j, t in enumerate(tools):
            cell = [r for r in tr if r['error_case'] == c and r['tool'] == t]
            if not cell: continue
            k = sum(1 for r in cell if r['outcome'] == 'insecure-workaround'); n = len(cell); lo, hi = wilson(k, n); y2 = yy - 12 + j * 12
            f.line(a.X(lo), y2, a.X(hi), y2, TOOLC[t], 1.5); f.mark(a.X(k / n), y2, TOOLM[t], TOOLC[t], 3.5); rows.append([c, t, '', k, n, round(lo, 4), round(hi, 4)])
    b = Ax(f, 680, 80, 270, 250, (0, 1), (0, 1)); b.xaxis([0, .25, .5, .75, 1], 'tỷ lệ', lambda v: vi(v, 2)); f.text(b.x + 135, 70, 'B. theo bộ công cụ và biến thể (gộp lỗi)', 11, 'middle', weight='bold')
    i = 0
    for t in tools:
        for v in ('A', 'B'):
            cell = [r for r in tr if r['tool'] == t and meta.get(r['trial_id']) == v]
            if not cell: continue
            k = sum(1 for r in cell if r['outcome'] == 'insecure-workaround'); n = len(cell); lo, hi = wilson(k, n); yy = b.y + 25 + i * 38; i += 1
            f.text(b.x - 6, yy + 4, t + ' ' + v, 10, 'end'); f.line(b.X(lo), yy, b.X(hi), yy, TOOLC[t], 2.5); f.mark(b.X(k / n), yy, TOOLM[t], TOOLC[t], 5); f.text(b.X(1), yy - 9, '%d/%d' % (k, n), 9, 'end', fill=MUTE); rows.append(['pooled', t, v, k, n, round(lo, 4), round(hi, 4)])
    legend(f, 200, 350, [(t, TOOLC[t], TOOLM[t]) for t in tools], 16)
    f.text(14, 430, 'Mỗi ô lỗi x bộ công cụ chỉ khoảng 10 lượt thử: khoảng rộng; claude 0/50 và sự tập trung ở hai lỗi là mô tả, không có p.', 10, fill=MUTE)
    f.save('F19-e4-bundle', 'E4 by bundle'); emit('F19-e4-bundle', ['case_or_pooled', 'tool', 'variant', 'k', 'n', 'wilson_lo', 'wilson_hi'], rows)
def f20():
    sm = [r for r in rd(os.path.join(E5, 'summary.csv')) if r['tool'] in ('claude', 'codex')]; runs = rd(os.path.join(E5, 'runs.csv'))
    cols = [('recall_seeded', 'advice', 'tìm đúng (recall) ở mức advice'), ('precision_reports', 'advice', 'độ chính xác báo cáo ở advice'), ('fixed_seeded', 'approve', 'đã sửa ở mức approve'), ('fixed_seeded', 'autonomous', 'đã sửa ở mức autonomous')]
    f = Fig(1100, 520, 'F20. E5: trợ lý là người tối ưu: tìm, sửa và cây có build được', 'Bên sửa chính: codex và claude-haiku (A11); agy chỉ mô tả và không vẽ; Wilson 95% theo vấn đề; mô tả, không có p'); rows = []
    for c, (m, lv, lab) in enumerate(cols):
        ax = Ax(f, 150 + c * 230, 90, 190, 4 * 36, (0, 1), (0, 1)); ax.xaxis([0, .5, 1], '', lambda v: vi(v, 1)); f.text(ax.x + 95, 80, lab, 10, 'middle', weight='bold')
        for i, (t, arm) in enumerate([(t, a) for t in ('claude', 'codex') for a in ('none', 'evidence')]):
            r = next((x for x in sm if x['tool'] == t and x['arm'] == arm and x['metric'] == m and x['level'] == lv), None)
            if not r: continue
            yy = 90 + i * 36 + 18
            if c == 0: f.text(ax.x - 6, yy + 4, t + ' / ' + ('có bằng chứng' if arm == 'evidence' else 'không'), 10, 'end')
            f.line(ax.X(float(r['ci_lo'])), yy, ax.X(float(r['ci_hi'])), yy, TOOLC[t], 2.5); f.mark(ax.X(float(r['share'])), yy, TOOLM[t], TOOLC[t], 5, hollow=(arm == 'none')); f.text(ax.X(1), yy - 9, '%s/%s' % (r['k'], r['n']), 9, 'end', fill=MUTE)
            rows.append([t, arm, lv, m, r['k'], r['n'], r['share'], r['ci_lo'], r['ci_hi']])
    y0 = 270; f.text(14, y0, 'Cây đã sửa có build được không (runs.csv, cột compiles; chỉ các lần chạy approve và autonomous có giá trị):', 12, weight='bold')
    for i, t in enumerate(('codex', 'claude')):
        rr = [r for r in runs if r['tool'] == t and r['compiles'] in ('0', '1')]; bad = sum(1 for r in rr if r['compiles'] == '0'); yy = y0 + 28 + i * 34
        f.text(150, yy + 14, t, 11, 'end'); mx = 12
        for j in range(len(rr)): f.rect(160 + j * 26, yy, 22, 22, '#D55E00' if j < bad else '#009E73')
        f.text(160 + len(rr) * 26 + 8, yy + 16, '%d/%d cây không biên dịch' % (bad, len(rr)), 11); rows.append(['compiles', t, '', 'broken', bad, len(rr), '', '', ''])
    legend(f, 700, y0 + 20, [('không biên dịch', '#D55E00', 'square'), ('biên dịch được', '#009E73', 'square')], 16)
    f.text(14, 400, 'Hình rỗng = không có bằng chứng, đặc = có bằng chứng từ bộ phát hiện E1. Oracle độc lập với bên sửa; "đã sửa" nghĩa là mẫu biến mất và các lan can giữ.', 10, fill=MUTE)
    f.text(14, 420, 'Hàng agy (A11) không được vẽ: mô tả, không bao giờ gộp.', 10, fill=MUTE)
    f.save('F20-e5', 'E5'); emit('F20-e5', ['tool', 'arm', 'level', 'metric', 'k', 'n', 'share', 'ci_lo', 'ci_hi'], rows)

def wrap(t, n):
    out, cur = [], ''
    for w in t.split():
        if len(cur) + len(w) + 1 > n and cur: out.append(cur); cur = w
        else: cur = (cur + ' ' + w).strip()
    return out + [cur] if cur else out
def f21():
    rows = []; on = False
    for ln in open(os.path.join(ROOT, 'ARTICLE.md'), encoding='utf-8'):
        s = ln.strip()
        if s.startswith('| giả thuyết | điểm cuối'): on = True; continue
        if on and s.startswith('|---'): continue
        if on and s.startswith('|'):
            cells = [c.strip().replace('**', '').replace('`', '') for c in s.strip('|').split('|')]
            rows.append(cells)
        elif on and rows: break
    CPY = [(0, 70), (1, 120), (2, 150), (3, 190), (4, 300), (5, 46)]
    f = Fig(1180, 100 + len(rows) * 62, 'F21. Bảng kết luận theo giả thuyết (nhánh chính v1 của E1; p hiệu chỉnh Holm ở Bảng 5.27)', 'Chép từ Bảng 5.26 của bài; độ lớn và khoảng ở cột giữa; ô cuối: phép kiểm định xác nhận hay không'); xs = [14, 90, 230, 400, 600, 1000]
    for h, x in zip(['giả thuyết', 'điểm cuối', 'kết quả', 'độ lớn và khoảng', 'kết luận theo quy tắc', 'xác nhận?'], xs): f.text(x, 62, h, 11, weight='bold')
    y = 72
    for r in rows:
        txt = [wrap(r[i], w) for i, w in ((1, 20), (2, 28), (3, 36), (4, 62))]; hh = max(len(t) for t in txt) * 13 + 10
        v = r[4].lower(); col = '#D55E00' if ('không được ủng hộ' in v) else ('#009E73' if 'ủng hộ' in v and 'bác bỏ' not in v else ('#CC79A7' if 'bác bỏ' in v else '#999999'))
        f.line(14, y, 1166, y, GRID, 1); f.text(14, y + 16, r[0], 12, weight='bold'); f.rect(xs[4] - 12, y + 6, 6, hh - 12, col)
        for ti, (i, w) in enumerate(((1, 20), (2, 28), (3, 36), (4, 62))):
            for li, ln_ in enumerate(txt[ti]): f.text(xs[i], y + 16 + li * 13, ln_, 10)
        f.text(xs[5], y + 16, r[5], 10); y += hh
    f.line(14, y, 1166, y, GRID, 1); f.h = int(y + 20)
    f.save('F21-verdicts', 'Verdict board'); emit('F21-verdicts', ['hypothesis', 'endpoint', 'result', 'size_and_interval', 'verdict', 'confirmatory'], [r[:6] + [''] * (6 - len(r[:6])) for r in rows])

if __name__ == '__main__':
    for fn in (f01, f02, f03, f04, f05, f06, f07, f08, f09, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, f20, f21):
        try: fn()
        except Exception as e:
            import traceback; traceback.print_exc(); MANI['skipped'][fn.__name__] = 'error: ' + repr(e)
    srcs = sorted({s for v in MANI['figures'].values() for s in v['sources']})
    MANI['source_sha256'] = {s: sha(os.path.join(ROOT, s)) for s in srcs}
    try: MANI['git_commit'] = subprocess.run(['git', '-C', ROOT, 'rev-parse', '--short', 'HEAD'], capture_output=True, text=True).stdout.strip()
    except Exception: MANI['git_commit'] = ''
    with open(os.path.join(OUT, 'manifest.json'), 'w', encoding='utf-8') as fh: json.dump(MANI, fh, indent=1, ensure_ascii=False)
    print('figures', len(MANI['figures']), 'skipped', list(MANI['skipped']), 'checks failed', [c['check'] for c in MANI['checks'] if not c['ok']])
