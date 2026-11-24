// Shared pieces for the wide-window boards (960 x 720). Each board file stays standalone:
// the generator writes the full HTML, so the boards need nothing from this file at run time.
const TOK_L = '--switch-on:#2f5b3a;--switch-knob-on:#ffffff;--bg:#ece7e1;--bg-rgb:236,231,225;--bg2:#d9d1c6;--surface:#ffffff;--glass:255,255,255;--edge:255,255,255;--ink:#1c1b1a;--ink-rgb:28,27,26;--ink2:#3d3833;--ink2b:#4a4540;--ink3:#5c554e;--primary:#1c1b1a;--on-primary:#ffffff;--ok:#2f5b3a;--danger:#7a3420;--danger2:#8a2f1b;--warn:#6b4a10;--warn2:#6b5220;--warn3:#8a6a2a;--warn-bg:#fff4dc;--danger-bg:#f6e4dc;--dialog:#f4f1ec;';
const TOK_D = '--switch-on:#9fd3ac;--switch-knob-on:#1c1b1a;--bg:#151412;--bg-rgb:21,20,18;--bg2:#1f1c19;--surface:#2a2723;--glass:44,40,36;--edge:96,88,79;--ink:#f2eee9;--ink-rgb:242,238,233;--ink2:#d3cbc1;--ink2b:#c6bdb2;--ink3:#a99f94;--primary:#f2eee9;--on-primary:#1c1b1a;--ok:#9fd3ac;--danger:#f0a58e;--danger2:#f0a58e;--warn:#f0c878;--warn2:#e6c27a;--warn3:#e6c27a;--warn-bg:#3a2e14;--danger-bg:#3b221a;--dialog:#2a2723;';

// Expanded window that is narrow (840-900 dp) or short (under 480 dp, e.g. a phone held sideways):
// the side menu folds into an icon rail and the panes move left. The board root is the container,
// so this follows the board's own size (render-static's 844 x 390 variant), not the browser window.
const COMPACT = `div.st-root{container-type:size}
@container (max-width: 900px) or (max-height: 480px){
  .st-nav{width:72px !important;padding:12px 6px !important}
  .st-nav .st-hide-c{display:none !important}
  .st-nav a{justify-content:center !important;padding:6px !important}
  .st-list{left:104px !important;width:300px !important}
  .st-pane{left:420px !important}
  .st-full{left:104px !important}
}`;

