// Static WCAG contrast audit of the ShelfTap mockup boards, light and dark.
// Renders each .dc.html template with the board's own renderVals(), resolves CSS tokens,
// alpha-composites ancestor backgrounds, and checks every text run.
// Usage (from optimization/ui-ux-mockup): node tools/contrast.js .
// Writes contrast-results.json next to the boards and prints a summary plus each distinct failure.
// Text over the welcome image (Main) is not modelled here; see tools/hero-worst-case.js.
const fs = require('fs'), path = require('path');
const dir = process.argv[2];
global.setTimeout = () => 0; global.clearTimeout = () => {};
global.window = { matchMedia: () => ({ matches: false }) };

// ---------- colors ----------
const NAMED = { white: [255, 255, 255, 1], black: [0, 0, 0, 1], transparent: [0, 0, 0, 0] };
function parseColor(s) {
  if (!s) return null;
  s = s.trim().toLowerCase();
  if (NAMED[s]) return NAMED[s].slice();
  let m = s.match(/^#([0-9a-f]{3})$/);
  if (m) return [...m[1]].map(c => parseInt(c + c, 16)).concat(1);
  m = s.match(/^#([0-9a-f]{6})$/);
  if (m) return [0, 2, 4].map(i => parseInt(m[1].slice(i, i + 2), 16)).concat(1);
  m = s.match(/^rgba?\(([^)]+)\)$/);
  if (m) { const p = m[1].split(',').map(x => parseFloat(x)); return [p[0], p[1], p[2], p.length > 3 ? p[3] : 1]; }
  return null;
}
const over = (top, bot) => { const a = top[3]; return [0, 1, 2].map(i => top[i] * a + bot[i] * (1 - a)).concat(1); };
const lum = c => { const f = v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }; return 0.2126 * f(c[0]) + 0.7152 * f(c[1]) + 0.0722 * f(c[2]); };
const ratio = (a, b) => { const x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); };
const hex = c => '#' + c.slice(0, 3).map(v => Math.round(v).toString(16).padStart(2, '0')).join('');

// ---------- template parsing ----------
const VOID = new Set(['input', 'img', 'br', 'meta', 'link', 'hr']);
function parse(html) {
  const root = { tag: '#root', attrs: {}, children: [] }; const stack = [root];
  const re = /<(\/?)([a-zA-Z][\w-]*)((?:[^>"']|"[^"]*"|'[^']*')*)>|([^<]+)/g; let m;
  while ((m = re.exec(html))) {
    const top = stack[stack.length - 1];
    if (m[4] !== undefined) { if (m[4].trim()) top.children.push({ text: m[4] }); continue; }
    const tag = m[2].toLowerCase();
    if (m[1]) { while (stack.length > 1 && stack.pop().tag !== tag); continue; }
    const attrs = {}; const ar = /([\w:-]+)(?:="([^"]*)")?/g; let a;
    while ((a = ar.exec(m[3]))) attrs[a[1]] = a[2] === undefined ? '' : a[2];
    const node = { tag, attrs, children: [] }; top.children.push(node);
    if (!VOID.has(tag) && !m[3].trim().endsWith('/')) stack.push(node);
  }
  return root;
}
const get = (scope, expr) => expr.trim().split('.').reduce((o, k) => (o == null ? undefined : o[k]), scope);
const subst = (s, scope) => s.replace(/\{\{\s*([^}]+?)\s*\}\}/g, (_, e) => { if (e === 'true' || e === 'false') return e; const v = get(scope, e); return v == null ? '' : String(v); });

// expand sc-if / sc-for and substitute
function expand(node, scope) {
  if (node.text !== undefined) return [{ text: subst(node.text, scope) }];
  if (node.tag === 'sc-if') {
    const e = (node.attrs.value || '').replace(/^\{\{\s*|\s*\}\}$/g, '');
    return get(scope, e) ? node.children.flatMap(c => expand(c, scope)) : [];
  }
  if (node.tag === 'sc-for') {
    const list = get(scope, (node.attrs.list || '').replace(/^\{\{\s*|\s*\}\}$/g, '')) || [];
    return list.flatMap(item => node.children.flatMap(c => expand(c, Object.assign({}, scope, { [node.attrs.as]: item }))));
  }
  const attrs = {}; for (const k in node.attrs) attrs[k] = subst(node.attrs[k], scope);
  return [{ tag: node.tag, attrs, children: node.children.flatMap(c => expand(c, scope)) }];
}

// ---------- styles ----------
function styleMap(s) { const o = {}; (s || '').split(';').forEach(d => { const i = d.indexOf(':'); if (i > 0) o[d.slice(0, i).trim().toLowerCase()] = d.slice(i + 1).trim(); }); return o; }
function resolveVars(v, tokens) { let prev; do { prev = v; v = v.replace(/var\(--([\w-]+)\)/g, (_, n) => tokens[n] !== undefined ? tokens[n] : 'transparent'); } while (v !== prev); return v; }

function tokensOf(html) {
  const light = {}, dark = {};
  const lm = html.match(/\.st-root\{([^}]*)\}/), dm = html.match(/\.st-root\[data-theme="dark"\]\{([^}]*)\}/);
  for (const [m, o] of [[lm, light], [dm, dark]]) if (m) m[1].split(';').forEach(d => { const i = d.indexOf(':'); if (i > 0) o[d.slice(2, i).trim()] = d.slice(i + 1).trim(); });
  const linkColor = (html.match(/a\{color:([^}]+)\}/) || [])[1];
  return { light, dark, linkColor };
}

