/**
 * 매니저 예약 관리 (static/manager/reservation.html)
 *
 * API
 *  GET   /manager/reservations         : 내 매장 예약 전체 조회
 *  PUT   /manager/reservations/{id}    : 예약 수정
 *  PATCH /manager/reservations/{id}    : 예약 취소
 */
const $ = (sel) => document.querySelector(sel);

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
const storeId   = params.get('storeId');
const storeName = params.get('storeName') || '내 매장';

/* 페이지 헤더 업데이트 */
$('#heroEyebrow').textContent = `// Roomescape · ${storeName} · 예약 관리`;
$('#heroTitle').textContent = `${storeName} 예약 관리`;

const STATUS_LABEL = { RESERVED: '예약됨', CANCELED: '취소됨', COMPLETED: '이용완료' };
let cachedList = [];
let cachedThemes = [];

async function api(path, options = {}) {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    let msg;
    try { const b = await res.json(); msg = b.message || JSON.stringify(b); }
    catch (_) { msg = `요청 실패 (HTTP ${res.status})`; }
    throw new Error(msg);
  }
  if (res.status === 204) return null;
  return res.json();
}

function escapeHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function setMessage(msg, isError = false) {
  const el = $('#message');
  el.textContent = msg;
  el.classList.toggle('error', isError);
}