const head = (title, w, h) => `<!doctype html>
<html lang="vi">
<head>
<meta charset="utf-8">
<title>ShelfTap · ${title}</title>
<script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Nunito:wght@400;500;600;700;800&amp;display=swap" rel="stylesheet">
<style>
body{margin:0}
a{color:var(--ink)}a:hover{opacity:.85}
@keyframes st-rise{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:none}}
@keyframes st-item{from{opacity:0;transform:translateX(-10px)}to{opacity:1;transform:none}}
@keyframes st-swapin{from{opacity:0;transform:translateX(12px)}to{opacity:1;transform:none}}
@keyframes st-fade{from{opacity:0}to{opacity:1}}
@keyframes st-pop{0%{opacity:0;scale:.6}70%{opacity:1;scale:1.06}100%{opacity:1;scale:1}}
@keyframes st-draw{to{stroke-dashoffset:0}}
@keyframes st-ping{0%{opacity:.55;scale:1}100%{opacity:0;scale:1.35}}
@keyframes st-breathe{0%,100%{scale:1}50%{scale:1.04}}
@keyframes st-spin{to{transform:rotate(360deg)}}
.st-row{animation:st-rise 320ms cubic-bezier(.2,.8,.2,1) both}
.st-rise{animation:st-rise 420ms cubic-bezier(.2,.8,.2,1) both}
.st-item{animation:st-item 240ms cubic-bezier(.2,.8,.2,1) both}
.st-detail{animation:st-swapin 260ms cubic-bezier(.2,.8,.2,1) both}
.st-fade{animation:st-fade 260ms ease-out both}
.st-pop{animation:st-pop 380ms cubic-bezier(.2,.8,.2,1) both}
.st-draw{stroke-dasharray:30;stroke-dashoffset:30;animation:st-draw 420ms 180ms ease-out forwards}
.st-ping{opacity:0;animation:st-ping 1.8s cubic-bezier(.2,.6,.3,1) infinite}
.st-late{animation-delay:.9s}
.st-breathe{animation:st-breathe 2.4s ease-in-out infinite}
.st-spin{animation:st-spin 900ms linear infinite;transform-origin:50% 50%}
.st-press{transition:scale 120ms ease}.st-press:active{scale:.96}
.st-swap{transition:background-color 220ms ease,color 220ms ease}
.st-knob{transition:transform 220ms cubic-bezier(.2,.8,.2,1)}
.st-chip{transition:background-color 180ms ease,color 180ms ease,transform 120ms ease}
.st-chip:active{transform:scale(0.96)}
.st-pick{transition:background-color 180ms ease}
@media (prefers-reduced-motion: reduce){
  .st-row,.st-rise,.st-item,.st-detail,.st-fade,.st-pop,.st-ping,.st-breathe,.st-spin{animation:none}
  .st-draw{animation:none;stroke-dashoffset:0}
  .st-press,.st-swap,.st-knob,.st-chip,.st-pick{transition:none}
}
.st-root{${TOK_L}}
.st-root[data-theme="dark"]{${TOK_D}}
@media (prefers-color-scheme: dark){.st-root[data-theme="auto"]{${TOK_D}}}
.st-root button,.st-root input{color:var(--ink)}
.st-root ::placeholder{color:var(--ink3);opacity:1}
.st-root :is(button,a,[role="switch"]){position:relative}
.st-root :is(button,a,[role="switch"])::after{content:"";position:absolute;left:50%;top:50%;width:max(100%,48px);height:max(100%,48px);transform:translate(-50%,-50%)}
.st-root input{min-width:0}
.st-scroll{scrollbar-width:none}.st-scroll::-webkit-scrollbar{display:none}
.st-scroll>*{flex-shrink:0}
${COMPACT}
</style>
</helmet>
<div class="st-root" data-theme="{{themeAttr}}" style="width: ${w}px; height: ${h}px; position: relative; overflow: hidden; background: var(--bg); font-family: 'Nunito', system-ui, sans-serif; color: var(--ink)">
`;

const themeBtn = `
  <button type="button" onClick="{{toggleTheme}}" aria-label="{{themeAria}}" class="st-press" style="position: absolute; top: 24px; right: 28px; z-index: 50; height: 30px; padding: 0 12px 0 9px; border-radius: 999px; border: 1px solid rgba(var(--edge),0.7); background: rgba(var(--glass),0.8); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); color: var(--ink); font: inherit; font-size: 12px; font-weight: 700; display: flex; align-items: center; gap: 6px; cursor: pointer">
    <sc-if value="{{themeIsLight}}" hint-placeholder-val="{{ true }}"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"></path></svg></sc-if>
    <sc-if value="{{themeIsDark}}" hint-placeholder-val="{{ false }}"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="4"></circle><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"></path></svg></sc-if>
    {{themeLabel}}
  </button>`;

