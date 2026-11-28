// Shared pieces for the boards drawn for larger windows. Each board file stays standalone:
// the generator writes the full HTML, so the boards need nothing from this file at run time.
// Two sizes, following the Material 3 window size classes:
//   'wide'   (Wide*.dc.html, 960 x 720): expanded window, 840 dp and up. Fixed side menu with a
//            collapse button, list and detail side by side.
//   'medium' (Mid*.dc.html, 720 x 840): medium window, 600-840 dp. Navigation rail, one pane at a
//            time: the list, then the detail with a back button.
let MODE = 'wide';
const setMode = m => { MODE = m; };
const isMid = () => MODE === 'medium';
const W = () => isMid() ? 720 : 960;
const H = () => isMid() ? 840 : 720;
// positions of the two panes; in medium both take the content area (capped at 528 dp, centred)
const MID_AREA = 'left: 112px; right: 16px; max-width: 528px; margin: 0 auto';
const listPos = w => isMid() ? MID_AREA : `left: 248px; width: ${w || 320}px`;
const panePos = l => isMid() ? MID_AREA : `left: ${l || 584}px; right: 16px`;
const fullPos = () => isMid() ? 'left: 112px; right: 16px' : 'left: 248px; right: 16px';
const BACK = '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="color: var(--ink)"><path d="m15 18-6-6 6-6"></path></svg>';
// back button shown in a detail pane on a medium window (goes back to the list)
const backBtn = () => isMid() ? `
        <button type="button" onClick="{{back}}" aria-label="Quay lại danh sách" style="width: 48px; height: 48px; flex: none; border-radius: 999px; border: 1px solid rgba(var(--edge),0.8); background:  rgba(var(--glass),0.6); display: flex; align-items: center; justify-content: center; cursor: pointer; padding: 0">${BACK}</button>` : '';
const TOK_L = '--switch-on:#2f5b3a;--switch-knob-on:#ffffff;--bg:#ece7e1;--bg-rgb:236,231,225;--bg2:#d9d1c6;--surface:#ffffff;--glass:255,255,255;--edge:255,255,255;--ink:#1c1b1a;--ink-rgb:28,27,26;--ink2:#3d3833;--ink2b:#4a4540;--ink3:#5c554e;--primary:#1c1b1a;--on-primary:#ffffff;--ok:#2f5b3a;--danger:#7a3420;--danger2:#8a2f1b;--warn:#6b4a10;--warn2:#6b5220;--warn3:#8a6a2a;--warn-bg:#fff4dc;--danger-bg:#f6e4dc;--dialog:#f4f1ec;';
const TOK_D = '--switch-on:#9fd3ac;--switch-knob-on:#1c1b1a;--bg:#151412;--bg-rgb:21,20,18;--bg2:#1f1c19;--surface:#2a2723;--glass:44,40,36;--edge:96,88,79;--ink:#f2eee9;--ink-rgb:242,238,233;--ink2:#d3cbc1;--ink2b:#c6bdb2;--ink3:#a99f94;--primary:#f2eee9;--on-primary:#1c1b1a;--ok:#9fd3ac;--danger:#f0a58e;--danger2:#f0a58e;--warn:#f0c878;--warn2:#e6c27a;--warn3:#e6c27a;--warn-bg:#3a2e14;--danger-bg:#3b221a;--dialog:#2a2723;';

// The side menu becomes a 72 dp icon rail in two cases:
// - the user presses the collapse button (data-nav="mini" on the board root);
// - the expanded window is narrow (840-900 dp) or short (under 480 dp, e.g. a phone held sideways).
//   The board root is the container, so this follows the board's own size (render-static's
//   844 x 390 variant), not the browser window. The collapse button is hidden then.
// Paragraphs are capped at about 80 characters a line.
const MINI = p => `
  ${p}.st-nav{width:72px !important;padding:12px 6px !important}
  ${p}.st-nav .st-hide-c{display:none !important}
  ${p}.st-nav a{justify-content:center !important;padding:6px !important}
  ${p}.st-nav .st-navhead{justify-content:center !important;padding:0 0 10px !important}`;
const COMPACT = `.st-root p,.st-root li{max-width:36em}
div.st-root{container-type:size}
${MINI('.st-root[data-nav="mini"] ')}
  .st-root[data-nav="mini"] .st-list{left:104px !important}
  .st-root[data-nav="mini"] .st-pane{left:440px !important}
  .st-root[data-nav="mini"] .st-full{left:104px !important}
@container (min-width: 840px) and ((max-width: 900px) or (max-height: 480px)){${MINI('')}
  .st-navtoggle{display:none !important}
  .st-list{left:104px !important;width:300px !important}
  .st-pane{left:420px !important}
  .st-full{left:104px !important}
}`;

