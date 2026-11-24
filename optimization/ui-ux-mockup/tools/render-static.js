// Renders every board to static HTML (no runtime needed), in layout variants, for layout checks in a browser.
// Each variant is one page with every board in its own same-origin iframe, so a script can measure them.
// Sizes are in dp (1 CSS px = 1 dp). The boards are drawn at 390 x 844.
//   base      : 390 x 844, text as designed
//   w320      : 320 x 844 (the narrowest phone width the plan asks for)
//   font2     : 390 x 844, text at Android font scale 2.0 (see FONT2 below)
//   w320f2    : both
//   tall      : 412 x 960, a tall 21:9 phone
//   wide      : 480 x 768, a wide 10:16 phone screen (wide cover screen of a book foldable)
//   flipcover : 360 x 400, the cover screen of a flip foldable (close to square)
//   split     : 390 x 420, half of a phone screen in split-screen mode
//   land      : 844 x 390, a phone turned sideways
//   unfold    : 720 x 840, the inner screen of a book foldable, open (close to square)
//   unfoldl   : 960 x 720, the same kind of screen held sideways (4:3)
// These are size classes, not exact models: published specs of this year's foldables disagree.
// Boards drawn for a wide window (not 390 x 844, e.g. WideCatalog, RailCatalog) are rendered at their
// own size, only in the variants that keep the size as drawn (base, font2).
// Usage (from optimization/ui-ux-mockup): node tools/render-static.js . <out-dir>
// Then open <out-dir>/<variant>.html in a browser and run tools/layout-check.js in it.
const fs = require('fs'), path = require('path');
global.setTimeout = () => 0; global.clearTimeout = () => {};
global.window = { matchMedia: () => ({ matches: false }) };
const { parse, expand } = require('./contrast.js');
const dir = process.argv[2] || '.', out = process.argv[3] || 'rendered';
fs.mkdirSync(out, { recursive: true });

// Android 14+ scales text non-linearly: small text grows by the full factor, large text grows less.
// Points (sp -> sp at font scale 2.0), interpolated linearly; above 100 sp text is not scaled.
// Values follow the font scale 2.0 table in AOSP FontScaleConverterFactory as I remember it; check
// against the AOSP source before quoting them in the report.
const FONT2 = [[8, 16], [10, 20], [12, 24], [14, 26], [18, 30], [20, 34], [24, 36], [30, 38], [100, 100]];
const scale2 = sp => {
  if (sp <= FONT2[0][0]) return sp * 2;
  for (let i = 1; i < FONT2.length; i++) {
    const [a, x] = FONT2[i - 1], [b, y] = FONT2[i];
    if (sp <= b) return x + (y - x) * (sp - a) / (b - a);
  }
  return sp;
};

const esc = s => s.replace(/&/g, '&amp;').replace(/"/g, '&quot;');
const VOID = new Set(['input', 'img', 'br', 'meta', 'link', 'hr']);
function toHtml(node, fontScale) {
  if (node.text !== undefined) return node.text;
  const attrs = Object.entries(node.attrs).map(([k, v]) => {
    if (/^on[A-Z]/.test(k)) return '';            // event handlers are runtime-only
    if (k === 'style' && fontScale) v = v.replace(/font-size:\s*([\d.]+)px/g, (_, n) => 'font-size: ' + scale2(parseFloat(n)).toFixed(1) + 'px');
    return ' ' + k + '="' + esc(String(v)) + '"';
  }).join('');
  if (VOID.has(node.tag)) return '<' + node.tag + attrs + '>';
  return '<' + node.tag + attrs + '>' + node.children.map(c => toHtml(c, fontScale)).join('') + '</' + node.tag + '>';
}

const PHONE = '<div class="st-root" data-theme="{{themeAttr}}" style="width: 390px; height: 844px;';
const sizeOf = file => {
  const m = fs.readFileSync(path.join(dir, file), 'utf8').match(/<div class="st-root" data-theme="\{\{themeAttr\}\}" style="width: (\d+)px; height: (\d+)px;/);
  if (!m) throw new Error(file + ': board root size not found');
  return [+m[1], +m[2]];
};

function board(file, width, height, fontScale) {
  const html = fs.readFileSync(path.join(dir, file), 'utf8');
  const js = html.split('data-dc-script')[1].split('>').slice(1).join('>').split('</script>')[0];
  const C = new Function('DCLogic', js + '\nreturn Component;')(class { setState(s) { this.state = Object.assign({}, this.state, s); } });
  const c = new C(); c.props = {}; c.state = { __theme: 'light', loaded: true, done: true, open: false, sheet: false };
  const vals = c.renderVals();
  const helmet = html.slice(html.indexOf('<helmet>') + 8, html.indexOf('</helmet>'));
  let tpl = html.slice(html.indexOf('</helmet>') + 9, html.indexOf('</x-dc>'));
  if (tpl.includes(PHONE)) tpl = tpl.replace(PHONE, '<div class="st-root" data-theme="{{themeAttr}}" style="width: ' + width + 'px; height: ' + height + 'px;');
  const body = expand(parse(tpl), vals).map(n => toHtml(n, fontScale)).join('');
  // the global helmet rules (body margin, link colour) and the board's own font
  return '<!doctype html><html lang="vi"><head><meta charset="utf-8">' + helmet + '</head><body>' + body + '</body></html>';
}

const variants = {
  base: [390, 844, false], w320: [320, 844, false], font2: [390, 844, true], w320f2: [320, 844, true],
  tall: [412, 960, false], wide: [480, 768, false], flipcover: [360, 400, false], split: [390, 420, false],
  land: [844, 390, false], unfold: [720, 840, false], unfoldl: [960, 720, false],
};
const files = fs.readdirSync(dir).filter(f => f.endsWith('.dc.html')).sort();
const ownSize = new Set(['base', 'font2']);
for (const [name, [vw, vh, fs2]] of Object.entries(variants)) {
  const frames = files.map(f => {
    const [bw, bh] = sizeOf(f);
    const phone = bw === 390 && bh === 844;
    if (!phone && !ownSize.has(name)) return '';
    const [w, h] = phone ? [vw, vh] : [bw, bh];
    return `<figure data-board="${f}" style="display:inline-block;margin:8px;vertical-align:top"><figcaption style="font:12px sans-serif">${f}</figcaption><iframe srcdoc="${esc(board(f, w, h, fs2))}" style="width:${w}px;height:${h}px;border:1px solid #999"></iframe></figure>`;
  }).filter(Boolean).join('\n');
  fs.writeFileSync(path.join(out, name + '.html'), `<!doctype html><html><head><meta charset="utf-8"><title>${name}</title></head><body style="margin:0;background:#888">${frames}</body></html>`);
  console.log('wrote', name);
}
