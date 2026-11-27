"""Render PLAYBOOK.md to PLAYBOOK.html (same look as ARTICLE.html). Run: python3 build_playbook_html.py"""
import html, re, unicodedata
from pathlib import Path
from markdown_it import MarkdownIt

here = Path(__file__).parent
md = (here / 'PLAYBOOK.md').read_text(encoding='utf-8')
body = MarkdownIt('commonmark', {'html': True}).enable(['table', 'strikethrough']).render(md)
css = re.search(r'<style>(.*?)</style>', (here / 'ARTICLE.html').read_text(encoding='utf-8'), re.S).group(1)

def slug(t):
    t = unicodedata.normalize('NFD', re.sub('<[^>]+>', '', t).lower().replace('đ', 'd'))
    return re.sub('[^a-z0-9]+', '-', ''.join(c for c in t if not unicodedata.combining(c))).strip('-') or 'h'

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
@media (max-width:900px){body{display:block}.toc{position:static;height:auto;border-right:0;border-bottom:1px solid var(--rule)}}
"""
(here / 'PLAYBOOK.html').write_text(f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)}</title><style>{css}{extra}</style></head><body>
<nav class="toc"><div class="tochead">Contents</div><ol>{''.join(toc)}</ol></nav>
<main>{body}</main></body></html>
""", encoding='utf-8')