// Non-text items (WCAG 1.4.11, 3:1). `required` marks what the criterion asks for; the rest is advisory.
const colorIn = s => { const m = (s || '').match(/#[0-9a-fA-F]{6}\b|#[0-9a-fA-F]{3}\b|rgba?\([^)]*\)/); return m ? parseColor(m[0]) : null; };

function audit(file, theme, boardDir = dir) {
  const ui = [];
  const html = fs.readFileSync(path.join(boardDir, file), 'utf8');
  const js = html.split('data-dc-script')[1].split('>').slice(1).join('>').split('</script>')[0];
  const C = new Function('DCLogic', js + '\nreturn Component;')(class { setState(s) { this.state = Object.assign({}, this.state, s); } });
  const c = new C(); c.props = {}; c.state = { __theme: theme, loaded: true, done: true };
  const vals = c.renderVals();
  const { light, dark, linkColor } = tokensOf(html);
  const tokens = theme === 'dark' ? dark : light;
  const tpl = html.slice(html.indexOf('</helmet>') + 9, html.indexOf('</x-dc>'));
  const tree = { tag: '#root', attrs: {}, children: expand(parse(tpl), vals) };
  const out = [];
  function walk(node, ctx) {
    if (node.text !== undefined) {
      const t = node.text.replace(/\s+/g, ' ').trim();
      if (t && ctx.control) ctx.control.hasText = true;
      if (t && !ctx.hidden) out.push(Object.assign({ text: t }, ctx));
      return;
    }
    const rawStyle = node.attrs.style || '';
    const st = styleMap(resolveVars(rawStyle, tokens));
    const n = Object.assign({}, ctx);
    if (node.attrs['aria-hidden'] === 'true') n.hidden = true;
    // mockup-only chrome: the state preview bar and the light/dark button
    if (rawStyle.includes('rgba(28,27,26,0.82)') || /bản phác ở nền/.test(node.attrs['aria-label'] || '')) n.chrome = true;
    const parentBg = ctx.bg;
    const where = node.attrs['aria-label'] || '';
    if (node.tag === 'svg') {
      const own = st.color ? parseColor(st.color) : null;
      const pick = a => { const v = node.attrs[a]; if (!v || v === 'none') return null; return v.toLowerCase() === 'currentcolor' ? (own || n.color) : parseColor(v); };
      const ic = pick('stroke') || pick('fill');
      if (ic && !n.chrome) ui.push({ kind: 'icon', control: n.control || null, fg: ic[3] < 1 ? over(ic, n.bg) : ic, bg: n.bg, onGradient: !!n.onGradient, where: (n.control && n.control.label) || '' });
      return;
    }
    // UA defaults that do not inherit
    const bgv = st.background || st['background-color'];
    if (bgv) {
      if (/gradient|url\(/.test(bgv)) { n.onGradient = true; }
      else { const pb = parseColor(bgv); if (pb && pb[3] > 0) { n.bg = over(pb, n.bg); n.bgSrc = bgv; } }
    }
    const isControl = node.tag === 'button' || node.tag === 'a' || node.tag === 'input';
    if (node.tag === 'button' || node.tag === 'a') n.control = { label: where, hasText: false };
    const border = st.border && !/^0\b|none/.test(st.border) ? colorIn(st.border) : null;
    const borderC = border ? over(border, parentBg) : null;
    let fillVs = ratio(n.bg, parentBg), borderVs = borderC ? ratio(borderC, parentBg) : 0;
    // a search field drawn by its <label> pill: the input itself is transparent and borderless
    if (node.tag === 'label' && (bgv || border)) n.box = { fillVs, borderVs, fill: n.bg, borderC, outside: parentBg };
    if (node.tag === 'input' && (!bgv || bgv.trim() === 'transparent') && !border && ctx.box) { fillVs = ctx.box.fillVs; borderVs = ctx.box.borderVs; }
    if (isControl && !n.chrome && !n.hidden) {
      const role = node.attrs.role, pressed = node.attrs['aria-pressed'];
      let kind = 'boundary', required = false;
      if (node.tag === 'input') { kind = 'field'; required = true; }
      else if (role === 'switch') { kind = 'switch-track (' + (node.attrs['aria-checked'] === 'true' ? 'bật' : 'tắt') + ')'; required = true; }
      else if (pressed === 'true') { kind = 'selected'; required = true; }
      if (pressed !== 'false') ui.push({ kind, required, ratioOverride: Math.max(fillVs, borderVs), fg: (borderVs > fillVs ? (borderC || (ctx.box && ctx.box.borderC)) : (ctx.box && node.tag === 'input' && !borderC ? ctx.box.fill : n.bg)) || n.bg, bg: (ctx.box && node.tag === 'input' && !borderC) ? ctx.box.outside : parentBg, onGradient: !!n.onGradient, where: where || node.tag });
      if (role === 'switch') n.switchTrack = n.bg;
    }
    if (ctx.switchTrack && bgv && node.tag === 'span') {
      ui.push({ kind: 'switch-knob', required: true, fg: n.bg, bg: ctx.switchTrack, where: 'núm công tắc' });
    }
    let color = st.color;
    if (!color && node.tag === 'a') color = linkColor;
    if (!color && (node.tag === 'button' || node.tag === 'input')) color = /st-root button/.test(html) ? resolveVars('var(--ink)', tokens) : '#000000';
    if (color && color !== 'inherit' && color !== 'currentcolor') { const pc = parseColor(color); if (pc) { n.color = pc[3] < 1 ? over(pc, n.bg) : pc; n.colorSrc = color; } }
    if (st['font-size']) n.size = parseFloat(st['font-size']);
    if (st['font-weight']) n.weight = parseInt(st['font-weight']);
    if (node.tag === 'h1') { n.size = n.size || 32; n.weight = n.weight || 700; }
    if (node.tag === 'h2') { n.size = n.size || 24; n.weight = n.weight || 700; }
    if (node.tag === 'input') {
      const v = node.attrs.value || node.attrs.placeholder;
      if (v && node.attrs.type !== 'password') {
        if (node.attrs.value) out.push(Object.assign({}, n, { text: v }));
        else out.push(Object.assign({}, n, { text: v + ' (placeholder)', color: /::placeholder\{color:var\(--ink3\)/.test(html) ? parseColor(tokens.ink3) : [117, 117, 117, 1], colorSrc: 'placeholder' }));
      }
    }
    node.children.forEach(ch => walk(ch, n));
  }
  const rootBg = parseColor(resolveVars('var(--bg)', tokens)) || [255, 255, 255, 1];
  walk(tree, { bg: rootBg, color: [0, 0, 0, 1], size: 16, weight: 400 });
  audit.lastUi = ui.map(u => {
    const required = u.kind === 'icon' ? !!(u.control && !u.control.hasText) : u.required;
    const cr = u.ratioOverride !== undefined ? u.ratioOverride : ratio(u.fg, u.bg);
    return { file, theme, kind: u.kind, required, where: u.where, ratio: Math.round(cr * 100) / 100, pass: cr >= 3, fg: hex(u.fg), bg: hex(u.bg), gradient: !!u.onGradient };
  });
  return out.map(r => {
    const large = r.size >= 24 || (r.size >= 18.66 && r.weight >= 700);
    const need = large ? 3 : 4.5;
    const cr = ratio(r.color, r.bg);
    return { file, theme, text: r.text.slice(0, 48), ratio: Math.round(cr * 100) / 100, need, pass: cr >= need, fg: hex(r.color), bg: hex(r.bg), fgSrc: r.colorSrc || 'inherit', bgSrc: r.bgSrc || 'root', gradient: !!r.onGradient, size: r.size };
  });
}

module.exports = { audit, ratio, parseColor };
if (require.main !== module) return;

const files = fs.readdirSync(dir).filter(f => f.endsWith('.dc.html')).sort();
const all = [];
for (const f of files) for (const th of ['light', 'dark']) all.push(...audit(f, th));
fs.writeFileSync(path.join(dir, 'contrast-results.json'), JSON.stringify(all, null, 1));
const summary = {};
for (const r of all) { const k = r.theme; summary[k] = summary[k] || { total: 0, fail: 0, failGradient: 0 }; summary[k].total++; if (!r.pass) { if (r.gradient) summary[k].failGradient++; else summary[k].fail++; } }
console.log(JSON.stringify(summary));
const fails = all.filter(r => !r.pass);
const dedup = new Map();
for (const r of fails) { const k = [r.file, r.theme, r.fg, r.bg, r.fgSrc, r.bgSrc].join('|'); if (!dedup.has(k)) dedup.set(k, Object.assign({ n: 0, samples: [] }, r)); const d = dedup.get(k); d.n++; if (d.samples.length < 2) d.samples.push(r.text); }
for (const d of dedup.values()) console.log([d.file.replace('.dc.html', ''), d.theme, d.ratio + '/' + d.need, d.fg + ' on ' + d.bg, 'fg=' + d.fgSrc, 'bg=' + d.bgSrc, d.gradient ? 'GRADIENT' : '', 'x' + d.n, JSON.stringify(d.samples)].join(' | '));
