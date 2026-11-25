// Non-text contrast (WCAG 2.2 SC 1.4.11, 3:1) for every board, light and dark.
// Required: icons in controls with no visible text, input field outlines, switch tracks and knobs,
// and the fill of a selected chip or tab. Advisory: icons next to text, edges of buttons and links.
// Usage (from optimization/ui-ux-mockup): node tools/nontext-contrast.js .
// Writes nontext-results.json next to the boards.
const fs = require('fs'), path = require('path');
const { audit } = require('./contrast.js');
const dir = process.argv[2] || '.';
const files = fs.readdirSync(dir).filter(f => f.endsWith('.dc.html')).sort();
const all = [];
for (const f of files) for (const th of ['light', 'dark']) { audit(f, th, dir); all.push(...audit.lastUi); }
fs.writeFileSync(path.join(dir, 'nontext-results.json'), JSON.stringify(all, null, 1));
const sum = {};
for (const r of all) {
  const k = r.theme + (r.required ? ' bắt buộc' : ' khuyến nghị');
  sum[k] = sum[k] || { total: 0, fail: 0 }; sum[k].total++; if (!r.pass) sum[k].fail++;
}
console.log(JSON.stringify(sum));
const seen = new Map();
for (const r of all.filter(r => !r.pass)) {
  const key = [r.required, r.file, r.theme, r.kind, r.fg, r.bg].join('|');
  if (!seen.has(key)) seen.set(key, Object.assign({ n: 0, places: new Set() }, r));
  const s = seen.get(key); s.n++; if (r.where) s.places.add(r.where);
}
for (const s of [...seen.values()].sort((a, b) => b.required - a.required || a.file.localeCompare(b.file)))
  console.log([s.required ? 'BẮT BUỘC' : 'khuyến nghị', s.file.replace('.dc.html', ''), s.theme, s.kind, s.ratio + '/3', s.fg + ' vs ' + s.bg, s.gradient ? 'GRADIENT' : '', 'x' + s.n, [...s.places].slice(0, 3).join('; ')].join(' | '));