const foot = (w, h, script) => `${themeBtn}
</div>
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{"$preview":{"width":${w},"height":${h}}}'>
class Component extends DCLogic {
  __theme() {
    var self = this;
    var t = (this.state && this.state.__theme) || 'auto';
    var sys = typeof window !== 'undefined' && window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)').matches : false;
    var dark = t === 'dark' || (t === 'auto' && sys);
    var opts = [['auto', 'Theo hệ thống'], ['light', 'Sáng'], ['dark', 'Tối']];
    return {
      themeAttr: t,
      themeLabel: dark ? 'Sáng' : 'Tối',
      themeAria: dark ? 'Xem bản phác ở nền sáng' : 'Xem bản phác ở nền tối',
      themeIsDark: dark,
      themeIsLight: !dark,
      toggleTheme: function () { self.setState({ __theme: dark ? 'light' : 'dark' }); },
      themeOpts: opts.map(function (o) {
        var on = o[0] === t;
        return {
          label: o[1], on: on,
          pick: function () { self.setState({ __theme: o[0] }); },
          style: 'flex:1;height:40px;border-radius:999px;border:0;font:inherit;font-size:13px;font-weight:' + (on ? '700' : '500') + ';cursor:pointer;background:' + (on ? 'var(--primary)' : 'transparent') + ';color:' + (on ? 'var(--on-primary)' : 'var(--ink2)')
        };
      })
    };
  }
  // helpers shared by the wide boards
  __ui() {
    var self = this;
    var mono = function (t) { var w = t.split(/\\s+/); return (w[0][0] + (w[1] ? w[1][0] : '')).toUpperCase(); };
    return {
      mono: mono,
      // state preview tabs (mockup only), same look as the preview bar on the phone boards
      tabs: function (defs, cur, key, extra) {
        return defs.map(function (d) {
          var on = d[0] === cur;
          return {
            label: d[1], on: on,
            pick: function () { var o = {}; o[key] = d[0]; if (extra) Object.assign(o, extra); self.setState(o); },
            style: 'min-height:32px;padding:4px 10px;border-radius:999px;border:0;font:inherit;font-size:12px;line-height:1.15;font-weight:' + (on ? '700' : '500') + ';cursor:pointer;background:' + (on ? '#ffffff' : 'transparent') + ';color:' + (on ? '#1c1b1a' : 'rgba(255,255,255,0.85)')
          };
        });
      },
      row: function (o) {
        return {
          title: o.title, author: o.author || '', mono: mono(o.title),
          status: o.status || '', pill: o.pill || '',
          hasNote: !!o.note, note: o.note || '', hasReason: !!o.reason, reason: o.reason || '',
          current: o.on ? 'true' : 'false', pick: o.pick,
          delay: 'animation-delay:' + Math.min(o.i || 0, 8) * 40 + 'ms',
          style: 'width:100%;display:flex;gap:12px;align-items:center;padding:10px;border-radius:20px;text-align:left;font:inherit;color:var(--ink);cursor:pointer;min-height:48px;border:1px solid ' + (o.on ? 'rgba(var(--ink-rgb),0.5)' : 'transparent') + ';background:' + (o.on ? 'var(--surface)' : 'rgba(var(--glass),0.72)'),
          cover: 'width:44px;height:62px;flex:none;border-radius:10px;display:flex;align-items:flex-end;padding:6px;box-sizing:border-box;font-size:13px;font-weight:700;letter-spacing:0.5px;background:' + o.bg + ';color:' + o.fg
        };
      },
      pill: function (kind) {
        var bg = { ok: 'rgba(47,91,58,0.12)', bad: 'rgba(181,69,43,0.12)', wait: 'rgba(var(--ink-rgb),0.08)' }[kind];
        var fg = { ok: 'var(--ok)', bad: 'var(--danger2)', wait: 'var(--ink2)' }[kind];
        return 'font-size:12px;font-weight:600;padding:3px 10px;border-radius:999px;background:' + bg + ';color:' + fg;
      },
      bigCover: function (bg, fg) {
        return 'width:104px;height:150px;flex:none;border-radius:14px;padding:12px 10px;box-sizing:border-box;display:flex;flex-direction:column;justify-content:space-between;box-shadow:0 12px 28px rgba(0,0,0,0.2);background:' + bg + ';color:' + fg;
      },
      phaseBtn: function (phase, labels, extra) {
        var bg = { idle: 'var(--primary)', sending: '#5c554e', done: '#2f5b3a' };
        return { label: labels[phase], isIdle: phase === 'idle', isDone: phase === 'done',
          style: 'flex:1;min-height:52px;padding:6px 16px;border-radius:999px;border:0;background:' + bg[phase] + ';color:' + (phase === 'idle' ? 'var(--on-primary)' : '#ffffff') + ';display:flex;align-items:center;justify-content:center;gap:10px;font:inherit;font-size:15px;font-weight:600;line-height:1.2;cursor:pointer' + (extra || '') };
      },
      track: function (on) { return 'width:52px;height:32px;flex:none;box-sizing:border-box;border-radius:999px;padding:2px;cursor:pointer;display:flex;align-items:center;justify-content:flex-start;border:2px solid ' + (on ? 'var(--switch-on)' : 'var(--ink3)') + ';background:' + (on ? 'var(--switch-on)' : 'rgba(var(--ink-rgb),0.06)'); },
      knob: function (on) { return 'width:24px;height:24px;border-radius:999px;background:' + (on ? 'var(--switch-knob-on)' : 'var(--ink3)') + ';box-shadow:0 1px 3px rgba(0,0,0,0.2);transform:translateX(' + (on ? '20px' : '0') + ') scale(' + (on ? '1' : '0.72') + ')'; }
    };
  }
${script}
}
</script>
</body>
</html>
`;