/* ── 리스트 렌더링 ── */
function renderReservations(list) {
  const tbody = $('#reservationRows');
  tbody.innerHTML = '';

  if (!list.length) {
    tbody.innerHTML = '<tr><td colspan="7" class="muted" style="text-align:center;">조건에 맞는 예약이 없습니다.</td></tr>';
    $('#listSummary').textContent = '0 건';
    return;
  }

  list.forEach((r) => {
    const status  = r.status ?? 'RESERVED';
    const canEdit = status === 'RESERVED';
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td class="num">${r.id}</td>
      <td>${escapeHtml(r.memberName ?? '')}</td>
      <td>${escapeHtml(r.date)}</td>
      <td>${escapeHtml(r.time?.startAt ?? '')}</td>
      <td>${escapeHtml(r.theme?.name ?? '')}</td>
      <td><span class="badge badge-${status}">${STATUS_LABEL[status] ?? status}</span></td>
      <td class="actions" style="display:flex; gap:6px; justify-content:flex-end; flex-wrap:wrap;">
        ${canEdit ? `<button class="ghost" data-edit-id="${r.id}" style="font-size:12px;" type="button">수정</button>` : ''}
        ${canEdit ? `<button class="danger" data-cancel-id="${r.id}" style="font-size:12px;" type="button">취소</button>` : ''}
      </td>
    `;
    tbody.appendChild(tr);
  });

  $('#listSummary').textContent = `${list.length} 건`;
}

async function refreshList() {
  cachedList = await api('/manager/reservations');

  // storeId 필터 (매니저가 여러 매장 관리 시)
  const filtered = storeId
    ? cachedList.filter(r => String(r.theme?.storeId) === String(storeId))
    : cachedList;

  renderReservations(filtered);
}

/* ── 수정 모달 ── */
let _pinTimeId    = '';
let _pinTimeLabel = '';

async function loadEditTimes(themeId, date, pinTimeId = null, pinTimeLabel = null) {
  const sel = $('#editTime');
  if (!themeId || !date) {
    sel.innerHTML = '<option value="">테마와 날짜를 먼저 선택하세요</option>';
    return;
  }
  sel.innerHTML = '<option value="">불러오는 중…</option>';
  sel.disabled = true;
  try {
    const times = await api(`/themes/${themeId}/available-times?date=${date}`);
    sel.innerHTML = '';

    if (pinTimeId && !times.some(t => String(t.id) === String(pinTimeId))) {
      const opt = document.createElement('option');
      opt.value = pinTimeId;
      opt.textContent = `${pinTimeLabel} (현재 예약)`;
      sel.appendChild(opt);
    }

    times.forEach(t => {
      const opt = document.createElement('option');
      opt.value = t.id;
      opt.textContent = t.startAt;
      sel.appendChild(opt);
    });

    if (!times.length && !pinTimeId) {
      sel.innerHTML = '<option value="">예약 가능한 시간이 없습니다</option>';
    } else {
      sel.value = pinTimeId || (times[0]?.id ?? '');
    }
  } catch (_) {
    sel.innerHTML = '<option value="">시간 로딩 실패</option>';
  } finally {
    sel.disabled = false;
  }
}

function openEditModal(id) {
  const r = cachedList.find(x => x.id === id);
  if (!r) return;

  _pinTimeId    = String(r.time?.id ?? '');
  _pinTimeLabel = r.time?.startAt ?? '';

  $('#editId').value   = r.id;
  $('#editDate').value = r.date;
  $('#editModalTitle').textContent = `예약 #${r.id} 수정 (예약자: ${r.memberName ?? ''})`;

  // 테마 셀렉트 채우기 (내 매장 테마만)
  const themeSel = $('#editTheme');
  themeSel.innerHTML = '';
  cachedThemes.forEach(t => {
    const opt = document.createElement('option');
    opt.value = t.id;
    opt.textContent = t.name;
    themeSel.appendChild(opt);
  });
  themeSel.value = r.theme?.id ?? '';

  loadEditTimes(r.theme?.id, r.date, _pinTimeId, _pinTimeLabel);
  $('#editModal').classList.remove('hidden');
}

function closeEditModal() {
  $('#editModal').classList.add('hidden');
}

$('#editDate').addEventListener('change', () => {
  loadEditTimes($('#editTheme').value, $('#editDate').value);
});
$('#editTheme').addEventListener('change', () => {
  loadEditTimes($('#editTheme').value, $('#editDate').value);
});
$('#editModalClose').addEventListener('click', closeEditModal);
$('#editModal').addEventListener('click', e => { if (e.target === $('#editModal')) closeEditModal(); });

$('#editSaveBtn').addEventListener('click', async () => {
  const id = Number($('#editId').value);
  const payload = {
    date:    $('#editDate').value,
    timeId:  Number($('#editTime').value)  || null,
    themeId: Number($('#editTheme').value) || null,
  };
  if (!payload.date || !payload.timeId || !payload.themeId) {
    setMessage('모든 필드를 입력해 주세요.', true);
    return;
  }
  try {
    await api(`/manager/reservations/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
    closeEditModal();
    await refreshList();
    setMessage(`#${id} 예약을 수정했습니다.`);
  } catch (e) {
    setMessage(e.message, true);
  }
});

/* ── 테이블 이벤트 ── */
$('#reservationRows').addEventListener('click', async (e) => {
  const editBtn   = e.target.closest('button[data-edit-id]');
  const cancelBtn = e.target.closest('button[data-cancel-id]');

  if (editBtn) {
    openEditModal(Number(editBtn.dataset.editId));
    return;
  }
  if (cancelBtn) {
    const id = cancelBtn.dataset.cancelId;
    if (!confirm(`#${id} 예약을 취소하시겠습니까?`)) return;
    try {
      await api(`/manager/reservations/${id}`, { method: 'PATCH' });
      await refreshList();
      setMessage(`#${id} 예약을 취소했습니다.`);
    } catch (e) {
      setMessage(e.message, true);
    }
  }
});

$('#refreshList').addEventListener('click', async () => {
  try { await refreshList(); setMessage('새로고침 완료.'); }
  catch (e) { setMessage(e.message, true); }
});

/* ── Init ── */
(async function init() {
  try {
    // 내 매장 테마 로드 (수정 모달용)
    cachedThemes = storeId
      ? (await api('/manager/themes')).filter(t => String(t.storeId) === String(storeId))
      : await api('/manager/themes');

    await refreshList();
    setMessage('초기 로딩 완료.');
  } catch (e) {
    setMessage(e.message, true);
  }
})();