// The dock changes shape as in the app (ActionDock.kt): a 56 dp round icon folds out into the glass bar, the tag circle grows 40 -> 52 dp and
// turns dark, the reserve button fades in after half way; 260 ms. Only the "from" frame is written: the end frame is the element's own style.
const DOCK_MS = '260ms cubic-bezier(.2,.8,.2,1) both';
const DOCK_CSS = ['a', 'b'].map(k => `
@keyframes st-dko${k}{from{width:56px;height:56px;background-color:var(--primary);box-shadow:0 6px 14px rgba(28,27,26,0.3)}}
@keyframes st-dkoc${k}{from{width:40px;height:40px;background-color:color-mix(in srgb,var(--on-primary) 16%,transparent)}}
@keyframes st-dkol${k}{from{opacity:1}to{opacity:0}}
@keyframes st-dkor${k}{0%,50%{opacity:0}100%{opacity:1}}
@keyframes st-dkx${k}{from{width:var(--dkw);height:68px;background-color:rgba(var(--glass),0.92);box-shadow:none}}
@keyframes st-dkxc${k}{from{width:52px;height:52px;background-color:var(--primary)}}
@keyframes st-dkxr${k}{from{opacity:1}to{opacity:0}}
.st-dk.dko-${k}{animation:st-dko${k} ${DOCK_MS}}.dko-${k} .st-dkc{animation:st-dkoc${k} ${DOCK_MS}}.dko-${k} .st-dkr{animation:st-dkor${k} ${DOCK_MS}}
.st-dk.dkc-${k}{animation:st-dkx${k} ${DOCK_MS}}.dkc-${k} .st-dkc{animation:st-dkxc${k} ${DOCK_MS}}.dkc-${k} .st-dkr{animation:st-dkxr${k} 130ms ease-out both}`).join('') + `
@media (prefers-reduced-motion: reduce){.st-dk,.st-dk .st-dkc,.st-dk .st-dkr{animation:none !important}}`;

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
@keyframes st-xa{from{opacity:0}to{opacity:1}}
@keyframes st-xb{from{opacity:0}to{opacity:1}}
@keyframes st-axr{from{opacity:0;transform:translateX(30px)}to{opacity:1;transform:none}}
@keyframes st-axl{from{opacity:0;transform:translateX(-30px)}to{opacity:1;transform:none}}
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
.st-pick{transition:background-color 180ms ease,scale 120ms ease}.st-pick:active{scale:.98}
@media (prefers-reduced-motion: reduce){
  .st-row,.st-rise,.st-item,.st-detail,.st-fade,.st-pop,.st-ping,.st-breathe,.st-spin{animation:none}
  .st-list,.st-detail{animation:none !important}
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
${DOCK_CSS}
</style>
</helmet>
<div class="st-root" data-theme="{{themeAttr}}" style="width: ${w}px; height: ${h}px; position: relative; overflow: hidden; background: var(--bg); font-family: 'Nunito', system-ui, sans-serif; color: var(--ink)" data-nav="{{navMode}}">
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
    return Object.assign(this.__layout(), {
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
    });
  }
  // side menu collapse (expanded window), list/detail switching (medium window) and pane transitions:
  // medium window slides the incoming pane 30 px along the x axis (forward from the right, back from the
  // left); expanded window fades the detail content in (the animation name alternates so it replays).
  __layout() {
    var self = this, st = this.state || {};
    var mini = !!st.navMini, mid = ${isMid()};
    var view = st.view || 'list', n = st.swapN || 0;
    var ease = ' 300ms cubic-bezier(.2,.8,.2,1) both';
    return {
      navMode: mini ? 'mini' : 'full', navOpen: !mini,
      navToggleLabel: mini ? 'Mở rộng menu' : 'Thu gọn menu',
      toggleNav: function () { self.setState({ navMini: !mini }); },
      showList: !mid || view === 'list', showDetail: !mid || view === 'detail',
      listAnim: mid && st.navDir === 'back' ? 'st-axl' + ease : 'st-fade 260ms ease-out both',
      detailAnim: mid ? 'st-axr' + ease : (n % 2 ? 'st-xb' : 'st-xa') + ' 150ms ease-out both',
      rowsStill: !!st.navDir,
      back: function () { self.setState({ view: 'list', navDir: 'back', swapN: n + 1 }); },
      toDetail: function () { var o = { swapN: n + 1, navDir: 'fwd' }; if (mid) o.view = 'detail'; self.setState(o); }
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
        var lay = self.__layout();
        return {
          title: o.title, author: o.author || '', mono: mono(o.title),
          status: o.status || '', hasStatus: !!o.status, pill: o.pill || '',
          hasNote: !!o.note, note: o.note || '', hasReason: !!o.reason, reason: o.reason || '',
          current: o.on ? 'true' : 'false', pick: function () { o.pick(); lay.toDetail(); },
          // rows rise once, on first load; coming back to the list does not replay it
          delay: lay.rowsStill ? 'animation:none' : 'animation-delay:' + Math.min(o.i || 0, 8) * 40 + 'ms',
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
  books: ['M4 6h16', 'M4 12h16M4 18h10'],
  login: ['M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4', 'm10 17 5-5-5-5M15 12H3']
};
// Same destinations and labels as the app ("Gợi ý" is the short name of the menu; the screen's own title is "Gợi ý cho bạn").
const MEMBER_MENU = [
  ['Danh mục', 'WideCatalog.dc.html', 'catalog'],
  ['Gợi ý', 'WideRecommend.dc.html', 'recommend'],
  ['Đang mượn', 'WideLoans.dc.html', 'loans'],
  ['Đặt trước', 'WideReservations.dc.html', 'reservations'],
  ['Lịch sử đọc', 'WideHistory.dc.html', 'history'],
  ['Tài khoản', 'WideAccount.dc.html', 'account']
];
const memberMenu = () => MEMBER_MENU;
// the guest menu: the catalog and the way to sign in
const GUEST_MENU = [['Danh mục', 'WideGuest.dc.html', 'catalog'], ['Đăng nhập', 'WideSignIn.dc.html', 'login']];
const NFC = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="color: var(--on-primary)"><path d="M6 8.5a6 6 0 0 1 0 7"></path><path d="M9.5 6a10 10 0 0 1 0 12"></path><path d="M13 3.5a14 14 0 0 1 0 17"></path></svg>';

const menuItem = (label, href, icon, active, i) => `
        <a href="${href}" class="st-item" aria-label="${label}" aria-current="${active ? 'page' : 'false'}" style="animation-delay: ${40 + 35 * i}ms; display: flex; align-items: center; gap: 12px; min-height: 52px; padding: 6px 10px; border-radius: 20px; text-decoration: none; color: var(--ink); font-size: 15px; font-weight: ${active ? 700 : 500}; background: ${active ? 'rgba(var(--glass),0.85)' : 'transparent'}">
          <span style="width: 36px; height: 36px; flex: none; border-radius: 12px; display: flex; align-items: center; justify-content: center; background: ${active ? 'var(--surface)' : 'rgba(var(--glass),0.5)'}">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="color: var(--ink)"><path d="${ICON[icon][0]}"></path><path d="${ICON[icon][1]}"></path></svg>
          </span>
          <span class="st-hide-c">${label}</span>
        </a>`;

// state preview (mockup only), dark like the phone boards' preview bar
// narrow = inside the 96 dp rail: one tab per line
const preview = (list, label, extraRow, cls, narrow) => `
    <div role="group" aria-label="Xem trước trạng thái (bản phác)" class="${cls || ''}" style="margin: 4px 0 ${narrow ? 0 : 10}px; padding: ${narrow ? 6 : 8}px; ${narrow ? 'width: 100%; box-sizing: border-box; ' : ''}border-radius: 22px; background:  rgba(28,27,26,0.82); display: flex; flex-direction: column; gap: 6px">
      <div style="font-size: 11px; font-weight: 600; color: rgba(255,255,255,0.85); padding: 0 ${narrow ? 2 : 6}px">${label}</div>
      <div style="display: flex; flex-${narrow ? 'direction: column' : 'wrap: wrap'}; gap: 2px">
        <sc-for list="{{${list}}}" as="t" hint-placeholder-count="3">
          <button type="button" aria-pressed="{{t.on}}" onClick="{{t.pick}}" style="{{t.style}}">{{t.label}}</button>
        </sc-for>
      </div>${extraRow || ''}
    </div>`;

const LOGIN_ICON = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="color: var(--on-primary); flex: none"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path><path d="m10 17 5-5-5-5"></path><path d="M15 12H3"></path></svg>';
const PANEL_ICON = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="color: var(--ink)"><rect x="3" y="4" width="18" height="16" rx="3"></rect><path d="M9 4v16"></path></svg>';
// Self-checkout is a round floating button at the bottom right, over the content, as in the app: it is not part of the menu or
// the rail. A guest has no such button (the menu has "Đăng nhập" instead); the checkout board itself does not draw it.
const dock = (guest, active) => guest || active === 'WideCheckout.dc.html' ? '' : `
  <a href="WideCheckout.dc.html" class="st-press st-dock" aria-label="Chạm sách để mượn" style="position: absolute; right: 16px; bottom: 16px; z-index: 30; width: 56px; height: 56px; border-radius: 999px; background: var(--primary); color: var(--on-primary); display: flex; align-items: center; justify-content: center; text-decoration: none; box-shadow: 0 10px 30px rgba(28,27,26,0.3)">${NFC.replace('width="20" height="20"', 'width="26" height="26"')}</a>`;
// The same button on a book's page: the round icon at rest, the glass bar (tag circle and reserve button) when a book is open.
// Needs in renderVals: dockCls, dockStyle, dockW (css width of the bar), reserveBtn (label and colours), reserve (handler), isDone.
const dockMorph = () => `
  <div class="st-dk {{dockCls}}" style="{{dockStyle}}">
    <a href="WideCheckout.dc.html" class="st-dkc" aria-label="{{dockTagLabel}}" style="{{dockCircle}}">${NFC.replace('width="20" height="20"', 'width="22" height="22"').replace('color: var(--on-primary)', 'color: var(--on-primary)')}</a>
    <button type="button" class="st-dkr st-swap st-press" onClick="{{reserve}}" aria-live="polite" aria-hidden="{{dockHidden}}" tabindex="{{dockTab}}" style="{{dockReserve}}">
      <sc-if value="{{reserveBtn.isDone}}" hint-placeholder-val="{{ false }}"><svg class="st-pop" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M6 3h12v18l-6-4-6 4z"></path><path class="st-draw" d="m9 10 2 2 4-4"></path></svg></sc-if>
      <sc-if value="{{reserveBtn.isIdle}}" hint-placeholder-val="{{ true }}"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M6 3h12v18l-6-4-6 4z"></path></svg></sc-if>
      {{reserveBtn.label}}
    </button>
  </div>`;
// renderVals code for dockMorph: open = a book is open (always on the expanded window, the detail view on the medium one)
const DOCK_JS = () => `
    var dkOpen = ${isMid()} ? (st.view === 'detail') : true, dkN = (st.swapN || 0);
    var dkW = ${isMid()} ? 360 : 336;
    var dkKey = (dkOpen ? 'dko-' : 'dkc-') + (dkN % 2 ? 'a' : 'b');
    var dock = {
      dockCls: (!dkOpen && dkN === 0) ? '' : dkKey,
      dockTagLabel: dkOpen ? 'Mượn bằng cách chạm điện thoại vào sách' : 'Chạm sách để mượn',
      dockTab: dkOpen ? '0' : '-1', dockHidden: dkOpen ? 'false' : 'true',
      dockStyle: 'position:absolute;right:16px;bottom:16px;z-index:30;--dkw:' + dkW + 'px;display:flex;align-items:center;gap:8px;box-sizing:border-box;padding:8px;border-radius:999px;overflow:hidden;' +
        (dkOpen ? 'width:' + dkW + 'px;height:68px;background:rgba(var(--glass),0.92);border:1px solid rgba(var(--edge),0.8)' : 'width:56px;height:56px;background:var(--primary);border:1px solid transparent;box-shadow:0 6px 14px rgba(28,27,26,0.3)'),
      dockCircle: 'flex:none;border-radius:999px;display:flex;align-items:center;justify-content:center;text-decoration:none;' +
        (dkOpen ? 'width:52px;height:52px;background:var(--primary)' : 'width:40px;height:40px;background:color-mix(in srgb,var(--on-primary) 16%,transparent)'),
      dockReserve: rb.style + ';min-height:0;height:52px;min-width:0;white-space:normal;line-height:1.15;text-align:center;padding:0 12px;' + (dkOpen ? 'opacity:1' : 'opacity:0;pointer-events:none')
    };`;

// room kept under the panes for that button (the app keeps the same clearance)
const BOT = 88;

// navigation; active = href of the current item (or '' for none).
// opts.preview: state preview group (mockup only); opts.guest: the reduced guest menu
const nav = (active, opts) => {
  opts = opts || {};
  const menu = opts.guest ? GUEST_MENU : memberMenu();
  const own = opts.dock === false ? () => '' : dock;
  if (isMid()) return rail(active, opts, menu) + own(opts.guest, active);
  const items = menu.map((m, i) => menuItem(m[0], m[1], m[2], m[1] === active, i)).join('');
  return `
  <nav aria-label="Điều hướng chính" class="st-nav" style="position: absolute; left: 16px; top: 16px; bottom: 16px; width: 216px; padding: 14px 10px 12px; box-sizing: border-box; border-radius: 30px; background:  rgba(var(--glass),0.62); backdrop-filter: blur(26px) saturate(160%); -webkit-backdrop-filter: blur(26px) saturate(160%); border: 1px solid rgba(var(--edge),0.85); display: flex; flex-direction: column; gap: 2px">
    <div class="st-navhead" style="display: flex; align-items: center; padding: 0 0 10px 4px">
      <button type="button" class="st-navtoggle st-press" onClick="{{toggleNav}}" aria-label="{{navToggleLabel}}" aria-expanded="{{navOpen}}" style="width: 48px; height: 48px; flex: none; border-radius: 999px; border: 0; background: rgba(var(--glass),0.85); display: flex; align-items: center; justify-content: center; cursor: pointer; padding: 0">${PANEL_ICON}</button>
    </div>
    <div class="st-scroll" style="flex: 1; min-height: 0; overflow-y: auto; display: flex; flex-direction: column; gap: 2px; padding-bottom: 12px">${items}
    </div>${opts.preview ? preview(opts.preview[0], opts.preview[1], '', 'st-hide-c') : ''}
  </nav>` + own(opts.guest, active);
};

// navigation rail for a medium window: the destinations with short labels, the current one on a filled block
const rail = (active, opts, menu) => {
  const items = menu.map((m, i) => {
    const on = m[1] === active, href = m[1] === 'WideCatalog.dc.html' ? 'RailCatalog.dc.html' : m[1];
    return `
      <a href="${href}" class="st-item" aria-label="${m[0]}" aria-current="${on ? 'page' : 'false'}" style="animation-delay: ${40 + 35 * i}ms; width: 80px; min-height: 64px; padding: 8px 0; box-sizing: border-box; border-radius: 20px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; text-decoration: none; color: var(--ink); font-size: 12px; line-height: 1.2; text-align: center; font-weight: ${on ? 700 : 500}; background: ${on ? 'var(--surface)' : 'transparent'}">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="color: var(--ink)"><path d="${ICON[m[2]][0]}"></path><path d="${ICON[m[2]][1]}"></path></svg>
        <span>${m[0]}</span>
      </a>`;
  }).join('');
  return `
  <nav aria-label="Điều hướng chính" style="position: absolute; left: 0; top: 0; bottom: 0; width: 96px; padding: 48px 8px 12px; box-sizing: border-box; background:  rgba(var(--glass),0.62); backdrop-filter: blur(26px) saturate(160%); -webkit-backdrop-filter: blur(26px) saturate(160%); border-right: 1px solid rgba(var(--edge),0.85); display: flex; flex-direction: column; align-items: center; gap: 4px">
    <div class="st-scroll" style="flex: 1; min-height: 0; width: 100%; overflow-y: auto; display: flex; flex-direction: column; align-items: center; gap: 4px">${items}
    </div>${opts.preview ? preview(opts.preview[0], opts.preview[1], '', '', true) : ''}
  </nav>`;
};

// list pane: title, optional extra html between title and list, rows bound to `list`
const listPane = (label, title, extra, list, after) => `
  <sc-if value="{{showList}}" hint-placeholder-val="{{ true }}">
  <section aria-label="${label}" class="st-list st-fade" style="animation: {{listAnim}}; position: absolute; ${listPos()}; top: ${isMid() ? 40 : 16}px; bottom: 0; display: flex; flex-direction: column; gap: 12px">
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
                <sc-if value="{{b.hasStatus}}" hint-placeholder-val="{{ true }}"><span style="{{b.pill}}">{{b.status}}</span></sc-if>
                <sc-if value="{{b.hasNote}}" hint-placeholder-val="{{ false }}"><span style="font-size: 12px; color: var(--ink3)">{{b.note}}</span></sc-if>
              </span>
              <sc-if value="{{b.hasReason}}" hint-placeholder-val="{{ false }}"><span style="font-size: 12px; font-weight: 700; line-height: 1.35; color: var(--warn2)">{{b.reason}}</span></sc-if>
            </span>
          </button>
        </li>
      </sc-for>${after || ''}
    </ul>
  </section>
  </sc-if>`;

// detail pane: header row with a label, scrollable body, optional action bar pinned at the bottom
const detailPane = (label, body, bar) => `
  <sc-if value="{{showDetail}}" hint-placeholder-val="{{ true }}">
  <section aria-label="${label}" class="st-pane" style="position: absolute; ${panePos()}; top: 16px; bottom: ${BOT}px; border-radius: 30px; overflow: hidden; background:  rgba(var(--glass),0.55); border: 1px solid rgba(var(--edge),0.85)">
    <div class="st-scroll st-detail" style="animation: {{detailAnim}}; position: absolute; inset: 0; padding: 12px 20px ${bar ? 96 : 24}px; box-sizing: border-box; overflow-y: auto; display: flex; flex-direction: column; gap: 16px">
      <div style="display: flex; align-items: center; gap: 8px; min-height: 48px">${backBtn()}
        <div style="flex: 1; font-size: 13px; font-weight: 600; color: var(--ink3)">${label}</div>
        <div style="width: 84px; flex: none"></div>
      </div>${body}
    </div>${bar ? `
    <div style="position: absolute; left: 12px; right: 12px; bottom: 12px; padding: 8px; border-radius: 999px; background:  rgba(var(--glass),0.7); backdrop-filter: blur(22px) saturate(150%); -webkit-backdrop-filter: blur(22px) saturate(150%); border: 1px solid rgba(var(--edge),0.9); display: flex; gap: 8px">${bar}
    </div>` : ''}
  </section>
  </sc-if>`;

// book header used in detail panes: cover + tags + title + author, bound to `sel`
const bookHead = (tags, noAuthor) => `
      <div style="display: flex; gap: 16px; align-items: flex-start">
        <div aria-hidden="true" style="{{sel.coverStyle}}">
          <div style="font-size: 17px; font-weight: 700; line-height: 1.15">{{sel.title}}</div>
${noAuthor ? '' : `
          <div style="font-size: 11px; opacity: 0.85">{{sel.author}}</div>`}
        </div>
        <div style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8px">${tags ? `
          <div style="display: flex; gap: 6px; flex-wrap: wrap">
            <sc-for list="{{sel.tags}}" as="t" hint-placeholder-count="2">
              <span style="padding: 5px 10px; border-radius: 999px; background:  rgba(var(--glass),0.8); font-size: 12px; font-weight: 500; color: var(--ink2)">{{t.label}}</span>
            </sc-for>
          </div>` : ''}
          <h2 style="margin: 0; font-size: 24px; line-height: 1.15; font-weight: 700; letter-spacing: -0.2px">{{sel.title}}</h2>
${noAuthor ? '' : `
          <div style="font-size: 14px; color: var(--ink2b)">{{sel.author}}</div>`}
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


// ---------- Catalog home (member) and guest home, as the app draws them ----------
// Greeting or guest heading, search, a topic banner with covers, then strips of covers with a plate (author, copies). The page
// scrolls; the text column is capped (600 dp next to the rail, 720 dp next to the menu). The links "Xem tất cả" and the banner go to the list
// (a list beside the detail on the expanded window, `list`).
const home = (guest, list) => `
  <section aria-label="${guest ? 'Danh mục (khách)' : 'Danh mục'}" class="st-scroll" style="position: absolute; ${fullPos()}; max-width: ${isMid() ? 600 : 720}px; top: 0; bottom: 0; padding: 0 0 ${BOT + 16}px; box-sizing: border-box; overflow-y: auto; display: flex; flex-direction: column; gap: 18px">
    <div style="padding: ${isMid() ? 36 : 40}px 0 8px; font-size: 22px; font-weight: 600; color: var(--ink2b)">Danh mục</div>
    <div class="st-rise" style="display: flex; flex-direction: column; gap: 4px">
      <div style="font-size: 14px; color: var(--ink3)">${guest ? 'Bạn đang xem với tư cách khách' : 'Chào bạn,'}</div>
      <h1 style="margin: 0; font-size: 32px; line-height: 1.15; font-weight: 700; letter-spacing: -0.3px">${guest ? 'Tìm sách trong thư viện' : 'Hôm nay đọc gì?'}</h1>
    </div>
    <a href="${list}" aria-label="Tìm sách" class="st-rise" style="display: flex; align-items: center; gap: 10px; height: 52px; flex: none; padding: 0 18px; border-radius: 999px; background:  rgba(var(--glass),0.75); border: 1px solid rgba(var(--ink-rgb),0.5); text-decoration: none; color: var(--ink3); font-size: 15px">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="color: var(--ink3)"><circle cx="11" cy="11" r="7"></circle><path d="m20 20-3.5-3.5"></path></svg>
      Tên sách, tác giả, ISBN
    </a>${guest ? `
    <p class="st-rise" style="margin: 0; flex: none; padding: 14px 16px; border-radius: 22px; border: 1px dashed rgba(var(--ink-rgb),0.2); font-size: 13px; line-height: 1.5; color: var(--ink2b)">Khách xem và tìm sách được. Đặt trước, mượn, gia hạn và lịch sử đọc cần tài khoản thư viện.</p>` : ''}
    <a href="${list}" class="st-rise" style="position: relative; height: 200px; flex: none; border-radius: 28px; overflow: hidden; display: block; text-decoration: none; background:  #2f4a44">
      <div aria-hidden="true" style="position: absolute; inset: 0; display: flex; gap: 12px; padding: 22px 0 0 22px">
        <sc-for list="{{banner}}" as="c" hint-placeholder-count="2">
          <div class="st-rise" style="{{c.style}}"><div style="font-size: 17px; font-weight: 700; line-height: 1.2">{{c.title}}</div></div>
        </sc-for>
      </div>
      <div aria-hidden="true" style="position: absolute; left: 0; right: 0; bottom: 0; height: 110px; background:  linear-gradient(to bottom, rgba(20,30,27,0), rgba(20,30,27,0.75))"></div>
      <div style="position: absolute; left: 20px; right: 16px; bottom: 16px; display: flex; align-items: flex-end; justify-content: space-between">
        <div style="display: flex; flex-direction: column; gap: 2px">
          <div style="font-size: 12px; font-weight: 500; color: rgba(255,255,255,0.9)">Chủ đề</div>
          <div style="font-size: 22px; font-weight: 700; color: #ffffff">Thiếu nhi</div>
        </div>
        <span style="width: 44px; height: 44px; border-radius: 999px; background:  rgba(var(--glass),0.25); border: 1px solid rgba(var(--edge),0.5); display: flex; align-items: center; justify-content: center">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M7 17 17 7"></path><path d="M8 7h9v9"></path></svg>
        </span>
      </div>
    </a>
    ${guest ? '' : `
    <div style="display: flex; align-items: baseline; justify-content: space-between; flex: none">
      <h2 style="margin: 0; font-size: 18px; font-weight: 700">Gợi ý cho bạn</h2>
      <a href="${isMid() ? 'MidRecommend.dc.html' : 'WideRecommend.dc.html'}" style="font-size: 13px; font-weight: 500; color: var(--ink3); padding: 12px 0 12px 12px; margin: -12px 0">Xem tất cả</a>
    </div>
    <div style="display: flex; gap: 12px; overflow-x: auto; scrollbar-width: none; flex: none">
      <sc-for list="{{recs}}" as="c" hint-placeholder-count="3">
        <div style="width: 132px; flex: none; display: flex; flex-direction: column; gap: 8px">
          <a href="${list}" class="st-rise" style="{{c.cardStyle}}">
            <div style="font-size: 15px; font-weight: 700; line-height: 1.2; padding: 14px 12px 124px">{{c.title}}</div>
            <div style="position: absolute; left: 6px; right: 6px; bottom: 6px; padding: 8px 10px; border-radius: 14px; background:  rgba(var(--glass),0.7); color: var(--ink)">
              <div style="font-size: 12px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{{c.author}}</div>
              <div style="font-size: 11px; color: var(--ink)">{{c.status}}</div>
            </div>
          </a>
          <div style="font-size: 12px; font-weight: 700; line-height: 1.3; color: var(--warn)">{{c.reason}}</div>
        </div>
      </sc-for>
    </div>
    <div style="font-size: 11px; color: var(--ink3); flex: none">Tính trên máy từ sách bạn đã mượn. Tắt trong Tài khoản.</div>`}
    ${['Thiếu nhi', 'Tiểu thuyết'].map((t, i) => `
    <div style="display: flex; align-items: baseline; justify-content: space-between; flex: none">
      <h2 style="margin: 0; font-size: 18px; font-weight: 700">${t}</h2>
      <a href="${list}" style="font-size: 13px; font-weight: 500; color: var(--ink3); padding: 12px 0 12px 12px; margin: -12px 0">Xem tất cả</a>
    </div>
    <div style="display: flex; gap: 12px; overflow-x: auto; scrollbar-width: none; flex: none">
      <sc-for list="{{${i ? 'row' : 'kids'}}}" as="c" hint-placeholder-count="3">
        <a href="${list}" class="st-rise" style="{{c.cardStyle}}">
          <div style="font-size: 15px; font-weight: 700; line-height: 1.2; padding: 14px 12px 124px">{{c.title}}</div>
          <div style="position: absolute; left: 6px; right: 6px; bottom: 6px; padding: 8px 10px; border-radius: 14px; background:  rgba(var(--glass),0.7); color: var(--ink)">
            <div style="font-size: 12px; font-weight: 600">{{c.author}}</div>
            <div style="font-size: 11px; color: var(--ink)">{{c.status}}</div>
          </div>
        </a>
      </sc-for>
    </div>`).join('')}
  </section>`;
const HOME_JS = `
  renderVals() {
    var ki = 0, ci = 0;
    var cover = function (bg, fg) {
      return 'animation-delay:' + (160 + 90 * ki++) + 'ms;width:104px;height:150px;flex:none;border-radius:16px;background:' + bg + ';color:' + fg + ';padding:14px 12px;box-sizing:border-box;box-shadow:0 8px 20px rgba(0,0,0,0.18)';
    };
    var card = function (bg, fg) {
      return 'animation-delay:' + (140 + 70 * ci++) + 'ms;position:relative;display:block;width:132px;min-height:196px;flex:none;border-radius:22px;overflow:hidden;text-decoration:none;background:' + bg + ';color:' + fg;
    };
    return Object.assign(this.__theme(), {
      banner: [
        { title: 'Số đỏ', style: cover('#b5452b', '#ffffff') },
        { title: 'Nhà giả kim', style: cover('#c9a66b', '#1c1b1a') }
      ],
      kids: [
        { title: 'Dế Mèn phiêu lưu ký', author: 'Tô Hoài', status: 'Còn 3 bản', cardStyle: card('#b5452b', '#ffffff') },
        { title: 'Nhà giả kim', author: 'Paulo Coelho', status: 'Đã cho mượn hết', cardStyle: card('#c9a66b', '#1c1b1a') }
      ],
      recs: [
        { title: 'Vũ trụ trong vỏ hạt dẻ', author: 'Stephen Hawking', status: 'Còn 3 bản', reason: 'Cùng chủ đề Khoa học', cardStyle: card('#7a2e1e', '#ffffff') },
        { title: 'Tôi thấy hoa vàng trên cỏ xanh', author: 'Nguyễn Nhật Ánh', status: 'Còn 2 bản', reason: 'Cùng chủ đề Thiếu nhi', cardStyle: card('#7a2e1e', '#ffffff') },
        { title: 'Cho tôi xin một vé đi tuổi thơ', author: 'Nguyễn Nhật Ánh', status: 'Đang được mượn · đặt trước', reason: 'Cùng chủ đề Thiếu nhi', cardStyle: card('#b5452b', '#ffffff') }
      ],
      row: [
        { title: 'Số đỏ', author: 'Vũ Trọng Phụng', status: 'Còn 2 bản', cardStyle: card('#b5452b', '#ffffff') },
        { title: 'Tắt đèn', author: 'Ngô Tất Tố', status: 'Đã cho mượn hết', cardStyle: card('#7a2e1e', '#ffffff') },
        { title: 'Chí Phèo', author: 'Nam Cao', status: 'Còn 1 bản', cardStyle: card('#3e3a36', '#ffffff') },
        { title: 'Lão Hạc', author: 'Nam Cao', status: 'Còn 1 bản', cardStyle: card('#d8c9a8', '#3e3a36') }
      ]
    });
  }`;

module.exports = { home, HOME_JS, head, foot, nav, preview, listPane, detailPane, bookHead, fact, facts, BOOKS, searchAndChips, SEARCH_JS, btnPrimary, btnGlass, note, NFC, menuItem,
  setMode, isMid, W, H, listPos, panePos, fullPos, backBtn, BOT, dock, dockMorph, DOCK_JS };
