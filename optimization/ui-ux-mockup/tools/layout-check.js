// Layout check, run inside a page made by tools/render-static.js (paste into the browser console,
// or run with a browser automation tool). For every board it reports:
//   offscreen : text or a control that sticks out past the left/right edge of the screen
//   cut       : text or a control below the bottom of the screen, outside any scrolling area
//   overlap   : two pieces of text drawn on top of each other
//   clipped   : text cut off inside a box that hides overflow, without an ellipsis
// Decorative layers (aria-hidden) and anything inside a scrolling area are ignored for offscreen/cut.
// Returns { board: { offscreen: [...], cut: [...], overlap: [...], clipped: [...] } }.
(() => {
  const out = {};
  for (const fig of document.querySelectorAll('figure[data-board]')) {
    const doc = fig.querySelector('iframe').contentDocument;
    const root = doc.querySelector('.st-root');
    if (!root) continue;
    const R = root.getBoundingClientRect();
    const hidden = el => el.closest('[aria-hidden="true"]');
    const scroller = el => {
      for (let p = el.parentElement; p && p !== root; p = p.parentElement) {
        const cs = doc.defaultView.getComputedStyle(p);
        if (/(auto|scroll)/.test(cs.overflowY + cs.overflowX)) return p;
      }
      return null;
    };
    const label = el => (el.getAttribute('aria-label') || el.textContent || el.tagName).replace(/\s+/g, ' ').trim().slice(0, 40);
    // leaf text boxes: elements with their own non-empty text node
    const texts = [...root.querySelectorAll('*')].filter(el => [...el.childNodes].some(n => n.nodeType === 3 && n.textContent.trim()) && !hidden(el));
    const controls = [...root.querySelectorAll('button, a, input, [role="switch"]')].filter(el => !hidden(el));
    // 1 px boxes are visually hidden labels for screen readers, not visible text
    const visible = el => { const r = el.getBoundingClientRect(); const cs = doc.defaultView.getComputedStyle(el); return r.width > 1 && r.height > 1 && cs.visibility !== 'hidden' && cs.display !== 'none'; };
    const res = { offscreen: [], cut: [], overlap: [], clipped: [] };
    for (const el of new Set([...texts, ...controls])) {
      if (!visible(el) || scroller(el)) continue;
      const r = el.getBoundingClientRect();
      if (r.left < R.left - 1 || r.right > R.right + 1) res.offscreen.push(label(el) + ` (${Math.round(r.left - R.left)}..${Math.round(r.right - R.left)} px)`);
      if (r.bottom > R.bottom + 1) res.cut.push(label(el) + ` (đáy ${Math.round(r.bottom - R.top)} px)`);
    }
    // text measured with a Range so the box is the text itself, not the whole block
    const textRect = el => { const rg = doc.createRange(); rg.selectNodeContents(el); return rg.getBoundingClientRect(); };
    const boxes = texts.filter(visible).map(el => ({ el, r: textRect(el) })).filter(b => b.r.width > 1);
    for (let i = 0; i < boxes.length; i++) for (let j = i + 1; j < boxes.length; j++) {
      const a = boxes[i], b = boxes[j];
      if (a.el.contains(b.el) || b.el.contains(a.el)) continue;
      // scrolling content passing under a floating bar is expected (the scroll area leaves room at the bottom)
      if (!!scroller(a.el) !== !!scroller(b.el)) continue;
      const w = Math.min(a.r.right, b.r.right) - Math.max(a.r.left, b.r.left);
      const h = Math.min(a.r.bottom, b.r.bottom) - Math.max(a.r.top, b.r.top);
      if (w > 4 && h > 4) res.overlap.push(label(a.el) + ' × ' + label(b.el));
    }
    for (const { el } of boxes) {
      const cs = doc.defaultView.getComputedStyle(el);
      if (cs.textOverflow === 'ellipsis') continue;
      for (let p = el; p && p !== root; p = p.parentElement) {
        const pcs = doc.defaultView.getComputedStyle(p);
        if (pcs.overflow === 'hidden' || pcs.overflowX === 'hidden' || pcs.overflowY === 'hidden') {
          const pr = p.getBoundingClientRect(), tr = textRect(el);
          if (tr.right > pr.right + 1 || tr.bottom > pr.bottom + 1 || tr.left < pr.left - 1) res.clipped.push(label(el));
          break;
        }
      }
    }
    out[fig.dataset.board.replace('.dc.html', '')] = res;
  }
  return out;
})();