// ---------- static pieces ----------
const ICON = {
  catalog: ['M4 19.5V5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2.5z', 'M8 7h7'],
  recommend: ['M12 3l2.5 5.5L20 9l-4 4 1 6-5-3-5 3 1-6-4-4 5.5-.5z', ''],
  loans: ['M12 7v5l3 2', 'M21 12a9 9 0 1 1-9-9 9 9 0 0 1 9 9z'],
  reservations: ['M6 3h12v18l-6-4-6 4z', 'M9 9h6'],
  history: ['M3 12a9 9 0 1 0 3-6.7L3 8', 'M3 3v5h5'],
  account: ['M4 21a8 8 0 0 1 16 0', 'M16 8a4 4 0 1 1-8 0 4 4 0 0 1 8 0z'],
  books: ['M4 6h16', 'M4 12h16M4 18h10']
};
const MEMBER_MENU = [
  ['Danh mục', 'WideCatalog.dc.html', 'catalog'],
  ['Gợi ý cho bạn', 'WideRecommend.dc.html', 'recommend'],
  ['Sách đang mượn', 'WideLoans.dc.html', 'loans'],
  ['Đặt trước', 'WideReservations.dc.html', 'reservations'],
  ['Lịch sử đọc', 'WideHistory.dc.html', 'history'],
  ['Tài khoản', 'WideAccount.dc.html', 'account']
];
const NFC = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="color: var(--on-primary)"><path d="M6 8.5a6 6 0 0 1 0 7"></path><path d="M9.5 6a10 10 0 0 1 0 12"></path><path d="M13 3.5a14 14 0 0 1 0 17"></path></svg>';

const menuItem = (label, href, icon, active, i) => `
        <a href="${href}" class="st-item" aria-label="${label}" aria-current="${active ? 'page' : 'false'}" style="animation-delay: ${40 + 35 * i}ms; display: flex; align-items: center; gap: 12px; min-height: 52px; padding: 6px 10px; border-radius: 20px; text-decoration: none; color: var(--ink); font-size: 15px; font-weight: ${active ? 700 : 500}; background: ${active ? 'rgba(var(--glass),0.85)' : 'transparent'}">
          <span style="width: 36px; height: 36px; flex: none; border-radius: 12px; display: flex; align-items: center; justify-content: center; background: ${active ? 'var(--surface)' : 'rgba(var(--glass),0.5)'}">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="color: var(--ink)"><path d="${ICON[icon][0]}"></path><path d="${ICON[icon][1]}"></path></svg>
          </span>
          <span class="st-hide-c">${label}</span>
        </a>`;

