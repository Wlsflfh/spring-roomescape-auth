/**
 * 인증 공통 유틸리티
 *
 * localStorage 키: 'loginMember' → { id, loginId, name }
 * 로그인 → setMember()   로그아웃 → logout()
 */
const Auth = (() => {
  const KEY = 'loginMember';

  function getMember() {
    try { return JSON.parse(localStorage.getItem(KEY)); }
    catch (_) { return null; }
  }

  function setMember(m) {
    localStorage.setItem(KEY, JSON.stringify(m));
  }

  function clear() {
    localStorage.removeItem(KEY);
  }

  function isLoggedIn() {
    return !!getMember();
  }

  async function logout() {
    try { await fetch('/login/logout', { method: 'POST' }); } catch (_) {}
    try { await fetch('/admin/logout',  { method: 'POST' }); } catch (_) {}
    clear();
    location.href = '/';
  }

  /**
   * nav-actions 영역을 로그인 상태에 따라 렌더링.
   *
   * @param {string}  containerId  nav-actions 요소의 id (기본 'navActions')
   * @param {object}  opts
   *   hideMyReservations: true  → "내 예약 조회" 버튼 숨김 (my-reservations 페이지 자체에서 사용)
   *   extraLeft: HTML string    → 왼쪽에 추가할 버튼 (← 홈 등)
   */
  function initNav(containerId = 'navActions', opts = {}) {
    const container = document.getElementById(containerId);
    if (!container) return;

    const member = getMember();
    let html = '';

    if (opts.extraLeft) html += opts.extraLeft;

    if (member) {
      if (!opts.hideMyReservations) {
        html += `<a class="nav-btn" href="/my-reservations.html">🔍 내 예약 조회</a>`;
      }
      html += `<button class="nav-btn" id="logoutNavBtn">로그아웃</button>`;
    } else {
      html += `<a class="nav-btn" href="/login.html">로그인</a>`;
    }

    html += `<button class="nav-btn primary-btn" id="adminNavBtn">⌘ 관리자</button>`;

    container.innerHTML = html;

    // 로그아웃 버튼
    const logoutBtn = document.getElementById('logoutNavBtn');
    if (logoutBtn) logoutBtn.addEventListener('click', logout);

    // 관리자 버튼 → 비밀번호 모달
    const adminBtn = document.getElementById('adminNavBtn');
    if (adminBtn) {
      adminBtn.addEventListener('click', () => {
        openAdminModal();
      });
    }
  }

  /* ── 관리자 비밀번호 모달 ── */
  function openAdminModal() {
    let modal = document.getElementById('adminPasswordModal');
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'adminPasswordModal';
      modal.className = 'modal-overlay';
      modal.innerHTML = `
        <div class="modal-box" style="max-width:360px;">
          <div class="modal-header">
            <h3>⌘ 관리자 인증</h3>
            <button class="modal-close" id="adminModalClose" type="button">✕</button>
          </div>
          <p style="margin:0 0 16px; color:var(--text-2); font-size:14px;">
            관리자 비밀번호를 입력하세요.
          </p>
          <div class="field" style="margin-bottom:12px;">
            <input id="adminPasswordInput" placeholder="관리자 비밀번호" type="password"
              style="width:100%; box-sizing:border-box;" />
          </div>
          <p class="message" id="adminModalMsg" style="margin:0 0 12px;"></p>
          <button class="primary" id="adminModalSubmit" style="width:100%;" type="button">
            확인
          </button>
        </div>
      `;
      document.body.appendChild(modal);

      document.getElementById('adminModalClose').addEventListener('click', closeAdminModal);
      modal.addEventListener('click', (e) => { if (e.target === modal) closeAdminModal(); });
      document.getElementById('adminPasswordInput').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') submitAdminPassword();
      });
      document.getElementById('adminModalSubmit').addEventListener('click', submitAdminPassword);
    }
    modal.classList.remove('hidden');
    document.getElementById('adminPasswordInput').value = '';
    document.getElementById('adminModalMsg').textContent = '';
    document.getElementById('adminPasswordInput').focus();
  }

  function closeAdminModal() {
    const modal = document.getElementById('adminPasswordModal');
    if (modal) modal.classList.add('hidden');
  }

  async function submitAdminPassword() {
    const password = document.getElementById('adminPasswordInput').value.trim();
    const msgEl = document.getElementById('adminModalMsg');
    if (!password) { msgEl.textContent = '비밀번호를 입력해 주세요.'; return; }

    try {
      const res = await fetch('/admin/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password }),
      });
      if (res.ok) {
        closeAdminModal();
        location.href = '/admin/index.html';
      } else {
        const body = await res.json().catch(() => ({}));
        msgEl.textContent = body.message || '비밀번호가 올바르지 않습니다.';
        msgEl.style.color = 'var(--danger)';
      }
    } catch (_) {
      msgEl.textContent = '네트워크 오류가 발생했습니다.';
    }
  }

  return { getMember, setMember, clear, isLoggedIn, logout, initNav, openAdminModal };
})();
