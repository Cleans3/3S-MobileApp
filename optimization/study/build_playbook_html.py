"""Build PLAYBOOK.html from PLAYBOOK.md (same look as ARTICLE.html).

Before rendering, the block between <!-- SCRIPT:START --> and <!-- SCRIPT:END --> in PLAYBOOK.md is
refilled from the per-slide sections of presentation-script.md (headings demoted to level 4/5 so they
stay out of the table of contents; the timing table, Q&A, checklist and number check are not copied).

Run:   python3 build_playbook_html.py           (needs markdown-it-py; without it:
       uv run --no-project --with markdown-it-py python3 build_playbook_html.py)
Check: python3 build_playbook_html.py --check   exit 1 if PLAYBOOK.md or PLAYBOOK.html is out of date
"""
import html, re, sys, unicodedata
from pathlib import Path
from markdown_it import MarkdownIt

here = Path(__file__).parent
START, END = '<!-- SCRIPT:START -->', '<!-- SCRIPT:END -->'


def script_block():
    lines = (here / 'presentation-script.md').read_text(encoding='utf-8').split('\n')
    first = next(i for i, l in enumerate(lines) if re.match(r'## 1\. ', l))
    last = next(i for i, l in enumerate(lines) if i > first and l.startswith('## ') and not re.match(r'## \d+\. ', l))
    body = '\n'.join(lines[first:last]).strip()
    body = re.sub(r'\n-{3,}\s*$', '', body).strip()  # separator before the Q&A section
    body = re.sub(r'^(#{2,4}) ', lambda m: '#' * (len(m.group(1)) + 2) + ' ', body, flags=re.M)
    return (f'{START}\n<!-- filled by build_playbook_html.py from presentation-script.md; edit the script, not this block -->\n\n'
            f'{body}\n\n{END}')


def sync(md):
    i, j = md.index(START), md.index(END) + len(END)
    return md[:i] + script_block() + md[j:]


def slug(t):
    t = unicodedata.normalize('NFD', re.sub('<[^>]+>', '', t).lower().replace('đ', 'd'))
    return re.sub('[^a-z0-9]+', '-', ''.join(c for c in t if not unicodedata.combining(c))).strip('-') or 'h'


def render(md):
    body = MarkdownIt('commonmark', {'html': True}).enable(['table', 'strikethrough']).render(md)
    css = re.search(r'<style>(.*?)</style>', (here / 'ARTICLE.html').read_text(encoding='utf-8'), re.S).group(1)
    toc, seen = [], {}

    def heading(m):
        level, text = m.group(1), m.group(2)
        sid = slug(text)
        seen[sid] = seen.get(sid, 0) + 1
        if seen[sid] > 1: sid += f'-{seen[sid]}'
        if level != '1': toc.append(f'<li class="l{level}"><a href="#{sid}">{re.sub("<[^>]+>", "", text)}</a></li>')
        return f'<h{level} id="{sid}">{text}</h{level}>'

    body = re.sub(r'<h([1-3])>(.*?)</h\1>', heading, body, flags=re.S)
    body = re.sub(r'href="(ARTICLE)\.md(#[^"]*)?"', lambda m: f'href="{m.group(1)}.html{m.group(2) or ""}"', body)
    title = re.search(r'^# (.+)$', md, re.M).group(1)
    extra = """
table{border-collapse:collapse;width:100%;font-size:14.5px;margin:1rem 0;display:block;overflow-x:auto}
th,td{border:1px solid var(--rule);padding:.35rem .55rem;vertical-align:top;text-align:left}
th{background:var(--tbl)}
code{font:13.5px/1.4 ui-monospace,"JetBrains Mono",monospace;background:var(--tbl);padding:.05rem .25rem;border-radius:3px}
pre{background:var(--tbl);padding:.8rem 1rem;overflow-x:auto;border-radius:4px}pre code{background:none;padding:0}
main{max-width:1100px}
h4{margin-top:1.6rem}
@media (max-width:900px){body{display:block}.toc{position:static;height:auto;border-right:0;border-bottom:1px solid var(--rule)}}
"""
    return f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)}</title><style>{css}{extra}</style></head><body>
<nav class="toc"><div class="tochead">Contents</div><ol>{''.join(toc)}</ol></nav>
<main>{body}</main></body></html>
"""


def main(argv):
    md_path, html_path = here / 'PLAYBOOK.md', here / 'PLAYBOOK.html'
    old_md = md_path.read_text(encoding='utf-8')
    new_md = sync(old_md)
    new_html = render(new_md)
    old_html = html_path.read_text(encoding='utf-8') if html_path.is_file() else ''
    if '--check' in argv:
        stale = [p.name for p, a, b in ((md_path, old_md, new_md), (html_path, old_html, new_html)) if a != b]
        print('out of date: ' + ', '.join(stale) if stale else 'up to date')
        return 1 if stale else 0
    for p, a, b in ((md_path, old_md, new_md), (html_path, old_html, new_html)):
        if a != b:
            p.write_text(b, encoding='utf-8')
            print(f'wrote {p.name}')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