// state preview (mockup only), dark like the phone boards' preview bar
const preview = (list, label, extraRow, cls) => `
    <div role="group" aria-label="Xem trước trạng thái (bản phác)" class="${cls || ''}" style="margin: 4px 0 10px; padding: 8px; border-radius: 22px; background:  rgba(28,27,26,0.82); display: flex; flex-direction: column; gap: 6px">
      <div style="font-size: 11px; font-weight: 600; color: rgba(255,255,255,0.85); padding: 0 6px">${label}</div>
      <div style="display: flex; flex-wrap: wrap; gap: 2px">
        <sc-for list="{{${list}}}" as="t" hint-placeholder-count="3">
          <button type="button" aria-pressed="{{t.on}}" onClick="{{t.pick}}" style="{{t.style}}">{{t.label}}</button>
        </sc-for>
      </div>${extraRow || ''}
    </div>`;

// fixed side menu for a member; active = href of the current item (or '' for none)
const nav = (active, opts) => {
  opts = opts || {};
  const items = MEMBER_MENU.map((m, i) => menuItem(m[0], m[1], m[2], m[1] === active, i)).join('');
  return `
  <nav aria-label="Điều hướng chính" class="st-nav" style="position: absolute; left: 16px; top: 16px; bottom: 16px; width: 216px; padding: 20px 10px 12px; box-sizing: border-box; border-radius: 30px; background:  rgba(var(--glass),0.62); backdrop-filter: blur(26px) saturate(160%); -webkit-backdrop-filter: blur(26px) saturate(160%); border: 1px solid rgba(var(--edge),0.85); display: flex; flex-direction: column; gap: 2px">
    <div class="st-hide-c" style="padding: 0 12px 14px; display: flex; flex-direction: column; gap: 2px">
      <div style="font-size: 13px; color: var(--ink3)">Chào bạn,</div>
      <div style="font-size: 17px; font-weight: 700">Thư viện ShelfTap</div>
    </div>
    <div class="st-scroll" style="flex: 1; min-height: 0; overflow-y: auto; display: flex; flex-direction: column; gap: 2px; padding-bottom: 12px">${items}
    </div>${opts.preview || ''}
    <a href="WideCheckout.dc.html" class="st-press" aria-label="Chạm sách để mượn" aria-current="${active === 'WideCheckout.dc.html' ? 'page' : 'false'}" style="min-height: 56px; padding: 8px 14px 8px 8px; border-radius: 28px; background: var(--primary); color: var(--on-primary); display: flex; align-items: center; gap: 10px; text-decoration: none; font-size: 15px; font-weight: 600; line-height: 1.2; box-shadow: 0 10px 30px rgba(28,27,26,0.25)">
      <span style="width: 40px; height: 40px; flex: none; border-radius: 999px; background:  rgba(var(--glass),0.16); display: flex; align-items: center; justify-content: center">${NFC}</span>
      <span class="st-hide-c">Chạm sách để mượn</span>
    </a>
  </nav>`;
};

// list pane: title, optional extra html between title and list, rows bound to `list`
const listPane = (label, title, extra, list, after) => `
  <section aria-label="${label}" class="st-list" style="position: absolute; left: 248px; top: 16px; bottom: 0; width: 320px; display: flex; flex-direction: column; gap: 12px">
    <h1 style="margin: 12px 4px 0; font-size: 28px; line-height: 1.15; font-weight: 700; letter-spacing: -0.3px">${title}</h1>${extra || ''}
    <ul class="st-scroll" style="list-style: none; margin: 0; padding: 0 0 16px; flex: 1; min-height: 0; overflow-y: auto; display: flex; flex-direction: column; gap: 8px">
      <sc-for list="{{${list}}}" as="b" hint-placeholder-count="3">
        <li class="st-row" style="{{b.delay}}">
          <button type="button" class="st-pick" aria-current="{{b.current}}" onClick="{{b.pick}}" style="{{b.style}}">
            <span aria-hidden="true" style="{{b.cover}}">{{b.mono}}</span>
            <span style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px">
              <span style="font-size: 15px; font-weight: 700; line-height: 1.25">{{b.title}}</span>
              <span style="font-size: 13px; color: var(--ink2b); white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{{b.author}}</span>
              <span style="display: flex; gap: 6px; align-items: center; margin-top: 2px; flex-wrap: wrap">
                <span style="{{b.pill}}">{{b.status}}</span>
                <sc-if value="{{b.hasNote}}" hint-placeholder-val="{{ false }}"><span style="font-size: 12px; color: var(--ink3)">{{b.note}}</span></sc-if>
              </span>
              <sc-if value="{{b.hasReason}}" hint-placeholder-val="{{ false }}"><span style="font-size: 12px; font-weight: 700; line-height: 1.35; color: var(--warn2)">{{b.reason}}</span></sc-if>
            </span>
          </button>
        </li>
      </sc-for>${after || ''}
    </ul>
  </section>`;

