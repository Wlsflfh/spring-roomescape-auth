/**
 * 매니저 테마 관리 (static/manager/theme.html)
 *
 * API
 *  GET    /manager/themes         : 내 매장 테마 조회
 *  POST   /manager/themes         : 테마 추가
 *  PUT    /manager/themes/{id}    : 테마 수정
 *  DELETE /manager/themes/{id}    : 테마 삭제
 */
const $ = (selector) => document.querySelector(selector);

/* 매니저 권한 확인 */
(function checkManagerAuth() {
  const member = Auth.getMember();
  if (!member) {
    location.href = '/login.html?next=' + encodeURIComponent(location.href);
    return;
  }
  if (member.role !== 'MANAGER') {
    alert('매니저 권한이 필요합니다.');
    location.href = '/';
  }
})();

/* URL params */
const params = new URLSearchParams(location.search);
const storeId   = params.get('storeId') ? Number(params.get('storeId')) : null;
const storeName = params.get('storeName') || '내 매장';

/* 페이지 헤더 업데이트 */
$('#heroEyebrow').textContent = `// Roomescape · ${storeName} · 테마 관리`;
$('#heroTitle').textContent   = `${storeName} 테마 관리`;

/* storeId를 폼 hidden 필드에 미리 설정 */
if (storeId) $('#themeStoreId').value = storeId;

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!response.ok) {
    let message;
    try {
      const body = await response.json();
      message = body.message || JSON.stringify(body);
    } catch (_) {
      message = (await response.text()) || `요청 실패 (HTTP ${response.status})`;
    }
    throw new Error(message);
  }
  if (response.status === 204) return null;
  return response.json();
}

function setMessage(message, isError = false) {
  const el = $('#message');
  el.textContent = message;
  el.classList.toggle('error', isError);
}

function escapeHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

/* ── Form mode ── */
function setFormMode(mode, theme) {
  const form      = $('#themeForm');
  const idEl      = $('#themeId');
  const submitBtn = $('#submitBtn');
  const cancelBtn = $('#cancelEditBtn');
  const title     = $('#formTitle');
  const sub       = $('#formSub');

  if (mode === 'edit' && theme) {
    idEl.value = theme.id;
    form.elements['name'].value         = theme.name;
    form.elements['description'].value  = theme.description;
    form.elements['thumbnailUrl'].value = theme.thumbnailUrl;
    // storeId는 기존 테마 것 유지
    $('#themeStoreId').value = theme.storeId ?? storeId ?? '';
    submitBtn.textContent    = '저장';
    cancelBtn.style.display  = '';
    title.textContent        = `테마 #${theme.id} 수정`;
    sub.textContent          = '변경할 값을 입력하고 [저장] 버튼을 누르세요.';
  } else {
    idEl.value = '';
    form.reset();
    $('#themeStoreId').value = storeId ?? '';
    submitBtn.textContent    = '추가';
    cancelBtn.style.display  = 'none';
    title.textContent        = '테마 추가';
    sub.textContent          = '새로운 테마를 등록합니다.';
  }
}

/* ── List ── */
function renderThemes(themes) {
  const tbody = $('#themeRows');
  tbody.innerHTML = '';

  if (themes.length === 0) {
    tbody.innerHTML = '<tr><td colspan="5" class="muted" style="text-align: center;">등록된 테마가 없습니다.</td></tr>';
    $('#listSummary').textContent = '0 건';
    return;
  }

  themes.forEach((theme) => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td class="num">${theme.id}</td>
      <td><img src="${escapeHtml(theme.thumbnailUrl)}" alt="" style="width:60px;height:40px;object-fit:cover;border-radius:4px;" onerror="this.style.display='none'"></td>
      <td>${escapeHtml(theme.name)}</td>
      <td class="muted">${escapeHtml(theme.description)}</td>
      <td class="actions">
        <button class="ghost" data-edit-id="${theme.id}" type="button">수정</button>
        <button class="danger" data-delete-id="${theme.id}" type="button">삭제</button>
      </td>
    `;
    tbody.appendChild(tr);
  });
  $('#listSummary').textContent = `${themes.length} 건`;
}

let cachedThemes = [];

async function refresh() {
  const all = await api('/manager/themes');
  // 현재 storeId로 필터
  cachedThemes = storeId ? all.filter(t => t.storeId === storeId) : all;
  renderThemes(cachedThemes);
}

/* ── Events ── */
$('#themeForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = new FormData(event.target);
  const id   = form.get('id');

  const resolvedStoreId = storeId || Number(form.get('storeId')) || null;
  const payload = {
    name:         (form.get('name') || '').trim(),
    description:  (form.get('description') || '').trim(),
    thumbnailUrl: (form.get('thumbnailUrl') || '').trim(),
    storeId:      resolvedStoreId,
  };

  if (!payload.name || !payload.description || !payload.thumbnailUrl) {
    setMessage('모든 필드를 입력해 주세요.', true);
    return;
  }
  if (!payload.storeId) {
    setMessage('매장 ID를 확인할 수 없습니다. URL을 확인해 주세요.', true);
    return;
  }

  try {
    if (id) {
      const updated = await api(`/manager/themes/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
      setMessage(`테마를 수정했습니다: #${updated.id} ${updated.name}`);
    } else {
      const created = await api('/manager/themes', { method: 'POST', body: JSON.stringify(payload) });
      setMessage(`테마를 추가했습니다: #${created.id} ${created.name}`);
    }
    setFormMode('create');
    await refresh();
  } catch (error) {
    setMessage(error.message, true);
  }
});

$('#cancelEditBtn').addEventListener('click', () => {
  setFormMode('create');
  setMessage('수정 모드를 취소했습니다.');
});

$('#themeRows').addEventListener('click', async (event) => {
  const editBtn   = event.target.closest('button[data-edit-id]');
  const deleteBtn = event.target.closest('button[data-delete-id]');

  if (editBtn) {
    const id    = Number(editBtn.dataset.editId);
    const theme = cachedThemes.find(t => t.id === id);
    if (!theme) return;
    setFormMode('edit', theme);
    window.scrollTo({ top: 0, behavior: 'smooth' });
    setMessage(`#${id} 테마 수정 모드`);
    return;
  }

  if (deleteBtn) {
    const id = deleteBtn.dataset.deleteId;
    if (!confirm(`#${id} 테마를 삭제하시겠습니까?`)) return;
    try {
      await api(`/manager/themes/${id}`, { method: 'DELETE' });
      await refresh();
      setMessage(`#${id} 테마를 삭제했습니다.`);
    } catch (error) {
      setMessage(error.message, true);
    }
  }
});

$('#refreshBtn').addEventListener('click', async () => {
  try { await refresh(); setMessage('새로고침 완료.'); }
  catch (error) { setMessage(error.message, true); }
});

/* ── Init ── */
setFormMode('create');
refresh()
  .then(() => setMessage('초기 로딩 완료.'))
  .catch((error) => setMessage(error.message, true));
