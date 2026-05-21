/**
 * 인증 공통 유틸리티
 *
 * localStorage 키:
 *   'loginMember' → { id, loginId, name, role }  (role: "MEMBER" | "MANAGER")
 *   'accessToken' → 회원 JWT
 *   'adminToken'  → 관리자 JWT
 *   'selectedStore' → { id, name } (선택한 지점)
 *
 * 로그인 → setMember()   로그아웃 → logout()
 */

/* ── 전역 fetch 래핑: 모든 요청에 Authorization 헤더 자동 추가 ── */
(function wrapFetch() {
  const _fetch = window.fetch;
  window.fetch = function (url, options = {}) {
    const path = typeof url === 'string' ? url : url.url;
    const tokenKey = path.startsWith('/admin/') ? 'adminToken' : 'accessToken';
    const token = localStorage.getItem(tokenKey);
    if (token) {
      options.headers = { Authorization: `Bearer ${token}`, ...options.headers };
    }
    return _fetch(url, options);
  };
})();

const Auth = (() => {
  const MEMBER_KEY = 'loginMember';
  const STORE_KEY  = 'selectedStore';

  /* ── 회원 ── */
  function getMember() {
    try { return JSON.parse(localStorage.getItem(MEMBER_KEY)); }
    catch (_) { return null; }
  }

  function setMember(loginResponse) {
    const { accessToken, tokenType, ...member } = loginResponse;
    localStorage.setItem(MEMBER_KEY, JSON.stringify(member));
    if (accessToken) {
      localStorage.setItem('accessToken', accessToken);
    }
  }

  function clear() {
    localStorage.removeItem(MEMBER_KEY);
    localStorage.removeItem('accessToken');
    localStorage.removeItem('adminToken');
  }

  function isLoggedIn() {
    return !!getMember();
  }

  function isManager() {
    const member = getMember();
    return member?.role === 'MANAGER';
  }

  async function logout() {
    try { await fetch('/login/logout', { method: 'POST' }); } catch (_) {}
    try { await fetch('/admin/logout',  { method: 'POST' }); } catch (_) {}
    clear();
    location.href = '/';
  }

  /* ── 지점 선택 ── */
  function getSelectedStore() {
    try { return JSON.parse(localStorage.getItem(STORE_KEY)); }
    catch (_) { return null; }
  }

  function setSelectedStore(store) {
    localStorage.setItem(STORE_KEY, JSON.stringify(store));
  }

  function clearSelectedStore() {
    localStorage.removeItem(STORE_KEY);
  }

  /**
   * nav-actions 영역을 로그인 상태·역할에 따라 렌더링.
   *
   * @param {string}  containerId  nav-actions 요소의 id (기본 'navActions')
   * @param {object}  opts
   *   hideMyReservations: true  → "내 예약 조회" 버튼 숨김
   *   extraLeft: HTML string    → 왼쪽에 추가할 버튼 (← 홈 등)
   *   showStoreBadge: true      → 선택 지점 배지 표시
   */
  function initNav(containerId = 'navActions', opts = {}) {
    const container = document.getElementById(containerId);
    if (!container) return;

    const member = getMember();
    const store  = getSelectedStore();
    let html = '';

    if (opts.extraLeft) html += opts.extraLeft;

    /* 지점 배지 */
    if (opts.showStoreBadge !== false && store) {
      html += `<span class="nav-store-badge" id="storeChangeBadge">📍 ${escapeHtml(store.name)}</span>`;
    }

    if (member) {
      /* 매니저: 내 매장 정보 버튼 추가 */
      if (member.role === 'MANAGER') {
        html += `<a class="nav-btn manager-btn" href="/manager/index.html">🏪 내 매장 정보</a>`;
      }
      /* 내 예약 조회 */
      if (!opts.hideMyReservations) {
        html += `<a class="nav-btn" href="/my-reservations.html">🔍 내 예약 조회</a>`;
      }
      html += `<button class="nav-btn" id="logoutNavBtn">로그아웃</button>`;
    } else {
      html += `<a class="nav-btn" href="/login.html">로그인</a>`;
    }

    html += `<button class="nav-btn primary-btn" id="adminNavBtn">⌘ 관리자</button>`;

    container.innerHTML = html;

    /* 지점 배지 클릭 → 지점 재선택 */
    const storeBadge = document.getElementById('storeChangeBadge');
    if (storeBadge) {
      storeBadge.addEventListener('click', () => {
        clearSelectedStore();
        location.reload();
      });
    }

    /* 로그아웃 버튼 */
    const logoutBtn = document.getElementById('logoutNavBtn');
    if (logoutBtn) logoutBtn.addEventListener('click', logout);

    /* 관리자 버튼 → 비밀번호 모달 */
    const adminBtn = document.getElementById('adminNavBtn');
    if (adminBtn) adminBtn.addEventListener('click', openAdminModal);
  }

  function escapeHtml(str) {
    return String(str ?? '')
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
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
        const body = await res.json().catch(() => ({}));
        if (body.accessToken) {
          localStorage.setItem('adminToken', body.accessToken);
        }
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

  return {
    getMember,
    setMember,
    clear,
    isLoggedIn,
    isManager,
    logout,
    initNav,
    openAdminModal,
    getSelectedStore,
    setSelectedStore,
    clearSelectedStore,
  };
})();