// detail pane: header row with a label, scrollable body, optional action bar pinned at the bottom
const detailPane = (label, body, bar) => `
  <section aria-label="${label}" class="st-pane" style="position: absolute; left: 584px; right: 16px; top: 16px; bottom: 16px; border-radius: 30px; overflow: hidden; background:  rgba(var(--glass),0.55); border: 1px solid rgba(var(--edge),0.85)">
    <div class="st-scroll st-detail" style="position: absolute; inset: 0; padding: 12px 20px ${bar ? 96 : 24}px; box-sizing: border-box; overflow-y: auto; display: flex; flex-direction: column; gap: 16px">
      <div style="display: flex; align-items: center; gap: 8px; min-height: 48px">
        <div style="flex: 1; font-size: 13px; font-weight: 600; color: var(--ink3)">${label}</div>
        <div style="width: 84px; flex: none"></div>
      </div>${body}
    </div>${bar ? `
    <div style="position: absolute; left: 12px; right: 12px; bottom: 12px; padding: 8px; border-radius: 999px; background:  rgba(var(--glass),0.7); backdrop-filter: blur(22px) saturate(150%); -webkit-backdrop-filter: blur(22px) saturate(150%); border: 1px solid rgba(var(--edge),0.9); display: flex; gap: 8px">${bar}
    </div>` : ''}
  </section>`;

// book header used in detail panes: cover + tags + title + author, bound to `sel`
const bookHead = (tags) => `
      <div style="display: flex; gap: 16px; align-items: flex-start">
        <div aria-hidden="true" style="{{sel.coverStyle}}">
          <div style="font-size: 17px; font-weight: 700; line-height: 1.15">{{sel.title}}</div>
          <div style="font-size: 11px; opacity: 0.85">{{sel.author}}</div>
        </div>
        <div style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8px">${tags ? `
          <div style="display: flex; gap: 6px; flex-wrap: wrap">
            <sc-for list="{{sel.tags}}" as="t" hint-placeholder-count="2">
              <span style="padding: 5px 10px; border-radius: 999px; background:  rgba(var(--glass),0.8); font-size: 12px; font-weight: 500; color: var(--ink2)">{{t.label}}</span>
            </sc-for>
          </div>` : ''}
          <h2 style="margin: 0; font-size: 24px; line-height: 1.15; font-weight: 700; letter-spacing: -0.2px">{{sel.title}}</h2>
          <div style="font-size: 14px; color: var(--ink2b)">{{sel.author}}</div>
        </div>
      </div>`;

const fact = (label, value, valueStyle) => `
        <div style="padding: 12px; border-radius: 18px; background: var(--surface); display: flex; flex-direction: column; gap: 2px">
          <div style="font-size: 11px; color: var(--ink3)">${label}</div>
          <div style="${valueStyle || 'font-size: 15px; font-weight: 700; line-height: 1.25'}">${value}</div>
        </div>`;
const facts = (cells, cols) => `
      <div style="display: grid; grid-template-columns: repeat(${cols || cells.length}, minmax(0, 1fr)); gap: 8px">${cells.join('')}
      </div>`;

