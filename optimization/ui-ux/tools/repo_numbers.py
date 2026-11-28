"""The numbers of the app as first cloned (tag `ui-baseline`) and now, taken from git and from the audit tables; prints Markdown.

usage: python repo_numbers.py <audit run dir of now> [--apk-before <file>] [--apk-now <file>]

Every figure here is counted by this script from the repository or from a table a run wrote; nothing is typed in. Measurements that need a
phone (frame times, memory) are not redone here: they are in `optimization/ui-ux-metrics.md` and `optimization/ui-ux/measure/`.
"""
import csv
import os
import re
import subprocess
import sys

BASE = 'ui-baseline'
HEAD = 'HEAD'
KT = 'android/app/src/main/kotlin'
RES = 'android/app/src/main/res'


def git(*a):
    r = subprocess.run(['git'] + list(a), capture_output=True, text=True, encoding='utf-8')
    return r.stdout


def files(rev, prefix, suffix):
    return [f for f in git('ls-tree', '-r', '--name-only', rev, prefix).split('\n') if f.endswith(suffix)]


def text(rev, path):
    return git('show', '%s:%s' % (rev, path))


def loc(rev, prefix, suffix):
    fs = files(rev, prefix, suffix)
    return len(fs), sum(len(text(rev, f).split('\n')) for f in fs)


def count(rev, prefix, suffix, pattern):
    return sum(len(re.findall(pattern, text(rev, f))) for f in files(rev, prefix, suffix))


SAME = {'catalog', 'detail', 'loans', 'reservations', 'account', 'checkout', 'login'}  # the seven screens the first audit (A) covered


def screens(folder, cfg):
    p = os.path.join(folder, 'screens-%s.tsv' % cfg)
    if not os.path.exists(p):
        return 0
    return len({r.split('\t')[1] for r in open(p, encoding='utf-8') if r.strip()})


def findings(folder, cfg, only=None):
    p = os.path.join(folder, 'findings-%s.tsv' % cfg)
    if not os.path.exists(p):
        return None
    with open(p, encoding='utf-8') as f:
        rows = [r for r in list(csv.reader(f, delimiter='\t'))[1:] if only is None or r[1] in only]
    by = {}
    for r in rows:
        by[r[2]] = by.get(r[2], 0) + 1
    return len(rows), by


def main():
    args = sys.argv[1:]
    apk_before = apk_now = None
    if '--apk-before' in args:
        i = args.index('--apk-before'); apk_before = args[i + 1]; del args[i:i + 2]
    if '--apk-now' in args:
        i = args.index('--apk-now'); apk_now = args[i + 1]; del args[i:i + 2]
    if len(args) != 1:
        raise SystemExit(__doc__)
    audit = args[0]
    rows = []
    kb, lb = loc(BASE, KT, '.kt'); kn, ln = loc(HEAD, KT, '.kt')
    rows.append(('Kotlin files in `android/app/src/main`', kb, kn))
    rows.append(('Kotlin lines in `android/app/src/main`', lb, ln))
    ub, ubl = loc(BASE, KT + '/shelftap/android/ui', '.kt'); un, unl = loc(HEAD, KT + '/shelftap/android/ui', '.kt')
    rows.append(('files in `ui/`', ub, un))
    rows.append(('hard-coded `Text("` in `ui/`', count(BASE, KT + '/shelftap/android/ui', '.kt', r'Text\("'), count(HEAD, KT + '/shelftap/android/ui', '.kt', r'Text\("')))
    rows.append(('strings in `values/strings.xml` (Vietnamese)', len(re.findall(r'<string |<plurals ', text(BASE, RES + '/values/strings.xml'))), len(re.findall(r'<string |<plurals ', text(HEAD, RES + '/values/strings.xml')))))
    rows.append(('strings in `values-en/strings.xml` (English)', len(re.findall(r'<string |<plurals ', text(BASE, RES + '/values-en/strings.xml'))), len(re.findall(r'<string |<plurals ', text(HEAD, RES + '/values-en/strings.xml')))))
    tb = count(BASE, 'android/app/src/test', '.kt', r'@Test'); tn = count(HEAD, 'android/app/src/test', '.kt', r'@Test')
    rows.append(('unit tests (`@Test` in `src/test`)', tb, tn))
    ab = count(BASE, 'android/app/src/androidTest', '.kt', r'@Test'); an = count(HEAD, 'android/app/src/androidTest', '.kt', r'@Test')
    rows.append(('instrumented tests (`@Test` in `src/androidTest`)', ab, an))
    sb = len(files(BASE, KT + '/shelftap/android/ui', '.kt')); sn = len(files(HEAD, KT + '/shelftap/android/ui', '.kt'))
    ib = len([1 for f in files(BASE, KT, '.kt') if 'Screen' in f]); inn = len([1 for f in files(HEAD, KT, '.kt') if 'Screen' in f])
    rows.append(('files named `*Screen*`', ib, inn))
    print('| What | First clone (`%s`) | Now (`HEAD`) |' % BASE)
    print('|---|---|---|')
    for k, a, b in rows:
        print('| %s | %s | %s |' % (k, a, b))
    if apk_before and apk_now:
        sb_, sn_ = os.path.getsize(apk_before), os.path.getsize(apk_now)
        print('| debug APK size | %.1f MB | %.1f MB |' % (sb_ / 1e6, sn_ / 1e6))
    print()
    print('Accessibility Test Framework findings (the same 13 checks; before = `audit/A`, now = %s). "Same 7 screens" counts only the screens the first audit covered, so the two columns can be compared; "all screens" is every screen the audit visits now:' % audit)
    print()
    print('| Configuration | First clone (7 screens) | Now, same 7 screens | Now, all screens |')
    print('|---|---|---|---|')
    pairs = [('light (720 x 1280 px, 280 dpi)', 'light', 'light'), ('dark', 'dark', 'dark'), ('font scale 2.0', 'font2', 'font2'), ('320 dp wide', 'w320', 'w320')]
    here = os.path.dirname(os.path.abspath(__file__))
    root = os.path.normpath(os.path.join(here, '..', 'audit'))
    for label, a_cfg, n_cfg in pairs:
        a = findings(os.path.join(root, 'A'), a_cfg)
        n7 = findings(audit, n_cfg, SAME)
        nall = findings(audit, n_cfg)
        fmt = lambda x: 'not run' if x is None else '%d (%s)' % (x[0], ', '.join('%s %d' % (k, v) for k, v in sorted(x[1].items())) or 'none')
        extra = ' on %d screens' % screens(audit, n_cfg) if nall is not None else ''
        print('| %s | %s | %s | %s%s |' % (label, fmt(a), fmt(n7), fmt(nall), extra))


if __name__ == '__main__':
    main()
