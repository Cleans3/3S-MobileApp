// Wide boards: account settings, sign-in, system states, self-checkout, guest.
const S = require('./shell.js');
module.exports = function () {
const W = S.W(), H = S.H();
const out = {};

const sw = (label, desc, on, flip, track, knob) => `
          <div style="display: flex; align-items: center; gap: 12px; min-height: 64px; border-bottom: 1px solid rgba(var(--ink-rgb),0.08)">
            <div style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px">
              <div style="font-size: 15px; font-weight: 600">${label}</div>${desc ? `
              <div style="font-size: 12px; line-height: 1.45; color: var(--ink2b)">${desc}</div>` : ''}
            </div>
            <button type="button" role="switch" aria-checked="{{${on}}}" aria-label="${label}" onClick="{{${flip}}}" class="st-swap" style="{{${track}}}"><span class="st-knob" style="{{${knob}}}"></span></button>
          </div>`;
const card = inner => `
        <div style="border-radius: 24px; background:  rgba(var(--glass),0.8); padding: 6px 16px; display: flex; flex-direction: column">${inner}
        </div>`;

// ---------- Tài khoản (gộp Thông báo) ----------
out['WideAccount.dc.html'] = S.head('Tài khoản ở cửa sổ rộng', W, H) + S.nav('WideAccount.dc.html') + `
  <sc-if value="{{showList}}" hint-placeholder-val="{{ true }}">
  <section aria-label="Mục cài đặt" class="st-list st-fade" style="animation: {{listAnim}}; position: absolute; ${S.listPos(320)}; top: ${S.isMid() ? 40 : 16}px; bottom: 16px; display: flex; flex-direction: column; gap: 12px">
    <h1 style="margin: 12px 4px 0; font-size: 28px; line-height: 1.15; font-weight: 700; letter-spacing: -0.3px">Tài khoản</h1>
    <div style="padding: 16px; border-radius: 24px; background:  #2f4a44; color: #ffffff; display: flex; align-items: center; gap: 12px; flex: none">
      <span style="width: 48px; height: 48px; flex: none; border-radius: 999px; background:  rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="8" r="4"></circle><path d="M4 21a8 8 0 0 1 16 0"></path></svg>
      </span>
      <div style="min-width: 0; display: flex; flex-direction: column; gap: 2px">
        <div style="font-size: 17px; font-weight: 700">[Tên thành viên]</div>
        <div style="font-size: 13px; color: rgba(255,255,255,0.9); overflow: hidden; text-overflow: ellipsis">member@shelftap.test</div>
      </div>
    </div>
    <div class="st-scroll" style="flex: 1; min-height: 0; overflow-y: auto; display: flex; flex-direction: column; gap: 6px">
      <sc-for list="{{cats}}" as="c" hint-placeholder-count="5">
        <button type="button" class="st-pick" aria-current="{{c.current}}" onClick="{{c.pick}}" style="{{c.style}}">
          <span style="font-size: 15px; font-weight: 700">{{c.label}}</span>
          <span style="font-size: 12px; line-height: 1.4; color: var(--ink2b)">{{c.desc}}</span>
        </button>
      </sc-for>
    </div>
  </section>
  </sc-if>
  <sc-if value="{{showDetail}}" hint-placeholder-val="{{ true }}">
  <section aria-label="{{catLabel}}" class="st-pane" style="position: absolute; ${S.panePos()}; top: 16px; bottom: 16px; border-radius: 30px; overflow: hidden; background:  rgba(var(--glass),0.55); border: 1px solid rgba(var(--edge),0.85)">
    <div class="st-scroll st-detail" style="animation: {{detailAnim}}; position: absolute; inset: 0; padding: 12px 20px 24px; box-sizing: border-box; overflow-y: auto; display: flex; flex-direction: column; gap: 14px">
      <div style="display: flex; align-items: center; gap: 8px; min-height: 48px">${S.backBtn()}
        <h2 style="flex: 1; margin: 0; font-size: 20px; font-weight: 700">{{catLabel}}</h2>
        <div style="width: 84px; flex: none"></div>
      </div>
      <sc-if value="{{isPrivacy}}" hint-placeholder-val="{{ true }}">` +
  card(sw('Lưu lịch sử đọc', 'Tắt thì sách bạn trả sẽ không còn hiện ở Lịch sử đọc.', 'history', 'toggleHistory', 'historyTrack', 'historyKnob') +
       sw('Gợi ý theo lịch sử đọc', 'Tính ngay trên máy, không gửi đi đâu. Tắt thì Gợi ý chỉ theo sách đang mượn.', 'recs', 'toggleRecs', 'recsTrack', 'recsKnob')) +
  S.note('Sách bạn mượn là dữ liệu cá nhân nhạy cảm (Nghị định 13/2023). Sách đã trả quá 1 năm không còn gắn với tên bạn.') + `
      </sc-if>
      <sc-if value="{{isNotify}}" hint-placeholder-val="{{ false }}">
        <sc-if value="{{denied}}" hint-placeholder-val="{{ true }}">
          <div style="padding: 14px 16px; border-radius: 20px; background: var(--warn-bg); display: flex; align-items: center; gap: 12px">
            <div style="flex: 1; font-size: 13px; line-height: 1.45; color: var(--warn)">Thông báo của ShelfTap đang bị tắt trong cài đặt của máy.</div>
            <button type="button" onClick="{{allow}}" style="min-height: 40px; padding: 6px 16px; border-radius: 999px; border: 0; background: var(--primary); color: var(--on-primary); font: inherit; font-size: 14px; font-weight: 600; cursor: pointer">Bật lại</button>
          </div>
        </sc-if>` +
  card(sw('Nhắc trước hạn trả', 'Một ngày trước hạn, lúc 7:30 sáng.', 'due', 't_due', 'dueTrack', 'dueKnob') +
       sw('Sách đặt trước đã sẵn sàng', 'Báo khi thư viện giữ sách cho bạn.', 'ready', 't_ready', 'readyTrack', 'readyKnob') +
       sw('Ẩn tên sách trên màn khóa', 'Chỉ hiện tên sách sau khi mở khóa máy.', 'hide', 't_hide', 'hideTrack', 'hideKnob')) +
  S.note('Nhắc hạn trả được tính ngay trên máy từ hạn mượn, không cần server gửi. Báo sách đặt trước sẵn sàng thì cần server, API hiện chưa có.') + `
      </sc-if>
      <sc-if value="{{isSecurity}}" hint-placeholder-val="{{ false }}">` +
  card(sw('Khóa bằng vân tay', 'Nếu vân tay trên máy thay đổi, ShelfTap yêu cầu đăng nhập lại bằng mật khẩu.', 'bio', 'toggleBio', 'bioTrack', 'bioKnob') + `
          <a href="WideSignIn.dc.html" style="display: flex; align-items: center; min-height: 56px; text-decoration: none; font-size: 15px; font-weight: 600; color: var(--ink); border-bottom: 1px solid rgba(var(--ink-rgb),0.08)">
            <span style="flex: 1">Đổi mật khẩu</span>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="color: var(--ink)"><path d="m9 18 6-6-6-6"></path></svg>
          </a>
          <div style="display: flex; align-items: center; min-height: 56px; font-size: 14px; color: var(--ink2)">Tự đăng xuất sau 5 phút không dùng</div>`) + `
      </sc-if>
      <sc-if value="{{isLook}}" hint-placeholder-val="{{ false }}">
        <div role="group" aria-label="Giao diện" style="padding: 4px; border-radius: 999px; background:  rgba(var(--glass),0.8); display: flex; gap: 4px">
          <sc-for list="{{themeOpts}}" as="o" hint-placeholder-count="3">
            <button type="button" onClick="{{o.pick}}" aria-pressed="{{o.on}}" class="st-swap st-press" style="{{o.style}}">{{o.label}}</button>
          </sc-for>
        </div>` + S.note('"Theo hệ thống" đổi sáng hoặc tối cùng lúc với điện thoại.') + `
      </sc-if>
      <sc-if value="{{isLogout}}" hint-placeholder-val="{{ false }}">
        <div style="padding: 18px; border-radius: 24px; background: var(--dialog); border: 1px solid rgba(var(--ink-rgb),0.5); display: flex; flex-direction: column; gap: 12px">
          <div style="font-size: 18px; font-weight: 700">Đăng xuất khỏi ShelfTap?</div>
          <p style="margin: 0; font-size: 14px; line-height: 1.5; color: var(--ink2)">Phiên đăng nhập trên máy này sẽ bị xóa. Lần sau bạn cần đăng nhập lại bằng mật khẩu hoặc thẻ.</p>
          <a href="WideSignIn.dc.html" style="min-height: 52px; border-radius: 999px; background:  #7a3420; color: #ffffff; display: flex; align-items: center; justify-content: center; text-decoration: none; font-size: 15px; font-weight: 600">Đăng xuất</a>
          <button type="button" onClick="{{stay}}" style="${S.btnGlass}">Ở lại</button>
        </div>
      </sc-if>
    </div>
  </section>
  </sc-if>` +
  S.foot(W, H, `
  renderVals() {
    var self = this, st = this.state || {}, ui = this.__ui();
    var cat = st.cat || 'privacy';
    var val = function (k, def) { return st[k] === undefined ? def : st[k]; };
    var flip = function (k, def) { return function () { var o = {}; o[k] = !val(k, def); self.setState(o); }; };
    var cats = [
      ['privacy', 'Quyền riêng tư', 'Lịch sử đọc, gợi ý'],
      ['notify', 'Thông báo', 'Nhắc hạn trả, sách đặt trước, màn khóa'],
      ['security', 'Bảo mật', 'Vân tay, mật khẩu, tự đăng xuất'],
      ['look', 'Giao diện', 'Sáng, tối hoặc theo hệ thống'],
      ['logout', 'Đăng xuất', 'Xóa phiên trên máy này']
    ];
    var v = {};
    ['history', 'recs', 'due', 'ready', 'hide'].forEach(function (k) { v[k] = val(k, true); });
    v.bio = val('bio', false);
    var label = cats.filter(function (c) { return c[0] === cat; })[0][1];
    return Object.assign(this.__theme(), {
      cats: cats.map(function (c) {
        var on = c[0] === cat, bad = c[0] === 'logout';
        return { label: c[1], desc: c[2], current: on ? 'true' : 'false', pick: function () { self.setState({ cat: c[0] }); self.__layout().toDetail(); },
          style: 'width:100%;display:flex;flex-direction:column;align-items:flex-start;gap:2px;padding:12px 14px;border-radius:20px;text-align:left;font:inherit;cursor:pointer;min-height:56px;color:' + (bad ? 'var(--danger)' : 'var(--ink)') + ';border:1px solid ' + (on ? 'rgba(var(--ink-rgb),0.5)' : 'transparent') + ';background:' + (on ? 'var(--surface)' : 'rgba(var(--glass),0.72)') };
      }),
      catLabel: label,
      isPrivacy: cat === 'privacy', isNotify: cat === 'notify', isSecurity: cat === 'security', isLook: cat === 'look', isLogout: cat === 'logout',
      history: v.history, historyTrack: ui.track(v.history), historyKnob: ui.knob(v.history), toggleHistory: flip('history', true),
      recs: v.recs, recsTrack: ui.track(v.recs), recsKnob: ui.knob(v.recs), toggleRecs: flip('recs', true),
      due: v.due, dueTrack: ui.track(v.due), dueKnob: ui.knob(v.due), t_due: flip('due', true),
      ready: v.ready, readyTrack: ui.track(v.ready), readyKnob: ui.knob(v.ready), t_ready: flip('ready', true),
      hide: v.hide, hideTrack: ui.track(v.hide), hideKnob: ui.knob(v.hide), t_hide: flip('hide', true),
      bio: v.bio, bioTrack: ui.track(v.bio), bioKnob: ui.knob(v.bio), toggleBio: flip('bio', false),
      denied: val('denied', true), allow: function () { self.setState({ denied: false }); },
      stay: function () { self.setState({ cat: 'privacy' }); }
    });
  }`);

// ---------- Đăng nhập (gộp Màn chào, Đăng nhập bằng thẻ, Đổi mật khẩu) ----------
const input = (label, type, extra) => `
          <label style="display: flex; flex-direction: column; gap: 6px; font-size: 13px; font-weight: 600; color: var(--ink2)">
            ${label}
            <input type="${type}" ${extra || ''} style="height: 50px; padding: 0 16px; border-radius: 16px; border: 1px solid rgba(var(--ink-rgb),0.5); background: var(--surface); font: inherit; font-size: 15px; font-weight: 400">
          </label>`;
out['WideSignIn.dc.html'] = S.head('Chào và đăng nhập ở cửa sổ rộng', W, H) + `
  <div aria-hidden="true" style="position: absolute; left: 0; top: 0; bottom: 0; width: ${S.isMid() ? 240 : 440}px; overflow: hidden; background:  #3a2a1e">
    <div style="position: absolute; inset: 0; padding: 40px 28px; display: flex; flex-direction: column; gap: 22px">
      <sc-for list="{{shelves}}" as="s" hint-placeholder-count="5">
        <div style="display: flex; align-items: flex-end; gap: 3px; padding-bottom: 6px; border-bottom: 8px solid #6b4a30">
          <sc-for list="{{s.books}}" as="b" hint-placeholder-count="12"><div style="{{b.style}}"></div></sc-for>
        </div>
      </sc-for>
    </div>
    <div style="position: absolute; inset: 0; background:  linear-gradient(to right, rgba(20,14,10,0.1), rgba(20,14,10,0.45))"></div>
  </div>
  <div style="position: absolute; z-index: 40; ${S.isMid() ? 'left: 16px; right: 16px; bottom: 12px' : 'left: 24px; bottom: 24px; width: 392px'}">` + S.preview('tabs', 'Xem trước màn') + `
  </div>
  <section aria-label="{{paneLabel}}" class="st-scroll" style="position: absolute; left: ${S.isMid() ? 240 : 440}px; right: 0; top: 0; bottom: 0; padding: ${S.isMid() ? '72px 40px 150px' : '72px 60px 40px'}; box-sizing: border-box; overflow-y: auto; display: flex; flex-direction: column">
    <div class="st-detail" style="width: 100%; max-width: 400px; margin: auto 0; display: flex; flex-direction: column; gap: 14px">
      <div style="font-size: 15px; font-weight: 700; color: var(--ink2b)">ShelfTap</div>
      <sc-if value="{{isWelcome}}" hint-placeholder-val="{{ true }}">
        <h1 style="margin: 0; font-size: 34px; line-height: 1.12; font-weight: 800; letter-spacing: -0.4px">Mượn sách chỉ bằng một chạm</h1>
        <p style="margin: 0; font-size: 15px; line-height: 1.55; color: var(--ink2b)">Tìm sách trong thư viện, chạm điện thoại vào sách để mượn, gia hạn ngay trên máy.</p>
        <button type="button" onClick="{{toLogin}}" class="st-press" style="${S.btnPrimary}">Bắt đầu</button>
        <a href="WideGuest.dc.html" style="align-self: center; padding: 12px; font-size: 14px; font-weight: 600">Xem sách mà không cần đăng nhập</a>
      </sc-if>
      <sc-if value="{{isLogin}}" hint-placeholder-val="{{ false }}">
        <h1 style="margin: 0; font-size: 30px; line-height: 1.15; font-weight: 700">Đăng nhập</h1>
        <p style="margin: 0; font-size: 14px; line-height: 1.5; color: var(--ink2b)">Dùng tài khoản thư viện đã cấp cho bạn.</p>` +
  input('Email', 'email', 'autocomplete="username" value="member@shelftap.test"') + `
        <label style="display: flex; flex-direction: column; gap: 6px; font-size: 13px; font-weight: 600; color: var(--ink2)">
          Mật khẩu
          <span style="display: flex; align-items: center; height: 50px; padding: 0 4px 0 16px; border-radius: 16px; border: 1px solid rgba(var(--ink-rgb),0.5); background: var(--surface)">
            <input type="{{pwType}}" autocomplete="current-password" style="flex: 1; border: 0; background:  transparent; font: inherit; font-size: 15px; font-weight: 400; outline: none">
            <button type="button" aria-label="{{eyeLabel}}" onClick="{{toggleShow}}" style="width: 44px; height: 44px; border: 0; background:  transparent; display: flex; align-items: center; justify-content: center; cursor: pointer; padding: 0">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="color: var(--ink)"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"></path><circle cx="12" cy="12" r="3"></circle></svg>
            </button>
          </span>
        </label>
        <button type="button" onClick="{{toChange}}" class="st-press" style="${S.btnPrimary}">Đăng nhập</button>
        <div style="text-align: center; font-size: 13px; color: var(--ink3)">hoặc</div>
        <button type="button" onClick="{{toCard}}" class="st-press" style="${S.btnGlass}">Đăng nhập bằng thẻ thư viện</button>` +
  S.note('Chưa có tài khoản? Đăng ký tại quầy thủ thư, mang theo CCCD.', '; text-align: center') + `
      </sc-if>
      <sc-if value="{{isCard}}" hint-placeholder-val="{{ false }}">
        <h1 style="margin: 0; font-size: 30px; line-height: 1.15; font-weight: 700">Đăng nhập bằng thẻ</h1>
        <p style="margin: 0; font-size: 14px; line-height: 1.5; color: var(--ink2b)">Áp thẻ thư viện vào mặt sau máy, chỗ có ăng-ten NFC, và giữ yên vài giây. Trên máy gập và tablet, chỗ này khác điện thoại; app chỉ đúng chỗ nếu máy cho biết vị trí ăng-ten.</p>
        <button type="button" onClick="{{advance}}" aria-label="Mô phỏng chạm thẻ" style="align-self: center; position: relative; width: 200px; height: 200px; border: 0; padding: 0; background:  transparent; cursor: pointer">
          <span aria-hidden="true" class="st-ping" style="{{ping}}"></span>
          <span aria-hidden="true" class="st-swap" style="{{ring}}"></span>
          <span aria-hidden="true" class="st-breathe" style="position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%) rotate(-8deg); width: 132px; height: 84px; border-radius: 12px; background:  #2f4a44; box-shadow: 0 16px 32px rgba(30,45,40,0.35); display: flex; flex-direction: column; justify-content: space-between; padding: 10px 12px; box-sizing: border-box; color: #ffffff; text-align: left">
            <span style="font-size: 12px; font-weight: 700">ShelfTap</span>
            <span style="width: 24px; height: 18px; border-radius: 4px; background:  #c9a66b"></span>
          </span>
        </button>
        <div aria-live="polite" style="padding: 14px 16px; border-radius: 22px; background:  rgba(var(--glass),0.72); border: 1px solid rgba(var(--edge),0.9); display: flex; flex-direction: column; gap: 10px">
          <sc-for list="{{steps}}" as="s" hint-placeholder-count="3">
            <div style="display: flex; align-items: center; gap: 12px">
              <span class="st-swap" style="{{s.dotStyle}}">
                <sc-if value="{{s.done}}" hint-placeholder-val="{{ false }}"><svg class="st-pop" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m5 12 5 5 9-10"></path></svg></sc-if>
              </span>
              <span style="{{s.textStyle}}">{{s.label}}</span>
            </div>
          </sc-for>
        </div>
        <sc-if value="{{cardDone}}" hint-placeholder-val="{{ false }}">
          <a href="WideCatalog.dc.html" class="st-rise" style="${S.btnPrimary}">Vào thư viện</a>
        </sc-if>` +
  S.note('Không cần PIN: thẻ chỉ chứng minh bạn đang giữ thẻ. Muốn xem lịch sử đọc, bạn sẽ cần nhập mật khẩu.') + `
      </sc-if>
      <sc-if value="{{isChange}}" hint-placeholder-val="{{ false }}">
        <span style="align-self: flex-start; padding: 6px 12px; border-radius: 999px; background:  rgba(var(--glass),0.8); font-size: 12px; font-weight: 600; color: var(--ink2)">Lần đăng nhập đầu tiên</span>
        <h1 style="margin: 0; font-size: 30px; line-height: 1.15; font-weight: 700">Đặt mật khẩu của riêng bạn</h1>
        <p style="margin: 0; font-size: 14px; line-height: 1.5; color: var(--ink2b)">Mật khẩu thư viện cấp chỉ dùng một lần. Đổi xong, các thiết bị khác đang đăng nhập sẽ bị đăng xuất.</p>` +
  input('Mật khẩu thư viện cấp', 'password', 'value="mật khẩu tạm" autocomplete="current-password"') +
  input('Mật khẩu mới', 'password', 'value="{{pw}}" onChange="{{onPw}}" autocomplete="new-password" placeholder="Nhập mật khẩu mới"') +
  input('Nhập lại mật khẩu mới', 'password', 'value="{{pw2}}" onChange="{{onPw2}}" autocomplete="new-password" placeholder="Nhập lại"') + `
        <ul aria-live="polite" style="list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px">
          <sc-for list="{{rules}}" as="r" hint-placeholder-count="3">
            <li style="display: flex; align-items: center; gap: 10px; font-size: 13px; color: var(--ink2)">
              <span style="{{r.dot}}"><sc-if value="{{r.ok}}" hint-placeholder-val="{{ false }}"><svg class="st-pop" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m5 12 5 5 9-10"></path></svg></sc-if></span>
              {{r.label}}
            </li>
          </sc-for>
        </ul>
        <sc-if value="{{valid}}" hint-placeholder-val="{{ false }}"><a href="WideCatalog.dc.html" class="st-fade" style="${S.btnPrimary}">Lưu mật khẩu</a></sc-if>
        <sc-if value="{{invalid}}" hint-placeholder-val="{{ true }}"><button type="button" disabled style="min-height: 52px; border-radius: 999px; border: 0; background:  rgba(var(--ink-rgb),0.12); color: var(--ink2b); font: inherit; font-size: 15px; font-weight: 600">Lưu mật khẩu</button></sc-if>
      </sc-if>
    </div>
  </section>` +
  S.foot(W, H, `
  renderVals() {
    var self = this, st = this.state || {}, ui = this.__ui();
    var v = st.v || 'welcome';
    var palette = ['#8e5a3c', '#2f4a44', '#c9a66b', '#6b6f7a', '#b5452b', '#3e3a36', '#d8c9a8', '#556b5a', '#a3633f', '#41506b'];
    var widths = [22, 30, 18, 26, 34, 20, 28, 24, 16, 30, 22, 26];
    var heights = [96, 84, 102, 76, 92, 104, 80, 98, 88, 78, 100, 86];
    var shelves = [];
    for (var s = 0; s < 5; s++) {
      var books = [];
      for (var i = 0; i < 12; i++) books.push({ style: 'width:' + widths[(i + s * 2) % 12] + 'px;height:' + heights[(i * 5 + s) % 12] + 'px;background:' + palette[(i * 3 + s * 5) % 10] + ';border-radius:2px 2px 0 0;box-shadow:inset -3px 0 0 rgba(0,0,0,0.12)' });
      shelves.push({ books: books });
    }
    var show = !!st.show, phase = st.phase || 0, pw = st.pw || '', pw2 = st.pw2 || '';
    var checks = [
      { label: 'Ít nhất 10 ký tự', ok: pw.length >= 10 },
      { label: 'Khác mật khẩu thư viện cấp', ok: pw.length > 0 && pw !== 'mật khẩu tạm' },
      { label: 'Hai lần nhập giống nhau', ok: pw.length > 0 && pw === pw2 }
    ];
    var valid = checks.every(function (c) { return c.ok; });
    var ringBase = 'position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);border-radius:999px;';
    var labels = { welcome: 'Chào mừng', login: 'Đăng nhập', card: 'Đăng nhập bằng thẻ', change: 'Đổi mật khẩu lần đầu' };
    return Object.assign(this.__theme(), {
      shelves: shelves,
      tabs: ui.tabs([['welcome', 'Chào'], ['login', 'Mật khẩu'], ['card', 'Thẻ'], ['change', 'Đổi mật khẩu']], v, 'v', { phase: 0 }),
      paneLabel: labels[v],
      isWelcome: v === 'welcome', isLogin: v === 'login', isCard: v === 'card', isChange: v === 'change',
      toLogin: function () { self.setState({ v: 'login' }); },
      toCard: function () { self.setState({ v: 'card', phase: 0 }); },
      toChange: function () { self.setState({ v: 'change' }); },
      pwType: show ? 'text' : 'password', eyeLabel: show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu',
      toggleShow: function () { self.setState({ show: !show }); },
      steps: ['Đọc thẻ', 'Thư viện xác thực thẻ', 'Đã đăng nhập'].map(function (l, i) {
        var done = phase > i, cur = phase === i;
        return { label: cur && i < 2 ? l + '…' : l, done: done,
          dotStyle: 'width:24px;height:24px;flex:none;border-radius:999px;display:flex;align-items:center;justify-content:center;box-sizing:border-box;background:' + (done ? '#2f5b3a' : cur ? 'var(--surface)' : 'rgba(var(--ink-rgb),0.08)') + ';border:' + (cur ? '2px solid var(--ink)' : '0'),
          textStyle: 'font-size:15px;font-weight:' + (cur ? '700' : '500') + ';color:' + (done || cur ? 'var(--ink)' : 'var(--ink3)') };
      }),
      cardDone: phase >= 3,
      ping: ringBase + 'width:170px;height:170px;border:2px solid rgba(47,74,68,0.45)' + (phase >= 3 ? ';display:none' : ''),
      ring: ringBase + 'width:200px;height:200px;background:rgba(var(--glass),' + (phase > 0 && phase < 3 ? '0.8' : '0.55') + ');border:1px solid rgba(var(--edge),0.95)',
      advance: function () { self.setState({ phase: phase >= 3 ? 0 : phase + 1 }); },
      pw: pw, pw2: pw2,
      onPw: function (e) { self.setState({ pw: e.target.value }); },
      onPw2: function (e) { self.setState({ pw2: e.target.value }); },
      rules: checks.map(function (c) { return { label: c.label, ok: c.ok, dot: 'width:20px;height:20px;flex:none;border-radius:999px;display:flex;align-items:center;justify-content:center;background:' + (c.ok ? '#2f5b3a' : 'rgba(var(--ink-rgb),0.1)') }; }),
      valid: valid, invalid: !valid
    });
  }`);

// ---------- Trạng thái hệ thống (gộp Khởi động, Khóa, Mất kết nối, Đăng xuất kèm lý do) ----------
out['WideStatus.dc.html'] = S.head('Trạng thái hệ thống ở cửa sổ rộng', W, H) + `
  <div aria-hidden="true" style="position: absolute; left: -120px; top: -140px; width: 520px; height: 520px; border-radius: 999px; background:  #d7c39a; opacity: 0.55"></div>
  <div aria-hidden="true" style="position: absolute; right: -140px; bottom: -160px; width: 560px; height: 560px; border-radius: 999px; background:  #b9c7bf; opacity: 0.55"></div>
  <div aria-hidden="true" style="position: absolute; inset: 0; backdrop-filter: blur(50px); -webkit-backdrop-filter: blur(50px)"></div>
  <div class="st-scroll" style="position: absolute; left: 0; right: 0; top: 0; bottom: 0; padding: 64px 0 148px; box-sizing: border-box; overflow-y: auto; display: flex; flex-direction: column; align-items: center">
    <section aria-label="{{paneLabel}}" class="st-detail" style="width: 460px; max-width: calc(100% - 32px); margin: auto 0; box-sizing: border-box; padding: 28px 26px; border-radius: 34px; background:  rgba(var(--glass),0.72); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px); border: 1px solid rgba(var(--edge),0.9); box-shadow: 0 24px 60px rgba(40,28,18,0.12); display: flex; flex-direction: column; gap: 14px">
      <sc-if value="{{isSplash}}" hint-placeholder-val="{{ true }}">
        <div style="display: flex; align-items: center; gap: 14px">
          <span style="width: 56px; height: 56px; flex: none; border-radius: 18px; background: var(--primary); display: flex; align-items: center; justify-content: center">${S.NFC.replace('width="20" height="20"', 'width="26" height="26"')}</span>
          <div style="display: flex; flex-direction: column; gap: 2px">
            <h1 style="margin: 0; font-size: 26px; font-weight: 800">ShelfTap</h1>
            <div style="font-size: 14px; color: var(--ink2b)">Thư viện trong túi bạn</div>
          </div>
        </div>
        <div role="status" style="display: flex; align-items: center; gap: 10px; font-size: 14px; color: var(--ink2)">
          <svg class="st-spin" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" aria-hidden="true" style="color: var(--ink2)"><path d="M21 12a9 9 0 1 1-6.2-8.6"></path></svg>
          Đang kiểm tra phiên đăng nhập…
        </div>
        <div style="font-size: 12px; color: var(--ink3)">[Bản phác] App tự chuyển tới một trong các màn:</div>
        <sc-for list="{{routes}}" as="r" hint-placeholder-count="3">
          <button type="button" onClick="{{r.go}}" style="display: flex; flex-direction: column; align-items: flex-start; gap: 2px; min-height: 52px; padding: 8px 14px; border-radius: 18px; border: 1px solid rgba(var(--ink-rgb),0.5); background: var(--surface); font: inherit; text-align: left; cursor: pointer">
            <span style="font-size: 14px; font-weight: 700">{{r.label}}</span>
            <span style="font-size: 12px; color: var(--ink2b)">{{r.when}}</span>
          </button>
        </sc-for>
      </sc-if>
      <sc-if value="{{isLock}}" hint-placeholder-val="{{ false }}">
        <span style="width: 56px; height: 56px; border-radius: 18px; background: var(--surface); display: flex; align-items: center; justify-content: center">
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="color: var(--ink)"><rect x="5" y="11" width="14" height="10" rx="2"></rect><path d="M8 11V7a4 4 0 0 1 8 0v4"></path></svg>
        </span>
        <h1 style="margin: 0; font-size: 26px; font-weight: 700">{{lockTitle}}</h1>
        <p style="margin: 0; font-size: 14px; line-height: 1.5; color: var(--ink2b)">{{lockBody}}</p>
        <sc-if value="{{locked}}" hint-placeholder-val="{{ true }}"><button type="button" onClick="{{unlock}}" class="st-press" style="${S.btnPrimary}">Mở khóa bằng vân tay</button></sc-if>
        <sc-if value="{{unlocked}}" hint-placeholder-val="{{ false }}"><a href="WideCatalog.dc.html" class="st-rise" style="${S.btnPrimary}">Tiếp tục</a></sc-if>
        <a href="WideSignIn.dc.html" style="${S.btnGlass}">Đăng xuất</a>` +
  S.note('Nếu vân tay trên máy thay đổi, ShelfTap sẽ yêu cầu bạn đăng nhập lại bằng mật khẩu.') + `
      </sc-if>
      <sc-if value="{{isOffline}}" hint-placeholder-val="{{ false }}">
        <h1 style="margin: 0; font-size: 26px; line-height: 1.2; font-weight: 700">Không kết nối được thư viện</h1>
        <p style="margin: 0; font-size: 14px; line-height: 1.5; color: var(--ink2b)">Kiểm tra Wi-Fi hoặc dữ liệu di động. Mượn, gia hạn và đặt trước cần mạng; tìm sách thì vẫn làm được trong bản lưu trên máy.</p>
        <sc-if value="{{tried}}" hint-placeholder-val="{{ false }}"><div role="status" style="font-size: 13px; font-weight: 600; color: var(--danger)">Vẫn chưa kết nối được (lần thử {{tries}})</div></sc-if>
        <button type="button" onClick="{{retry}}" aria-live="polite" class="st-press" style="${S.btnPrimary}">{{retryLabel}}</button>
        <a href="WideOfflineSearch.dc.html" style="${S.btnGlass}">Tìm sách khi không có mạng</a>
        <a href="WideSignIn.dc.html" style="align-self: center; padding: 12px; font-size: 14px; font-weight: 600">Đăng xuất</a>` +
  S.note('App không tự thử lại liên tục để đỡ tốn pin; khi mạng có lại, Android báo và app thử lại một lần.') + `
      </sc-if>
      <sc-if value="{{isSignedOut}}" hint-placeholder-val="{{ false }}">
        <div style="{{reasonBox}}">
          <div style="font-size: 18px; font-weight: 700; line-height: 1.25">{{reasonTitle}}</div>
          <div style="font-size: 14px; line-height: 1.5">{{reasonBody}}</div>
        </div>
        <h1 style="margin: 4px 0 0; font-size: 22px; font-weight: 700">{{formTitle}}</h1>` +
  input('Email', 'email', 'autocomplete="username"') + input('Mật khẩu', 'password', 'autocomplete="current-password"') + `
        <a href="WideCatalog.dc.html" style="${S.btnPrimary}">Đăng nhập</a>
        <a href="WideSignIn.dc.html" style="${S.btnGlass}">Đăng nhập bằng thẻ thư viện</a>` +
  S.note('Mỗi thông báo ứng với một lý do trong code: {{code}}.') + `
      </sc-if>
    </section>
  </div>
  <div style="position: absolute; left: 50%; bottom: 16px; transform: translateX(-50%); width: 560px">` + S.preview('tabs', 'Xem trước màn', `
      <sc-if value="{{isSignedOut}}" hint-placeholder-val="{{ false }}">
        <div style="display: flex; flex-wrap: wrap; gap: 2px; border-top: 1px solid rgba(255,255,255,0.2); padding-top: 6px">
          <sc-for list="{{reasons}}" as="t" hint-placeholder-count="5">
            <button type="button" aria-pressed="{{t.on}}" onClick="{{t.pick}}" style="{{t.style}}">{{t.label}}</button>
          </sc-for>
        </div>
      </sc-if>`) + `
  </div>` +
  S.foot(W, H, `
  renderVals() {
    var self = this, st = this.state || {}, ui = this.__ui();
    var v = st.v || 'offline', r = st.r || 'reused', tries = st.tries || 0, retrying = !!st.retrying;
    var R = {
      expired: ['Phiên đăng nhập đã hết hạn', 'Vì an toàn, bạn cần đăng nhập lại.', 'SESSION_EXPIRED'],
      reused: ['Đã đăng xuất để bảo vệ tài khoản', 'Phiên của bạn vừa được dùng trên một thiết bị khác. Nếu đó không phải bạn, hãy đổi mật khẩu ngay sau khi đăng nhập.', 'TOKEN_REUSED'],
      idle: ['Đã tự đăng xuất', 'Bạn không dùng app trong 5 phút. Đăng nhập lại để tiếp tục.', 'INACTIVITY'],
      biokey: ['Vân tay trên máy đã thay đổi', 'Khóa vân tay của ShelfTap không còn dùng được. Đăng nhập lại bằng mật khẩu hoặc thẻ, rồi bật lại trong Tài khoản.', 'LOCK_KEY_INVALIDATED'],
      integrity: ['Máy này có thể không an toàn', 'ShelfTap thấy máy đã root hoặc app không phải bản gốc. Bạn vẫn dùng được, nhưng thư viện cũng nhận được cảnh báo này.', 'IntegrityBanner']
    }[r];
    var warn = r === 'reused' || r === 'integrity';
    var labels = { splash: 'Khởi động', lock: 'Khóa sinh trắc học', offline: 'Mất kết nối', signedout: 'Lý do đăng xuất' };
    var open = !!st.open;
    return Object.assign(this.__theme(), {
      tabs: ui.tabs([['splash', 'Khởi động'], ['lock', 'Khóa'], ['offline', 'Mất kết nối'], ['signedout', 'Đăng xuất']], v, 'v', { open: false }),
      reasons: ui.tabs([['expired', 'Hết phiên'], ['reused', 'Nơi khác'], ['idle', '5 phút'], ['biokey', 'Vân tay'], ['integrity', 'Máy lạ']], r, 'r'),
      paneLabel: labels[v],
      isSplash: v === 'splash', isLock: v === 'lock', isOffline: v === 'offline', isSignedOut: v === 'signedout',
      routes: [['Khóa sinh trắc học', 'còn phiên, bật vân tay', 'lock'], ['Màn chào', 'chưa đăng nhập', 'signin'], ['Mất kết nối', 'không gọi được server', 'offline']].map(function (x) {
        return { label: x[0], when: x[1], go: function () { if (x[2] !== 'signin') self.setState({ v: x[2] }); } };
      }),
      locked: !open, unlocked: open,
      lockTitle: open ? 'Đã mở khóa' : 'ShelfTap đang khóa',
      lockBody: open ? 'Chào mừng bạn quay lại.' : 'Ứng dụng tự khóa khi bạn chuyển sang ứng dụng khác.',
      unlock: function () { self.setState({ open: true }); },
      tries: tries, tried: tries > 0 && !retrying, retryLabel: retrying ? 'Đang thử lại…' : 'Thử lại',
      retry: function () { if (retrying) return; self.setState({ retrying: true }); setTimeout(function () { self.setState({ retrying: false, tries: tries + 1 }); }, 1300); },
      reasonTitle: R[0], reasonBody: R[1], code: R[2],
      formTitle: r === 'integrity' ? 'Tiếp tục đăng nhập' : 'Đăng nhập lại',
      reasonBox: 'padding:14px 16px;border-radius:20px;display:flex;flex-direction:column;gap:4px;background:' + (warn ? 'var(--danger-bg)' : 'var(--surface)') + ';color:' + (warn ? 'var(--danger)' : 'var(--ink)')
    });
  }`);

// ---------- Tự mượn ----------
out['WideCheckout.dc.html'] = S.head('Tự mượn sách ở cửa sổ rộng', W, H) +
  S.nav('WideCheckout.dc.html', { preview: ['tabs', 'Xem trước trạng thái'] }) + `
  <section aria-label="Chỗ chạm" class="st-list st-scroll" style="position: absolute; ${S.listPos(320)}; top: ${S.isMid() ? 40 : 16}px; ${S.isMid() ? 'height: 380px' : 'bottom: 16px'}; overflow-y: auto; display: flex; flex-direction: column; gap: 12px">
    <h1 style="margin: 12px 4px 0; font-size: 28px; line-height: 1.15; font-weight: 700; letter-spacing: -0.3px">Tự mượn sách</h1>
    <div aria-hidden="true" style="position: relative; height: ${S.isMid() ? 200 : 260}px; flex: none; border-radius: 30px; background:  rgba(var(--glass),0.55); border: 1px solid rgba(var(--edge),0.85); overflow: hidden">
      <div style="position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%); width: 236px; height: 200px; border-radius: 22px; border: 3px solid var(--ink3); display: flex">
        <div style="flex: 1; border-right: 2px dashed var(--ink3)"></div>
        <div style="flex: 1; position: relative">
          <span class="st-ping" style="position: absolute; left: 50%; top: 38%; width: 64px; height: 64px; margin: -32px 0 0 -32px; border-radius: 999px; border: 3px solid var(--ok)"></span>
          <span class="st-ping st-late" style="position: absolute; left: 50%; top: 38%; width: 64px; height: 64px; margin: -32px 0 0 -32px; border-radius: 999px; border: 3px solid var(--ok)"></span>
          <span style="position: absolute; left: 50%; top: 38%; width: 22px; height: 22px; margin: -11px 0 0 -11px; border-radius: 999px; background: var(--ok)"></span>
        </div>
      </div>
    </div>
    <p style="margin: 0; padding: 0 4px; font-size: 14px; line-height: 1.5; color: var(--ink2b)">Áp mặt sau máy, ở chấm xanh, vào nhãn dán bên trong bìa sách.</p>` +
  S.note('Chấm xanh là chỗ có ăng-ten NFC. Máy gập và tablet mỗi mẫu một chỗ; từ Android 14 app đọc vị trí ăng-ten từ hệ thống (NfcAntennaInfo) để vẽ đúng, máy không có thông tin này thì hiện lời dặn chung. [Vị trí trên hình là ví dụ]', '; padding: 0 4px') + `
  </section>
  <section aria-label="Kết quả mượn" aria-live="polite" class="st-pane" style="position: absolute; ${S.panePos()}; top: ${S.isMid() ? 436 : 16}px; bottom: 16px; border-radius: 30px; overflow: hidden; background:  rgba(var(--glass),0.55); border: 1px solid rgba(var(--edge),0.85)">
    <div class="st-scroll st-detail" style="position: absolute; inset: 0; padding: 72px 22px 24px; box-sizing: border-box; overflow-y: auto; display: flex; flex-direction: column; gap: 14px">
      <sc-if value="{{isWait}}" hint-placeholder-val="{{ true }}">
        <div style="font-size: 20px; font-weight: 700; line-height: 1.25">Sẵn sàng đọc nhãn sách</div>
        <p style="margin: 0; font-size: 14px; line-height: 1.5; color: var(--ink2b)">Giữ máy yên trên bìa sách cho đến khi có tiếng rung.</p>
      </sc-if>
      <sc-if value="{{isSending}}" hint-placeholder-val="{{ false }}">
        <div style="display: flex; align-items: center; gap: 10px; font-size: 14px; font-weight: 600; color: var(--ok)">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="color: var(--ok)"><path d="m5 12 5 5 9-10"></path></svg>
          Đã đọc nhãn sách
        </div>
        <div style="display: flex; align-items: center; gap: 10px; font-size: 20px; font-weight: 700">
          <svg class="st-spin" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" aria-hidden="true" style="color: var(--ink)"><path d="M21 12a9 9 0 1 1-6.2-8.6"></path></svg>
          Đang gửi yêu cầu mượn…
        </div>
        <p style="margin: 0; font-size: 14px; line-height: 1.5; color: var(--ink2b)">Thư viện kiểm tra sách còn cho mượn, số sách bạn đang mượn và tiền phạt. Chỉ khi thư viện đồng ý, sách mới được ghi là đã mượn.</p>
      </sc-if>
      <sc-if value="{{isDone}}" hint-placeholder-val="{{ false }}">
        <div class="st-pop" style="width: 56px; height: 56px; border-radius: 999px; background:  #2f5b3a; display: flex; align-items: center; justify-content: center">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path class="st-draw" d="m5 12 5 5 9-10"></path></svg>
        </div>
        <div style="font-size: 22px; font-weight: 700">Đã mượn: Số đỏ</div>
        <div style="font-size: 15px; color: var(--ink2b)">Hạn trả 07/12/2026 · 14 ngày</div>
        <button type="button" onClick="{{toWait}}" class="st-press" style="${S.btnPrimary}">Mượn cuốn khác</button>
        <a href="WideLoans.dc.html" style="${S.btnGlass}">Xem sách đang mượn</a>
      </sc-if>
      <sc-if value="{{isError}}" hint-placeholder-val="{{ false }}">
        <div style="font-size: 22px; font-weight: 700; color: var(--danger)">Chưa mượn được</div>
        <p style="margin: 0; font-size: 14px; line-height: 1.5; color: var(--ink2b)">Bạn còn 2.000 ₫ tiền phạt chưa trả. Thư viện không cho mượn thêm cho tới khi thanh toán tại quầy.</p>
        <a href="WideLoans.dc.html" style="${S.btnPrimary}">Xem khoản phạt</a>
        <button type="button" onClick="{{toWait}}" class="st-press" style="${S.btnGlass}">Thử sách khác</button>
      </sc-if>
    </div>
  </section>` +
  S.foot(W, H, `
  renderVals() {
    var self = this, st = this.state || {}, ui = this.__ui();
    var s = st.s || 'wait';
    return Object.assign(this.__theme(), {
      tabs: ui.tabs([['wait', 'Chờ chạm'], ['sending', 'Đang gửi'], ['done', 'Thành công'], ['error', 'Lỗi']], s, 's'),
      isWait: s === 'wait', isSending: s === 'sending', isDone: s === 'done', isError: s === 'error',
      toWait: function () { self.setState({ s: 'wait' }); }
    });
  }`);

// ---------- Khách ----------
out['WideGuest.dc.html'] = S.head('Chế độ khách ở cửa sổ rộng', W, H) + S.nav('WideGuest.dc.html', { guest: true }) +
  S.listPane('Danh sách sách', 'Danh mục', S.searchAndChips, 'rows', `
      <sc-if value="{{empty}}" hint-placeholder-val="{{ false }}">
        <li style="padding: 24px 8px; font-size: 14px; color: var(--ink3)">Không có sách khớp. Thử bỏ dấu hoặc chọn "Tất cả".</li>
      </sc-if>`) +
  S.detailPane('Chi tiết sách', `
      <sc-if value="{{hasSel}}" hint-placeholder-val="{{ true }}">` + S.bookHead(true) +
  S.facts([S.fact('Tình trạng', '{{sel.status}}', '{{sel.statusStyle}}'), S.fact('Kệ', '{{sel.shelf}}'), S.fact('Hạn mượn', '14 ngày')]) + `
        <p style="margin: 0; font-size: 14px; line-height: 1.55; color: var(--ink2b)">[Mô tả sách: chưa có trong dữ liệu danh mục] · ISBN {{sel.isbn}}</p>
        <sc-if value="{{asking}}" hint-placeholder-val="{{ false }}">
          <div role="alertdialog" aria-label="Đăng nhập để đặt trước" class="st-fade" style="padding: 16px; border-radius: 22px; background: var(--dialog); border: 1px solid rgba(var(--ink-rgb),0.5); display: flex; flex-direction: column; gap: 10px">
            <div style="font-size: 17px; font-weight: 700">Đăng nhập để đặt trước</div>
            <p style="margin: 0; font-size: 14px; line-height: 1.5; color: var(--ink2)">Đặt trước và mượn sách cần tài khoản thư viện. Sau khi đăng nhập, bạn quay lại đúng cuốn sách này.</p>
            <a href="WideSignIn.dc.html" style="${S.btnPrimary}">Đăng nhập</a>
            <a href="WideSignIn.dc.html" style="${S.btnGlass}">Đăng nhập bằng thẻ</a>
            <button type="button" onClick="{{later}}" style="min-height: 48px; border: 0; background:  transparent; font: inherit; font-size: 15px; font-weight: 600; color: var(--ink); cursor: pointer">Để sau</button>
          </div>
        </sc-if>
      </sc-if>`, `
      <button type="button" onClick="{{ask}}" class="st-press" style="${S.btnPrimary}; flex: 1">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M6 3h12v18l-6-4-6 4z"></path></svg>
        Đặt trước
      </button>`) +
  S.foot(W, H, `
  renderVals() {
    var self = this, st = this.state || {}, ui = this.__ui();
    ${S.SEARCH_JS}
    var sel = hits.filter(function (b) { return b.isbn === (st.sel || '9780990000020'); })[0] || hits[0] || null;
    var out = sel && sel.n === 0;
    return Object.assign(this.__theme(), search, {
      rows: hits.map(function (b, i) {
        return ui.row({ i: i, title: b.t, author: b.a, bg: b.bg, fg: b.fg, on: sel && b.isbn === sel.isbn,
          status: b.n ? 'Còn ' + b.n + ' bản' : 'Đang được mượn', pill: ui.pill(b.n ? 'ok' : 'bad'), note: 'Kệ ' + b.shelf,
          pick: function () { self.setState({ sel: b.isbn, asking: false }); } });
      }),
      hasSel: !!sel,
      sel: sel ? { title: sel.t, author: sel.a, shelf: sel.shelf, isbn: sel.isbn, coverStyle: ui.bigCover(sel.bg, sel.fg),
        tags: sel.s.split(' · ').map(function (x) { return { label: x }; }),
        status: out ? 'Đang được mượn' : 'Còn ' + sel.n + ' bản',
        statusStyle: 'font-size:15px;font-weight:700;line-height:1.25;color:' + (out ? 'var(--danger2)' : 'var(--ok)') } : { tags: [] },
      asking: st.asking === undefined ? true : !!st.asking,
      ask: function () { self.setState({ asking: true }); },
      later: function () { self.setState({ asking: false }); }
    });
  }`);

return out;
};
