// Worst-case contrast of the white text on the welcome image (Main.dc.html), which tools/contrast.js
// cannot model: the text sits on a dark gradient laid over the shelf picture. Assumes the lightest
// colour of the picture (#d9d1c6) is directly behind each line. Positions are fractions of the
// 470 px gradient, measured from its top, estimated from the layout.
// Usage: node tools/hero-worst-case.js
const lum = c => { const f = v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }; return 0.2126 * f(c[0]) + 0.7152 * f(c[1]) + 0.0722 * f(c[2]); };
const ratio = (a, b) => { const x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); };
const over = (top, alpha, bottom) => top.map((v, i) => v * alpha + bottom[i] * (1 - alpha));
const grad = (stops, p) => { for (let i = 1; i < stops.length; i++) { const [a, x] = stops[i - 1], [b, y] = stops[i]; if (p <= b) return x + (y - x) * (p - a) / (b - a); } return stops[stops.length - 1][1]; };
const picture = [217, 209, 198], shade = [28, 22, 16], white = [255, 255, 255];
const versions = [
  ['before (v13)', [[0, 0], [0.4, 0.35], [1, 0.72]], 0.22],
  ['after (v14)', [[0, 0], [0.25, 0.5], [1, 0.8]], 0.16],
];
for (const [name, stops, glass] of versions) {
  const bg = p => over(shade, grad(stops, p), picture);
  const text = p => ratio(white, bg(p)).toFixed(2);
  const button = p => ratio(white, over(white, glass, bg(p))).toFixed(2);
  console.log(`${name}: title first line (p=0.28, needs 3) ${text(0.28)} | description (p=0.62, needs 4.5) ${text(0.62)} | "Bắt đầu" glass button (p=0.80, needs 4.5) ${button(0.80)} | bottom link (p=0.93, needs 4.5) ${text(0.93)} | card icon on the glass button (p=0.80, non-text, needs 3) ${button(0.80)}`);
}
