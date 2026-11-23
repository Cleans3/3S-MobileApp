
import re, sys, html, pathlib, markdown
from markdown.extensions.toc import slugify_unicode
src = pathlib.Path(sys.argv[1]); out = pathlib.Path(sys.argv[2]); base = src.parent
md = src.read_text(encoding='utf-8')

# title block: h1 + italic subtitle + scope line, rendered by hand
lines = md.split('\n')
title = lines[0].lstrip('# ').strip()
m = re.match(r'^# .*\n\n\*(.+?)\*\n\n(.+?)\n\n', md)
subtitle, scope = m.group(1), m.group(2)
md = md[m.end():]

def inline(s):
    h = markdown.markdown(s)
    return re.sub(r'^<p>|</p>$', '', h.strip())

figs = {}
def figrep(mm):
    num, cap, fid, path = mm.group(1), mm.group(2), mm.group(3), mm.group(4)
    svg = (base / path).read_text(encoding='utf-8')
    svg = re.sub(r'<\?xml[^>]*\?>', '', svg)
    svg = re.sub(r'<!DOCTYPE[^>]*>', '', svg)
    key = f'FIGPH{len(figs)}X'
    figs[key] = (f'<figure id="fig-{num.replace(".", "-")}"><div class="svgwrap">{svg}</div>'
                 f'<figcaption><b>Hình {num}.</b> {inline(cap)}</figcaption></figure>')
    return key
md = re.sub(r'^\*\*Hình ([\d.]+)\.\*\*\s*(.+?)\n\n!\[(F\d+)\]\(([^)]+\.svg)\)', figrep, md, flags=re.M)
left = re.findall(r'!\[F\d+\]', md)
assert not left, f'unmatched figures: {left}'

body = markdown.markdown(md, extensions=['tables', 'fenced_code', 'sane_lists', 'attr_list', 'footnotes',
                         'toc'], extension_configs={'toc': {'slugify': slugify_unicode, 'toc_depth': '2-3'}}, output_format='html5')
for k, v in figs.items():
    body = body.replace(f'<p>{k}</p>', v)
assert 'FIGPH' not in body
# table captions + scroll wrapper
body = re.sub(r'<p><strong>Bảng ([\d.]+)\.</strong>', r'<p class="tcap" id="tab-\1"><strong>Bảng \1.</strong>', body)
body = body.replace('<table>', '<div class="tablewrap"><table>').replace('</table>', '</table></div>')

# citation links: ids on reference entries, links on [Key] in text
refstart = body.find('>Tài liệu tham khảo</h2>'); refstart = body.rfind('<h2', 0, refstart) if refstart > 0 else -1
keys = set()
if refstart > 0:
    head, refs = body[:refstart], body[refstart:]
    def tag(mm):
        keys.add(mm.group(2)); return f'{mm.group(1)}<span class="refkey" id="ref-{mm.group(2)}">[{mm.group(2)}]</span>'
    refs = re.sub(r'(<(?:li|p)>(?:<strong>)?)\[([A-Za-z][\w.+-]*\d{2,4}[a-z]?|[A-Z][\w.+-]+)\]', tag, refs)
    def link(mm):
        return f'<a class="cite" href="#ref-{mm.group(1)}">[{mm.group(1)}]</a>' if mm.group(1) in keys else mm.group(0)
    # only outside tags/code: split on tags
    parts = re.split(r'(<code>.*?</code>|<[^>]+>)', head, flags=re.S)
    head = ''.join(p if p.startswith('<') else re.sub(r'\[([A-Za-z][\w.+-]*)\]', link, p) for p in parts)
    refs = re.sub(r'(?<![">])(https?://[^\s<;]+[^\s<;.,)])', r'<a href="\1">\1</a>', refs)
    body = head + refs

# toc from h2/h3
toc = []
for lvl, hid, txt in re.findall(r'<h([23]) id="([^"]+)">(.*?)</h\1>', body):
    t = re.sub(r'<[^>]+>', '', txt)
    toc.append(f'<li class="l{lvl}"><a href="#{hid}">{t}</a></li>')

css = pathlib.Path(sys.argv[3]).read_text(encoding='utf-8')
doc = f'''<!doctype html>
<html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)}</title><style>{css}</style></head>
<body>
<nav class="toc"><div class="tochead">Mục lục</div><ol>{''.join(toc)}</ol></nav>
<main>
<header class="paper">
<div class="kicker">Báo cáo nghiên cứu · 3S-MobileApp · Tối ưu ứng dụng di động</div>
<h1>{inline(title)}</h1>
<p class="subtitle">{inline(subtitle)}</p>
<p class="scope">{inline(scope)}</p>
<p class="meta">Bản thảo, 23/11/2026 · Giao thức đăng ký trước <code>00-protocol.md</code> v1.13</p>
</header>
{body}
</main></body></html>'''
out.write_text(doc, encoding='utf-8')
print('ok', len(doc), 'figs', len(figs), 'refkeys', len(keys), 'toc', len(toc), 'cites', body.count('class="cite"'))
