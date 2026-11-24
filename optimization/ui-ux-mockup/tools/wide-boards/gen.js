// Writes the wide boards into the mockup folder (CRLF, like the other boards) and lays them out in canvas.json.
// Usage (from optimization/ui-ux-mockup): node tools/wide-boards/gen.js .
// shell.js holds the shared pieces (menu, panes, theme, preview bar); boards1.js and boards2.js the boards.
// WideCatalog and RailCatalog are written by hand and are not generated here.
const fs = require('fs'), path = require('path');
const dir = process.argv[2];
const boards = Object.assign({}, require('./boards1.js'), require('./boards2.js'));
for (const [name, html] of Object.entries(boards)) {
  fs.writeFileSync(path.join(dir, name), html.replace(/\r?\n/g, '\r\n'));
  console.log('wrote', name, html.length);
}
const cpath = path.join(dir, 'canvas.json');
const c = JSON.parse(fs.readFileSync(cpath, 'utf8'));
const place = (name, x, y, w, h, title) => {
  c.boards[name] = { x, y, w, h, title, radius: 32, is_interactive: true };
  if (!c.order.includes(name)) c.order.push(name);
};
const Y1 = 4896, Y2 = Y1 + 840 + 160, Y3 = Y2 + 720 + 160, Y4 = Y3 + 720 + 160;
place('WideGuest.dc.html', 1840, Y1, 960, 720, 'Rộng: chế độ khách');
place('WideLoans.dc.html', 0, Y2, 960, 720, 'Rộng: sách đang mượn');
place('WideReservations.dc.html', 1040, Y2, 960, 720, 'Rộng: đặt trước');
place('WideHistory.dc.html', 2080, Y2, 960, 720, 'Rộng: lịch sử đọc');
place('WideRecommend.dc.html', 0, Y3, 960, 720, 'Rộng: gợi ý cho bạn');
place('WideOfflineSearch.dc.html', 1040, Y3, 960, 720, 'Rộng: tìm khi không có mạng');
place('WideCheckout.dc.html', 2080, Y3, 960, 720, 'Rộng: tự mượn sách');
place('WideAccount.dc.html', 0, Y4, 960, 720, 'Rộng: tài khoản và thông báo');
place('WideSignIn.dc.html', 1040, Y4, 960, 720, 'Rộng: màn chào, đăng nhập, thẻ, đổi mật khẩu');
place('WideStatus.dc.html', 2080, Y4, 960, 720, 'Rộng: khởi động, khóa, mất kết nối, đăng xuất');
c.notes.wideRow.maxW = 3040;
fs.writeFileSync(cpath, JSON.stringify(c));
console.log('canvas boards', Object.keys(c.boards).length);