const BOOKS = `[
      { t: 'Truyện Kiều', a: 'Nguyễn Du', s: 'Thơ · Văn học Việt Nam', isbn: '9780990000013', n: 2, shelf: 'A01', g: 'vn', bg: '#e7dcc6', fg: '#2f4a44' },
      { t: 'Dế Mèn phiêu lưu ký', a: 'Tô Hoài', s: 'Thiếu nhi · Văn học Việt Nam', isbn: '9780990000020', n: 3, shelf: 'A02', g: 'vn', bg: '#c9a66b', fg: '#1c1b1a' },
      { t: 'Số đỏ', a: 'Vũ Trọng Phụng', s: 'Tiểu thuyết · Văn học Việt Nam', isbn: '9780990000037', n: 2, shelf: 'A03', g: 'vn', bg: '#b5452b', fg: '#ffffff' },
      { t: 'Tắt đèn', a: 'Ngô Tất Tố', s: 'Tiểu thuyết · Văn học Việt Nam', isbn: '9780990000044', n: 0, shelf: 'A04', g: 'vn', bg: '#7a2e1e', fg: '#ffffff' },
      { t: 'Lão Hạc', a: 'Nam Cao', s: 'Truyện ngắn · Văn học Việt Nam', isbn: '9780990000051', n: 1, shelf: 'A05', g: 'vn', bg: '#d8c9a8', fg: '#3e3a36' },
      { t: 'Chí Phèo', a: 'Nam Cao', s: 'Truyện ngắn · Văn học Việt Nam', isbn: '9780990000068', n: 1, shelf: 'A06', g: 'vn', bg: '#3e3a36', fg: '#ffffff' },
      { t: 'Clean Code', a: 'Robert C. Martin', s: 'Software engineering · Programming', isbn: '9780990000075', n: 3, shelf: 'A07', g: 'dev', bg: '#2f4a44', fg: '#ffffff' },
      { t: 'Introduction to Algorithms', a: 'Thomas H. Cormen, Charles E. Leiserson, Ronald L. Rivest, Clifford Stein', s: 'Algorithms · Computer science', isbn: '9780990000082', n: 2, shelf: 'A08', g: 'dev', bg: '#5a6b7a', fg: '#ffffff' },
      { t: 'The Pragmatic Programmer', a: 'Andrew Hunt, David Thomas', s: 'Software engineering · Programming', isbn: '9780990000099', n: 1, shelf: 'A09', g: 'dev', bg: '#c9a66b', fg: '#1c1b1a' },
      { t: 'Designing Data-Intensive Applications', a: 'Martin Kleppmann', s: 'Databases · Distributed systems', isbn: '9780990000105', n: 2, shelf: 'A10', g: 'data', bg: '#e7dcc6', fg: '#3e3a36' },
      { t: 'Java Card Technology for Smart Cards', a: 'Zhiqun Chen', s: 'Smart cards · Java', isbn: '9780990000112', n: 1, shelf: 'A11', g: 'sec', bg: '#3e3a36', fg: '#e7dcc6' },
      { t: 'Applied Cryptography', a: 'Bruce Schneier', s: 'Cryptography · Security', isbn: '9780990000129', n: 2, shelf: 'A12', g: 'sec', bg: '#1c1b1a', fg: '#c9a66b' },
      { t: 'Android Programming: The Big Nerd Ranch Guide', a: 'Bill Phillips, Chris Stewart, Kristin Marsicano', s: 'Android · Mobile development', isbn: '9780990000136', n: 2, shelf: 'A13', g: 'dev', bg: '#6b8f5e', fg: '#ffffff' }
    ]`;

