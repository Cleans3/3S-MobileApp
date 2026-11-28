// List-detail boards for the wide window: loans, reservations, history, offline search, recommendations.
const S = require('./shell.js');
module.exports = function () {
const W = S.W(), H = S.H();
const out = {};

// ---------- Sách đang mượn ----------
out['WideLoans.dc.html'] = S.head('Sách đang mượn ở cửa sổ rộng', W, H) + S.nav('WideLoans.dc.html') +
  S.listPane('Danh sách sách đang mượn', 'Đang mượn', `
    <div style="display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; flex: none">
      <div style="padding: 12px; border-radius: 18px; background: var(--surface); display: flex; flex-direction: column; gap: 2px">
        <div style="font-size: 11px; color: var(--ink3)">Đang mượn</div>
        <div style="font-size: 15px; font-weight: 700">2</div>
      </div>
      <div style="padding: 12px; border-radius: 18px; background: var(--danger-bg); display: flex; flex-direction: column; gap: 2px">
        <div style="font-size: 11px; color: var(--danger)">Tiền phạt chưa trả</div>
        <div style="font-size: 15px; font-weight: 700; color: var(--danger)">2.000 ₫</div>
      </div>
    </div>`, 'rows') +
  S.detailPane('Chi tiết lượt mượn', S.bookHead(false, true) + `
      <sc-if value="{{isDue}}" hint-placeholder-val="{{ true }}">${S.facts([
        S.fact('Hạn trả', '{{dueDate}}'),
        S.fact('Còn lại', '{{dueLeft}}')
      ])}
        <button type="button" onClick="{{renew}}" aria-live="polite" class="st-swap st-press" style="{{renewBtn.style}}; flex: none; align-self: flex-start">
          <sc-if value="{{renewBtn.isDone}}" hint-placeholder-val="{{ false }}"><svg class="st-pop" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path class="st-draw" d="m5 12 5 5 9-10"></path></svg></sc-if>
          {{renewBtn.label}}
        </button>
      </sc-if>
      <sc-if value="{{isOverdue}}" hint-placeholder-val="{{ false }}">${S.facts([
        S.fact('Hạn trả', '{{dueDate}}'),
        '<div style="padding: 12px; border-radius: 18px; background: var(--danger-bg); display: flex; flex-direction: column; gap: 2px"><div style="font-size: 11px; color: var(--danger)">Còn lại</div><div style="font-size: 15px; font-weight: 700; line-height: 1.25; color: var(--danger)">Quá hạn 2 ngày</div></div>'
      ])}
        <div style="font-size: 15px; font-weight: 700; color: var(--danger)">Quá hạn · phạt 2.000 ₫</div>
        <div style="font-size: 14px; line-height: 1.5; color: var(--ink2b)">Không gia hạn được vì đã quá hạn. Hãy trả sách và thanh toán tại quầy.</div>
      </sc-if>`) +
  S.foot(W, H, `
  renderVals() {
    var self = this, st = this.state || {}, ui = this.__ui();
    var phase = st.phase || 'idle';
    var loans = [
      { id: 'sodo', t: 'Số đỏ', a: 'Vũ Trọng Phụng', bg: '#b5452b', fg: '#ffffff', over: false },
      { id: 'chipheo', t: 'Chí Phèo', a: 'Nam Cao', bg: '#3e3a36', fg: '#ffffff', over: true }
    ];
    var sel = loans.filter(function (l) { return l.id === (st.sel || 'sodo'); })[0];
    var due = phase === 'done' ? '21/12/2026' : '07/12/2026';
    return Object.assign(this.__theme(), {
      rows: loans.map(function (l, i) {
        return ui.row({ i: i, title: l.t, author: l.a, bg: l.bg, fg: l.fg, on: l.id === sel.id,
          status: l.over ? 'Quá hạn 2 ngày' : 'Hạn trả ' + due, pill: ui.pill(l.over ? 'bad' : 'ok'),
          pick: function () { self.setState({ sel: l.id }); } });
      }),
      sel: { title: sel.t, author: sel.a, coverStyle: ui.bigCover(sel.bg, sel.fg) },
      isDue: !sel.over, isOverdue: sel.over,
      dueDate: sel.over ? '05/12/2026' : phase === 'done' ? '21/12/2026 (mới)' : '07/12/2026',
      dueLeft: phase === 'done' ? '28 ngày' : '14 ngày',
      renewBtn: ui.phaseBtn(phase, { idle: 'Gia hạn', sending: 'Đang gửi yêu cầu…', done: 'Đã gia hạn · thư viện xác nhận' }),
      renew: function () {
        if (phase === 'sending') return;
        if (phase === 'done') { self.setState({ phase: 'idle' }); return; }
        self.setState({ phase: 'sending' });
        self.timer = setTimeout(function () { self.setState({ phase: 'done' }); }, 1200);
      }
    });
  }`);

// ---------- Đặt trước ----------
out['WideReservations.dc.html'] = S.head('Đặt trước ở cửa sổ rộng', W, H) + S.nav('WideReservations.dc.html') +
  S.listPane('Danh sách sách đặt trước', 'Đặt trước', `
    <sc-if value="{{cancelled}}" hint-placeholder-val="{{ false }}">
      <div role="status" class="st-fade" style="padding: 12px 14px; border-radius: 18px; background: var(--surface); font-size: 14px; font-weight: 600">Đã hủy đặt trước Clean Code.</div>
    </sc-if>`, 'rows') +
  S.detailPane('Chi tiết đặt trước', S.bookHead(false, true) + `
      <sc-if value="{{isReady}}" hint-placeholder-val="{{ true }}">
        <div style="padding: 14px 16px; border-radius: 20px; background: var(--surface); display: flex; flex-direction: column; gap: 4px">
          <div style="font-size: 15px; font-weight: 700; color: var(--ok)">Sẵn sàng nhận</div>
          <div style="font-size: 14px; line-height: 1.5; color: var(--ink2b)">Giữ cho bạn tại quầy đến 26/11/2026.</div>
        </div>
      </sc-if>
      <sc-if value="{{isWaiting}}" hint-placeholder-val="{{ false }}">
        <div style="padding: 14px 16px; border-radius: 20px; background: var(--surface); display: flex; flex-direction: column; gap: 4px">
          <div style="font-size: 15px; font-weight: 700">Đang chờ</div>
          <div style="font-size: 14px; line-height: 1.5; color: var(--ink2b)">Bạn sẽ được báo khi có sách.</div>
        </div>
        <sc-if value="{{notAsking}}" hint-placeholder-val="{{ true }}">
          <button type="button" onClick="{{ask}}" class="st-press" style="align-self: flex-start; min-height: 48px; padding: 6px 20px; border-radius: 999px; border: 1px solid var(--danger); background:  transparent; color: var(--danger); font: inherit; font-size: 15px; font-weight: 600; cursor: pointer">Hủy đặt trước</button>
        </sc-if>
        <sc-if value="{{asking}}" hint-placeholder-val="{{ false }}">
          <div role="alertdialog" aria-label="Xác nhận hủy đặt trước" class="st-fade" style="padding: 16px; border-radius: 22px; background: var(--dialog); border: 1px solid rgba(var(--ink-rgb),0.5); display: flex; flex-direction: column; gap: 12px">
            <div style="font-size: 16px; font-weight: 700">Hủy đặt trước Clean Code?</div>
            <div style="display: flex; gap: 8px; flex-wrap: wrap">
              <button type="button" onClick="{{keep}}" style="${S.btnGlass}; flex: 1">Giữ lại</button>
              <button type="button" onClick="{{cancel}}" style="flex: 1; min-height: 52px; padding: 6px 16px; border-radius: 999px; border: 0; background:  #7a3420; color: #ffffff; font: inherit; font-size: 15px; font-weight: 600; cursor: pointer">Hủy đặt trước</button>
            </div>
          </div>
        </sc-if>
      </sc-if>`) +
  S.foot(W, H, `
  renderVals() {
    var self = this, st = this.state || {}, ui = this.__ui();
    var flow = st.flow || 'list';
    var items = [
      { id: 'kieu', t: 'Truyện Kiều', a: 'Nguyễn Du', bg: '#e7dcc6', fg: '#2f4a44', ready: true },
      { id: 'clean', t: 'Clean Code', a: 'Robert C. Martin', bg: '#2f4a44', fg: '#ffffff', ready: false }
    ].filter(function (x) { return !(x.id === 'clean' && flow === 'cancelled'); });
    var sel = items.filter(function (x) { return x.id === (st.sel || 'clean'); })[0] || items[0];
    return Object.assign(this.__theme(), {
      rows: items.map(function (x, i) {
        return ui.row({ i: i, title: x.t, author: x.a, bg: x.bg, fg: x.fg, on: x.id === sel.id,
          status: x.ready ? 'Sẵn sàng nhận' : 'Đang chờ', pill: ui.pill(x.ready ? 'ok' : 'wait'),
          note: x.ready ? 'đến 26/11' : '', pick: function () { self.setState({ sel: x.id }); } });
      }),
      sel: { title: sel.t, author: sel.a, coverStyle: ui.bigCover(sel.bg, sel.fg) },
      isReady: sel.ready, isWaiting: !sel.ready,
      cancelled: flow === 'cancelled', asking: flow === 'asking', notAsking: flow !== 'asking',
      ask: function () { self.setState({ flow: 'asking' }); },
      keep: function () { self.setState({ flow: 'list' }); },
      cancel: function () { self.setState({ flow: 'cancelled', sel: 'kieu' }); }
    });
  }`);

// ---------- Lịch sử đọc ----------
out['WideHistory.dc.html'] = S.head('Lịch sử đọc ở cửa sổ rộng', W, H) +
  S.nav('WideHistory.dc.html', { preview: ['tabs', 'Xem trước: đăng nhập bằng'] }) + `
  <sc-if value="{{isStepUp}}" hint-placeholder-val="{{ true }}">
    <div class="st-fade st-full" style="position: absolute; ${S.fullPos()}; top: 16px; bottom: 16px; display: flex; align-items: center; justify-content: center">
      <section aria-label="Cần mật khẩu" class="st-scroll" style="width: 420px; max-width: 100%; max-height: 100%; overflow-y: auto; box-sizing: border-box; padding: 28px 24px; border-radius: 32px; background:  rgba(var(--glass),0.72); border: 1px solid rgba(var(--edge),0.9); display: flex; flex-direction: column; gap: 14px">
        <h1 style="margin: 0; font-size: 24px; line-height: 1.2; font-weight: 700">Cần mật khẩu để xem</h1>
        <p style="margin: 0; font-size: 14px; line-height: 1.5; color: var(--ink2b)">Bạn đang đăng nhập bằng thẻ. Lịch sử đọc là dữ liệu cá nhân, nên cần thêm mật khẩu.</p>
        <label style="display: flex; flex-direction: column; gap: 6px; font-size: 13px; font-weight: 600; color: var(--ink2)">
          Mật khẩu
          <input type="password" autocomplete="current-password" style="height: 50px; padding: 0 16px; border-radius: 16px; border: 1px solid rgba(var(--ink-rgb),0.5); background: var(--surface); font: inherit; font-size: 15px; font-weight: 400">
        </label>
        <button type="button" onClick="{{confirm}}" class="st-press" style="${S.btnPrimary}">Xác nhận</button>
        <a href="WideSignIn.dc.html" style="align-self: center; font-size: 14px; font-weight: 600; padding: 12px">Đăng nhập bằng mật khẩu</a>
      </section>
    </div>
  </sc-if>
  <sc-if value="{{isList}}" hint-placeholder-val="{{ false }}">` +
`
    <section aria-label="Sách đã đọc" class="st-scroll" style="position: absolute; ${S.fullPos()}; max-width: ${S.isMid() ? 600 : 720}px; top: 0; bottom: 0; padding: 0 0 ${S.BOT + 16}px; box-sizing: border-box; overflow-y: auto; display: flex; flex-direction: column; gap: 12px">
      <h1 style="margin: 0; padding: ${S.isMid() ? 36 : 40}px 0 8px; font-size: 22px; font-weight: 600; color: var(--ink2b)">Lịch sử đọc</h1>
      <ul style="list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px">
        <sc-for list="{{hrows}}" as="h" hint-placeholder-count="3">
          <li class="st-row" style="display: flex; gap: 14px; align-items: center; padding: 12px; border-radius: 24px; background:  rgba(var(--glass),0.8)">
            <div aria-hidden="true" style="{{h.cover}}">{{h.title}}</div>
            <div style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px">
              <div style="font-size: 16px; font-weight: 600; line-height: 1.25">{{h.title}}</div>
              <div style="font-size: 13px; color: var(--ink2b)">{{h.line}}</div>
            </div>
          </li>
        </sc-for>
      </ul>
      <a href="WideAccount.dc.html" style="display: flex; align-items: center; min-height: 56px; padding: 0 18px; border-radius: 24px; background: var(--surface); text-decoration: none; font-size: 15px; font-weight: 500">
        <span style="flex: 1">Bật hoặc tắt lưu lịch sử trong Tài khoản</span>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="color: var(--ink)"><path d="m9 18 6-6-6-6"></path></svg>
      </a>
    </section>` + `
  </sc-if>` +
  S.foot(W, H, `
  renderVals() {
    var self = this, st = this.state || {}, ui = this.__ui();
    var v = st.v || 'pw';
    var items = [
      { t: 'Lão Hạc', a: 'Nam Cao', d: 'Trả 02/11/2026', line: 'Mượn 20/10/2026 · trả 02/11/2026', n: 1, bg: '#d8c9a8', fg: '#3e3a36' },
      { t: 'Tắt đèn', a: 'Ngô Tất Tố', d: 'Trả 15/10/2026', line: 'Mượn 01/10/2026 · trả 15/10/2026', n: 0, bg: '#3e3a36', fg: '#ffffff' },
      { t: 'Dế Mèn phiêu lưu ký', a: 'Tô Hoài', d: 'Trả 20/09/2026', line: 'Mượn 20/11/2026 · chưa trả', n: 3, bg: '#c9a66b', fg: '#1c1b1a' }
    ];
    var sel = items[st.sel || 0];
    return Object.assign(this.__theme(), {
      tabs: ui.tabs([['card', 'Thẻ: cần mật khẩu'], ['pw', 'Mật khẩu']], v, 'v'),
      isStepUp: v === 'card', isList: v !== 'card',
      hrows: items.map(function (x) {
        return { title: x.t, line: x.line, cover: 'width:56px;height:80px;flex:none;border-radius:12px;padding:8px;box-sizing:border-box;overflow:hidden;font-size:11px;font-weight:600;line-height:1.2;background:' + x.bg + ';color:' + x.fg };
      }),
      confirm: function () { self.setState({ v: 'pw' }); },
      rows: items.map(function (x, i) {
        return ui.row({ i: i, title: x.t, author: x.a, bg: x.bg, fg: x.fg, on: x === sel,
          status: x.d, pill: ui.pill('wait'), pick: function () { self.setState({ sel: i }); } });
      }),
      sel: { title: sel.t, author: sel.a, date: sel.d.replace('Trả ', ''), coverStyle: ui.bigCover(sel.bg, sel.fg),
        copies: sel.n ? 'Còn ' + sel.n + ' bản' : 'Đang được mượn',
        copiesStyle: 'font-size:15px;font-weight:700;line-height:1.25;color:' + (sel.n ? 'var(--ok)' : 'var(--danger2)') }
    });
  }`);

// ---------- Tìm khi không có mạng ----------
out['WideOfflineSearch.dc.html'] = S.head('Tìm khi không có mạng ở cửa sổ rộng', W, H) + S.nav('WideCatalog.dc.html') +
  S.listPane('Danh sách sách trong bản lưu', 'Tất cả sách', `
    <div role="status" style="padding: 10px 14px; border-radius: 18px; background: var(--warn-bg); font-size: 13px; line-height: 1.45; color: var(--warn)"><strong>Không có mạng.</strong> Đang tìm trong bản lưu trên máy, cập nhật lúc 03:40 hôm nay.</div>` + S.searchAndChips, 'rows', `
      <sc-if value="{{empty}}" hint-placeholder-val="{{ false }}">
        <li style="padding: 24px 8px; font-size: 14px; color: var(--ink3)">Không tìm thấy sách. Thử tên tác giả, ISBN hoặc chọn "Tất cả".</li>
      </sc-if>`) +
  S.detailPane('Chi tiết sách (bản lưu)', `
      <sc-if value="{{hasSel}}" hint-placeholder-val="{{ true }}">` + S.bookHead(true) +
  S.facts([S.fact('Kệ', '{{sel.shelf}}'), S.fact('Số bản còn', 'Cần mạng', 'font-size: 15px; font-weight: 700; line-height: 1.25; color: var(--ink3)')]) + `
        <p style="margin: 0; font-size: 14px; line-height: 1.55; color: var(--ink2b)">ISBN {{sel.isbn}}. Số bản còn sẽ cập nhật khi có mạng.</p>
      </sc-if>`, `
      <button type="button" disabled style="flex: 1; min-height: 52px; padding: 6px 16px; border-radius: 999px; border: 0; background:  rgba(var(--ink-rgb),0.12); color: var(--ink2b); font: inherit; font-size: 15px; font-weight: 600; line-height: 1.2">Đặt trước cần mạng</button>`) +
  S.foot(W, H, `
  renderVals() {
    var self = this, st = this.state || {}, ui = this.__ui();
    ${S.SEARCH_JS}
    var sel = hits.filter(function (b) { return b.isbn === (st.sel || '9780990000068'); })[0] || hits[0] || null;
    return Object.assign(this.__theme(), search, {
      rows: hits.map(function (b, i) {
        return ui.row({ i: i, title: b.t, author: b.a, bg: b.bg, fg: b.fg, on: sel && b.isbn === sel.isbn,
          status: 'Kệ ' + b.shelf, pill: ui.pill('wait'), note: b.s.split(' · ')[0],
          pick: function () { self.setState({ sel: b.isbn }); } });
      }),
      hasSel: !!sel,
      sel: sel ? { title: sel.t, author: sel.a, shelf: sel.shelf, isbn: sel.isbn, coverStyle: ui.bigCover(sel.bg, sel.fg),
        tags: sel.s.split(' · ').map(function (x) { return { label: x }; }) } : { tags: [] }
    });
  }`);


// the medium window: one pane, the same cards as the phone
const RECO_MID = `
  <section aria-label="Gợi ý cho bạn" class="st-scroll" style="position: absolute; ${S.fullPos()}; max-width: 600px; top: 0; bottom: 0; padding: 0 0 ${S.BOT + 16}px; box-sizing: border-box; overflow-y: auto; display: flex; flex-direction: column; gap: 12px">
    <h1 style="margin: 0; padding: 36px 0 0; font-size: 22px; font-weight: 600; color: var(--ink2b)">Gợi ý cho bạn</h1>
    <p style="margin: 0; padding: 0 4px; font-size: 14px; line-height: 1.45; color: var(--ink2)">{{subtitle}}</p>
    <sc-if value="{{isCard}}" hint-placeholder-val="{{ false }}">
      <a href="MidHistory.dc.html" style="display: flex; align-items: center; min-height: 52px; padding: 0 18px; border-radius: 20px; background: var(--surface); text-decoration: none; font-size: 14px; font-weight: 600">Nhập mật khẩu để có gợi ý chính xác hơn</a>
    </sc-if>
    <sc-if value="{{hasRecs}}" hint-placeholder-val="{{ true }}">
      <ul style="list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 12px">
        <sc-for list="{{crows}}" as="b" hint-placeholder-count="3">
          <li class="st-row" style="border-radius: 28px; background:  rgba(var(--glass),0.8); display: flex; flex-direction: column">
            <a href="MidCatalog.dc.html" style="display: flex; gap: 14px; padding: 12px 12px 0; text-decoration: none; color: var(--ink); min-height: 48px">
              <div aria-hidden="true" style="{{b.cover}}">{{b.title}}</div>
              <div style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px">
                <div style="font-size: 15px; font-weight: 600; line-height: 1.25">{{b.title}}</div>
                <div style="font-size: 13px; color: var(--ink2)">{{b.author}}</div>
                <div style="font-size: 13px; font-weight: 700; line-height: 1.3; color: var(--warn)">{{b.reason}}</div>
                <div style="{{b.statusStyle}}">{{b.status}}</div>
              </div>
            </a>
            <button type="button" onClick="{{b.hide}}" class="st-press" aria-label="Không quan tâm: {{b.title}}" style="align-self: flex-start; min-height: 48px; margin: 0 0 4px 4px; padding: 0 16px; border-radius: 999px; border: 0; background: transparent; color: var(--ink2); font: inherit; font-size: 14px; cursor: pointer">Không quan tâm</button>
          </li>
        </sc-for>
      </ul>
      <p style="margin: 0; padding: 0 4px; font-size: 12px; line-height: 1.5; color: var(--ink3)">Gợi ý được tính ngay trên máy, theo tác giả và chủ đề của sách bạn mượn. Lịch sử không được gửi đi đâu.</p>
    </sc-if>
    <sc-if value="{{noRecs}}" hint-placeholder-val="{{ false }}">
      <div style="display: flex; flex-direction: column; gap: 14px; padding-top: 8px">
        <div style="font-size: 18px; font-weight: 600">Chưa có gì để gợi ý</div>
        <p style="margin: 0; font-size: 14px; line-height: 1.5; color: var(--ink2)">ShelfTap chưa có dữ liệu để gợi ý: bạn chưa mượn cuốn nào, hoặc đã tắt lưu lịch sử đọc và hiện không mượn cuốn nào. Bạn vẫn xem sách theo chủ đề được.</p>
        <a href="MidCatalog.dc.html" class="st-press" style="${S.btnPrimary}">Xem theo chủ đề</a>
        <a href="MidAccount.dc.html" class="st-press" style="${S.btnGlass}">Cài đặt lịch sử đọc</a>
      </div>
    </sc-if>
  </section>`;

// ---------- Gợi ý cho bạn ----------
out['WideRecommend.dc.html'] = S.head('Gợi ý cho bạn ở cửa sổ rộng', W, H) +
  S.nav('WideRecommend.dc.html', { preview: ['tabs', 'Xem trước: đăng nhập bằng'] }) +
  (S.isMid() ? RECO_MID : S.listPane('Sách gợi ý', 'Gợi ý cho bạn', `
    <div style="font-size: 14px; color: var(--ink2b); padding: 0 4px">{{subtitle}}</div>
    <sc-if value="{{isCard}}" hint-placeholder-val="{{ false }}">
      <a href="WideHistory.dc.html" style="padding: 10px 14px; border-radius: 18px; background: var(--surface); font-size: 13px; font-weight: 600; text-decoration: none">Nhập mật khẩu để có gợi ý chính xác hơn</a>
    </sc-if>`, 'rows', `
      <sc-if value="{{noRecs}}" hint-placeholder-val="{{ false }}">
        <li style="padding: 16px 8px; display: flex; flex-direction: column; gap: 8px">
          <div style="font-size: 16px; font-weight: 700">Chưa có gì để gợi ý</div>
          <div style="font-size: 14px; line-height: 1.5; color: var(--ink2b)">ShelfTap chưa có dữ liệu để gợi ý: bạn chưa mượn cuốn nào, hoặc đã tắt lưu lịch sử đọc và hiện không mượn cuốn nào. Bạn vẫn xem sách theo chủ đề được.</div>
        </li>
      </sc-if>
      <li style="padding: 4px; font-size: 12px; line-height: 1.5; color: var(--ink3)">Gợi ý được tính ngay trên máy, theo tác giả và chủ đề của sách bạn mượn. Lịch sử không được gửi đi đâu.</li>`) +
  S.detailPane('Vì sao gợi ý', `
      <sc-if value="{{hasRecs}}" hint-placeholder-val="{{ true }}">` + S.bookHead(false) + `
        <div style="padding: 14px 16px; border-radius: 20px; background: var(--surface); display: flex; flex-direction: column; gap: 6px">
          <div style="font-size: 15px; font-weight: 700; line-height: 1.35; color: var(--warn2)">{{sel.reason}}</div>
          <div style="{{sel.statusStyle}}">{{sel.status}}</div>
        </div>
        <div style="display: flex; gap: 8px; flex-wrap: wrap">
          <a href="WideCatalog.dc.html" class="st-press" style="${S.btnPrimary}; flex: 1">Xem chi tiết sách</a>
          <button type="button" onClick="{{hide}}" class="st-press" style="${S.btnGlass}; flex: 1">Không quan tâm</button>
        </div>
      </sc-if>
      <sc-if value="{{noRecs}}" hint-placeholder-val="{{ false }}">
        <div style="display: flex; flex-direction: column; gap: 8px">
          <a href="WideCatalog.dc.html" class="st-press" style="${S.btnPrimary}">Xem theo chủ đề</a>
          <a href="WideAccount.dc.html" class="st-press" style="${S.btnGlass}">Cài đặt lịch sử đọc</a>
        </div>
      </sc-if>`)) +
  S.foot(W, H, `
  renderVals() {
    var self = this, st = this.state || {}, ui = this.__ui();
    var v = st.v || 'pw', hidden = st.hidden || {};
    var b = function (t, a, bg, fg, n, why) { return { t: t, a: a, bg: bg, fg: fg, n: n, why: why }; };
    var sets = {
      pw: [b('Chí Phèo', 'Nam Cao', '#3e3a36', '#ffffff', 1, 'Vì bạn đã mượn sách của Nam Cao'),
           b('The Pragmatic Programmer', 'Andrew Hunt, David Thomas', '#c9a66b', '#1c1b1a', 1, 'Cùng chủ đề Software engineering'),
           b('Tắt đèn', 'Ngô Tất Tố', '#7a2e1e', '#ffffff', 0, 'Cùng chủ đề Tiểu thuyết'),
           b('Truyện Kiều', 'Nguyễn Du', '#e7dcc6', '#2f4a44', 2, 'Cùng chủ đề Văn học Việt Nam')],
      card: [b('Tắt đèn', 'Ngô Tất Tố', '#7a2e1e', '#ffffff', 0, 'Cùng chủ đề Tiểu thuyết với sách bạn đang mượn'),
             b('Truyện Kiều', 'Nguyễn Du', '#e7dcc6', '#2f4a44', 2, 'Cùng chủ đề Văn học Việt Nam')],
      off: [b('Tắt đèn', 'Ngô Tất Tố', '#7a2e1e', '#ffffff', 0, 'Cùng chủ đề Tiểu thuyết'),
            b('Truyện Kiều', 'Nguyễn Du', '#e7dcc6', '#2f4a44', 2, 'Cùng chủ đề Văn học Việt Nam')],
      none: []
    };
    var list = sets[v].filter(function (x) { return !hidden[v + x.t]; });
    var sel = list.filter(function (x) { return x.t === st.sel; })[0] || list[0] || null;
    var out = sel && sel.n === 0;
    return Object.assign(this.__theme(), {
      tabs: ui.tabs([['pw', 'Mật khẩu'], ['card', 'Thẻ'], ['off', 'Tắt lịch sử'], ['none', 'Chưa có gì']], v, 'v'),
      subtitle: v === 'pw' ? 'Theo sách bạn đã mượn và đang mượn.' : v === 'card' ? 'Đăng nhập bằng thẻ: chỉ theo sách bạn đang mượn.' : 'Lịch sử đọc đang tắt: chỉ theo sách bạn đang mượn.',
      isCard: v === 'card', hasRecs: !!sel, noRecs: !sel,
      crows: list.map(function (x) {
        var out = x.n === 0;
        return { title: x.t, author: x.a, reason: x.why, status: out ? 'Đang được mượn · đặt trước' : 'Còn ' + x.n + ' bản',
          statusStyle: 'font-size:13px;color:' + (out ? 'var(--danger)' : 'var(--ok)'),
          cover: 'width:48px;height:72px;flex:none;border-radius:8px;padding:6px;box-sizing:border-box;overflow:hidden;font-size:10px;font-weight:600;line-height:1.2;background:' + x.bg + ';color:' + x.fg,
          hide: function () { var h = Object.assign({}, hidden); h[v + x.t] = true; self.setState({ hidden: h, sel: null }); } };
      }),
      rows: list.map(function (x, i) {
        return ui.row({ i: i, title: x.t, author: '', bg: x.bg, fg: x.fg, on: x === sel, reason: x.why,
          pick: function () { self.setState({ sel: x.t }); } });
      }),
      sel: sel ? { title: sel.t, author: sel.a, reason: sel.why, coverStyle: ui.bigCover(sel.bg, sel.fg),
        status: sel.n ? 'Còn ' + sel.n + ' bản' : 'Đang được mượn · đặt trước',
        statusStyle: 'font-size:14px;font-weight:600;color:' + (out ? 'var(--danger2)' : 'var(--ok)') } : {},
      hide: function () { if (!sel) return; var h = Object.assign({}, hidden); h[v + sel.t] = true; self.setState({ hidden: h, sel: null }); }
    });
  }`);

// ---------- Danh mục: the home (RailCatalog / WideCatalog) and the list beside the detail (MidCatalog / WideList) ----------
out[S.isMid() ? 'RailCatalog.dc.html' : 'WideCatalog.dc.html'] = S.head('Danh mục ở cửa sổ ' + (S.isMid() ? 'vừa' : 'rộng'), W, H) +
  S.nav('WideCatalog.dc.html') + S.home(false, S.isMid() ? 'MidCatalog.dc.html' : 'WideList.dc.html') + S.foot(W, H, S.HOME_JS);
out[S.isMid() ? 'WideCatalog.dc.html' : 'WideList.dc.html'] = S.head('Danh sách và chi tiết sách ở cửa sổ ' + (S.isMid() ? 'vừa' : 'rộng'), W, H) + S.nav('WideCatalog.dc.html', { dock: false }) +
  S.listPane('Danh sách sách', 'Danh mục', S.searchAndChips, 'rows', `
      <sc-if value="{{empty}}" hint-placeholder-val="{{ false }}">
        <li style="padding: 24px 8px; font-size: 14px; color: var(--ink3)">Không có sách khớp. Thử bỏ dấu hoặc chọn "Tất cả".</li>
      </sc-if>`) +
  S.detailPane('Chi tiết sách', `
      <sc-if value="{{hasSel}}" hint-placeholder-val="{{ true }}">` + S.bookHead(true) +
  S.facts([S.fact('Tình trạng', '{{sel.status}}', '{{sel.statusStyle}}')]) + `
        <p style="margin: 0; font-size: 14px; line-height: 1.55; color: var(--ink2b)">[Mô tả sách: chưa có trong dữ liệu danh mục] · ISBN {{sel.isbn}}</p>
      </sc-if>`) + S.dockMorph() +
  S.foot(W, H, `
  renderVals() {
    var self = this, st = this.state || {}, ui = this.__ui();
    var phase = st.phase || 'idle';
    ${S.SEARCH_JS}
    var sel = hits.filter(function (b) { return b.isbn === (st.sel || '9780990000020'); })[0] || hits[0] || null;
    var out = sel && sel.n === 0;
    var rb = ui.phaseBtn(phase, { idle: 'Đặt trước sách này', sending: 'Đang gửi yêu cầu…', done: 'Đã đặt trước · thư viện xác nhận' });
    ${S.DOCK_JS()}
    return Object.assign(this.__theme(), search, dock, {
      rows: hits.map(function (b, i) {
        return ui.row({ i: i, title: b.t, author: b.a, bg: b.bg, fg: b.fg, on: sel && b.isbn === sel.isbn,
          status: b.n ? 'Còn ' + b.n + ' bản' : 'Đang được mượn · đặt trước', pill: ui.pill(b.n ? 'ok' : 'bad'),
          pick: function () { self.setState({ sel: b.isbn, phase: 'idle' }); } });
      }),
      hasSel: !!sel,
      sel: sel ? { title: sel.t, author: sel.a, shelf: sel.shelf, isbn: sel.isbn, coverStyle: ui.bigCover(sel.bg, sel.fg),
        tags: sel.s.split(' · ').map(function (x) { return { label: x }; }),
        status: out ? 'Đang được mượn' : 'Còn ' + sel.n + ' bản',
        statusStyle: 'font-size:15px;font-weight:700;line-height:1.25;color:' + (out ? 'var(--danger2)' : 'var(--ok)') } : { tags: [] },
      reserveBtn: rb,
      reserve: function () {
        if (phase === 'sending') return;
        if (phase === 'done') { self.setState({ phase: 'idle' }); return; }
        self.setState({ phase: 'sending' });
        self.timer = setTimeout(function () { self.setState({ phase: 'done' }); }, 1200);
      }
    });
  }`);

return out;
};
