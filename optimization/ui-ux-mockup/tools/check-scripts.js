// Runs every board's renderVals(), fires each handler once, and checks that every {{binding}} in the
// template has a value. Usage (from optimization/ui-ux-mockup): node tools/check-scripts.js .
const fs = require('fs'), path = require('path');
const dir = process.argv[2];
global.setTimeout = () => 0; global.clearTimeout = () => {};
for (const f of fs.readdirSync(dir).filter(f => f.endsWith('.dc.html'))) {
  const html = fs.readFileSync(path.join(dir, f), 'utf8');
  const js = html.split('data-dc-script')[1].split('>').slice(1).join('>').split('</script>')[0];
  try {
    const C = new Function('DCLogic', js + '\nreturn Component;')(class { setState(s) { this.state = Object.assign({}, this.state, s); } });
    const c = new C(); c.props = {}; const v = c.renderVals();
    // fire every handler once, then render again
    let n = 0;
    const walk = o => { for (const k in o) { const x = o[k]; if (typeof x === 'function') { x({ target: { value: 'chi pheo' } }); n++; } else if (x && typeof x === 'object') walk(x); } };
    walk(v); c.renderVals();
    const keys = new Set(Object.keys(v));
    const used = [...html.split('</x-dc>')[0].matchAll(/\{\{\s*([a-zA-Z_][\w]*)[^}]*\}\}/g)].map(m => m[1]).filter(k => !['true','false'].includes(k));
    const loopVars = new Set([...html.matchAll(/as="(\w+)"/g)].map(m => m[1]));
    const missing = [...new Set(used)].filter(k => !keys.has(k) && !loopVars.has(k));
    console.log((missing.length ? 'MISSING ' + missing.join(',') : 'ok'), f, 'handlers', n);
  } catch (e) { console.log('ERROR', f, e.message); }
}
