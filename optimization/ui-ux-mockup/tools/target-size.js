// Touch target size of every control on every board (the plan asks for 48 dp; WCAG 2.2 SC 2.5.8 AA
// asks for 24 px). Reads width/height/min-height/padding from inline styles after rendering each board.
// Controls whose size comes from their text (no height set) are listed as "auto" and checked by eye.
// A board whose <style> has the ::after touch-area rule gives every control at least 48 x 48 px;
// a scrolling ancestor still clips it, so the chip rows have padding for it (checked by eye).
// Usage (from optimization/ui-ux-mockup): node tools/target-size.js .
const fs = require('fs'), path = require('path');
global.setTimeout = () => 0; global.clearTimeout = () => {};
global.window = { matchMedia: () => ({ matches: false }) };
const { parse, expand, styleMap } = require('./contrast.js');
const dir = process.argv[2] || '.';
const rows = [];
for (const file of fs.readdirSync(dir).filter(f => f.endsWith('.dc.html')).sort()) {
  const html = fs.readFileSync(path.join(dir, file), 'utf8');
  const js = html.split('data-dc-script')[1].split('>').slice(1).join('>').split('</script>')[0];
  const C = new Function('DCLogic', js + '\nreturn Component;')(class { setState(s) { this.state = Object.assign({}, this.state, s); } });
  const c = new C(); c.props = {}; c.state = { loaded: true, done: true, open: true };
  const vals = c.renderVals();
  const tpl = html.slice(html.indexOf('</helmet>') + 9, html.indexOf('</x-dc>'));
  // boards with the ::after rule give every control a touch area of at least 48 x 48 px
  const hitRule = html.includes('width:max(100%,48px);height:max(100%,48px)');
  const walk = (node, chrome, parentH) => {
    if (node.text !== undefined) return;
    const raw = node.attrs.style || '';
    const st = styleMap(raw);
    const isChrome = chrome || raw.includes('rgba(28,27,26,0.82)') || /bản phác ở nền/.test(node.attrs['aria-label'] || '');
    const px = v => (v && /^-?[\d.]+px$/.test(v.trim()) ? parseFloat(v) : null);
    const h = px(st.height) || px(st['min-height']);
    const w = px(st.width);
    const control = ['button', 'a', 'input'].includes(node.tag) || node.attrs.role === 'switch';
    if (control && !isChrome && node.attrs['aria-hidden'] !== 'true') {
      const label = node.attrs['aria-label'] || textOf(node).slice(0, 30) || node.tag;
      const insetFull = /inset:\s*0/.test(raw);
      const hh = h || (node.tag === 'input' ? parentH : null);
      const visualSmall = insetFull ? false : (hh !== null && hh < 48) || (w !== null && w < 48);
      const small = visualSmall && !hitRule;
      rows.push({ file, label, h: hh, w, small, visualSmall, auto: hh === null && !insetFull && !hitRule });
    }
    node.children.forEach(ch => walk(ch, isChrome, h || parentH));
  };
  const textOf = n => n.text !== undefined ? n.text.trim() : n.children.map(textOf).join(' ').replace(/\s+/g, ' ').trim();
  expand(parse(tpl), vals).forEach(n => walk(n, false, null));
}
const small = rows.filter(r => r.small), auto = rows.filter(r => r.auto);
console.log(`controls: ${rows.length}, drawn under 48 px: ${rows.filter(r => r.visualSmall).length}, touch area under 48 px: ${small.length}, size from text without the 48 px rule: ${auto.length}`);
const g = new Map();
for (const r of small) { const k = r.file.replace('.dc.html', '') + ' | ' + r.label + ' | ' + (r.w ? r.w + '×' : '') + r.h; g.set(k, (g.get(k) || 0) + 1); }
for (const [k, n] of g) console.log('  <48 ', k, n > 1 ? 'x' + n : '');
const ga = new Map();
for (const r of auto) { const k = r.file.replace('.dc.html', '') + ' | ' + r.label; ga.set(k, (ga.get(k) || 0) + 1); }
for (const [k, n] of ga) console.log('  auto', k, n > 1 ? 'x' + n : '');