// search box and topic chips used by the catalog-like boards (bound to q/onQ and chips)
const searchAndChips = `
    <label style="display: flex; align-items: center; gap: 10px; height: 48px; flex: none; padding: 0 16px; border-radius: 999px; background:  rgba(var(--glass),0.75); border: 1px solid rgba(var(--ink-rgb),0.5)">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="color: var(--ink3)"><circle cx="11" cy="11" r="7"></circle><path d="m20 20-3.5-3.5"></path></svg>
      <span style="position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0)">Tìm sách</span>
      <input type="search" value="{{q}}" onInput="{{onQ}}" placeholder="Tên sách, tác giả, ISBN" style="flex: 1; border: 0; background:  transparent; font: inherit; font-size: 15px; color: var(--ink); outline: none">
    </label>
    <div class="st-scroll" style="display: flex; gap: 8px; overflow-x: auto; flex: none; margin: 0 -4px; padding: 0 4px">
      <sc-for list="{{chips}}" as="c" hint-placeholder-count="5">
        <button type="button" class="st-chip" aria-pressed="{{c.on}}" onClick="{{c.pick}}" style="{{c.style}}">{{c.label}}</button>
      </sc-for>
    </div>
    <div role="status" style="font-size: 13px; color: var(--ink3); padding: 0 4px">{{countLabel}}</div>`;

// renderVals code for search + chips over BOOKS; leaves `hits` and catalog values in scope
const SEARCH_JS = `
    var books = ${BOOKS};
    var groups = [['all', 'Tất cả'], ['vn', 'Văn học Việt Nam'], ['dev', 'Lập trình'], ['sec', 'Bảo mật và thẻ'], ['data', 'Dữ liệu']];
    var q = st.q || '', chip = st.chip || 'all';
    var fold = function (s) { return s.normalize('NFD').replace(/[\\u0300-\\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase(); };
    var fq = fold(q.trim());
    var hits = books.filter(function (b) {
      if (chip !== 'all' && b.g !== chip) return false;
      if (!fq) return true;
      return fold(b.t + ' ' + b.a + ' ' + b.s).indexOf(fq) >= 0 || b.isbn.indexOf(fq.replace(/-/g, '')) >= 0;
    });
    var search = {
      q: q,
      onQ: function (e) { self.setState({ q: e.target.value }); },
      countLabel: hits.length + ' / ' + books.length + ' đầu sách',
      empty: hits.length === 0,
      chips: groups.map(function (g) {
        var on = g[0] === chip;
        return { label: g[1], on: on, pick: function () { self.setState({ chip: g[0] }); },
          style: 'flex:none;height:40px;padding:0 14px;border-radius:999px;font:inherit;font-size:14px;font-weight:600;cursor:pointer;white-space:nowrap;border:1px solid ' + (on ? 'var(--primary)' : 'rgba(var(--edge),0.9)') + ';background:' + (on ? 'var(--primary)' : 'rgba(var(--glass),0.6)') + ';color:' + (on ? 'var(--on-primary)' : 'var(--ink2)') };
      })
    };`;

const btnPrimary = 'min-height: 52px; padding: 6px 20px; border-radius: 999px; border: 0; background: var(--primary); color: var(--on-primary); display: flex; align-items: center; justify-content: center; gap: 10px; text-decoration: none; font: inherit; font-size: 15px; font-weight: 600; line-height: 1.2; cursor: pointer; box-sizing: border-box';
const btnGlass = 'min-height: 52px; padding: 6px 20px; border-radius: 999px; border: 1px solid rgba(var(--ink-rgb),0.5); background: var(--surface); color: var(--ink); display: flex; align-items: center; justify-content: center; gap: 10px; text-decoration: none; font: inherit; font-size: 15px; font-weight: 600; line-height: 1.2; cursor: pointer; box-sizing: border-box';
const note = (text, extra) => `
      <p style="margin: 0; font-size: 12px; line-height: 1.5; color: var(--ink3)${extra || ''}">${text}</p>`;

module.exports = { head, foot, nav, preview, listPane, detailPane, bookHead, fact, facts, BOOKS, searchAndChips, SEARCH_JS, btnPrimary, btnGlass, note, NFC, menuItem };
