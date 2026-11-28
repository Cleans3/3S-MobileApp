// Writes the boards for larger windows into the mockup folder (CRLF, like the other boards) and lays
// them out in canvas.json.
// Usage (from optimization/ui-ux-mockup): node tools/wide-boards/gen.js .
// shell.js holds the shared pieces (menu or rail, panes, theme, preview bar); boards1.js and boards2.js
// build the boards for the current size. Each board is built twice:
//   Wide*.dc.html  960 x 720, expanded window (840 dp and up)
//   Mid*.dc.html   720 x 840, medium window (600-840 dp)
// Catalog: the home is RailCatalog (medium) and WideCatalog (expanded); the list beside the detail is MidCatalog and WideList.
const fs = require('fs'), path = require('path');
const S = require('./shell.js');
const dir = process.argv[2];
const build = () => Object.assign({}, require('./boards1.js')(), require('./boards2.js')());
const write = (name, html) => { fs.writeFileSync(path.join(dir, name), html.replace(/\r?\n/g, '\r\n')); console.log('wrote', name, html.length); };

S.setMode('wide');
for (const [name, html] of Object.entries(build())) write(name, html);
S.setMode('medium');
for (const [name, html] of Object.entries(build())) {
  // links between boards point at the medium versions
  write(name.replace(/^Wide/, 'Mid'), html.replace(/\bWide(\w+)\.dc\.html/g, 'Mid$1.dc.html'));
}

const cpath = path.join(dir, 'canvas.json');
const c = JSON.parse(fs.readFileSync(cpath, 'utf8'));
const place = (name, x, y, w, h, title) => {
  c.boards[name] = { x, y, w, h, title, radius: 32, is_interactive: true };
  c.order = c.order.filter(n => n !== name).concat(name);
};
// expanded window (840 dp and up): four rows of three
const Y1 = 4896, Y2 = Y1 + 720 + 160, Y3 = Y2 + 720 + 160, Y4 = Y3 + 720 + 160;
place('WideCatalog.dc.html', 0, Y1, 960, 720, 'Rộng: danh mục');
place('WideGuest.dc.html', 1040, Y1, 960, 720, 'Rộng: chế độ khách');
place('WideOfflineSearch.dc.html', 2080, Y1, 960, 720, 'Rộng: tìm khi không có mạng');
place('WideLoans.dc.html', 0, Y2, 960, 720, 'Rộng: sách đang mượn');
place('WideReservations.dc.html', 1040, Y2, 960, 720, 'Rộng: đặt trước');
place('WideHistory.dc.html', 2080, Y2, 960, 720, 'Rộng: lịch sử đọc');
place('WideRecommend.dc.html', 0, Y3, 960, 720, 'Rộng: gợi ý cho bạn');
place('WideCheckout.dc.html', 1040, Y3, 960, 720, 'Rộng: tự mượn sách');
place('WideAccount.dc.html', 2080, Y3, 960, 720, 'Rộng: tài khoản và thông báo');
place('WideSignIn.dc.html', 0, Y4, 960, 720, 'Rộng: màn chào, đăng nhập, thẻ, đổi mật khẩu');
place('WideStatus.dc.html', 1040, Y4, 960, 720, 'Rộng: khởi động, khóa, mất kết nối, đăng xuất');
place('WideList.dc.html', 2080, Y4, 960, 720, 'Rộng: danh sách và chi tiết sách');
c.notes.wideRow = { x: 0, y: Y1 - 260, text: 'Cửa sổ rộng từ 840 dp (máy gập cầm ngang, tablet, điện thoại xoay ngang)', kind: 'title1', maxW: 3040 };
// medium window (600-840 dp): three rows of four
const M1 = Y4 + 720 + 420, M2 = M1 + 840 + 160, M3 = M2 + 840 + 160, X = i => i * 800;
const mid = [
  ['RailCatalog.dc.html', 'Vừa: trang đầu danh mục'], ['MidCatalog.dc.html', 'Vừa: danh sách rồi chi tiết sách'],
  ['MidGuest.dc.html', 'Vừa: chế độ khách'], ['MidOfflineSearch.dc.html', 'Vừa: tìm khi không có mạng'],
  ['MidLoans.dc.html', 'Vừa: sách đang mượn'], ['MidReservations.dc.html', 'Vừa: đặt trước'],
  ['MidHistory.dc.html', 'Vừa: lịch sử đọc'], ['MidRecommend.dc.html', 'Vừa: gợi ý cho bạn'],
  ['MidCheckout.dc.html', 'Vừa: tự mượn sách'], ['MidAccount.dc.html', 'Vừa: tài khoản và thông báo'],
  ['MidSignIn.dc.html', 'Vừa: màn chào, đăng nhập, thẻ, đổi mật khẩu'], ['MidStatus.dc.html', 'Vừa: khởi động, khóa, mất kết nối, đăng xuất']
];
mid.forEach(([n, t], i) => place(n, X(i % 4), [M1, M2, M3][Math.floor(i / 4)], 720, 840, t));
c.notes.midRow = { x: 0, y: M1 - 260, text: 'Cửa sổ vừa 600–840 dp (máy gập mở gần vuông, tablet nhỏ cầm dọc): thanh điều hướng dọc, mỗi lúc một khung', kind: 'title1', maxW: 3120 };
fs.writeFileSync(cpath, JSON.stringify(c));
console.log('canvas boards', Object.keys(c.boards).length);
