/**
 * 사용자 예약 조회 페이지 (static/my-reservations.html)
 *
 * API
 *  GET    /reservations/my?from=&to=&themeId=   : 내 예약 조회 (세션 기반)
 *  PUT    /reservations/{id}                     : 예약 수정 (RESERVED만)
 *  PATCH  /reservations/{id}                     : 예약 취소 (RESERVED만)
 *  GET    /themes                                : 테마 목록 (필터 select용)
 *  GET    /themes/{themeId}/available-times?date=: 날짜별 예약 가능 시간
 */
const $ = (sel) => document.querySelector(sel);

// 비로그인 → 로그인 페이지로 리다이렉트
if (!Auth.isLoggedIn()) {
  location.href = '/login.html?next=' + encodeURIComponent(location.href);
}

Auth.initNav('navActions', {
  hideMyReservations: true,
  extraLeft: '<a class="nav-btn" href="/">← 홈</a>',
  showStoreBadge: true,
});

async function api(path, options = {}) {
  const res = await fetch(path, {
    headers: { "Content-Type": "application/json" },
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
  return String(str ?? "")
    .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
}

function setMessage(msg, isError = false) {
  const el = $("#message");
  el.textContent = msg;
  el.classList.toggle("error", isError);
}

const STATUS_LABEL = { RESERVED: "예약됨", CANCELED: "취소됨", COMPLETED: "이용완료" };

// 테마 목록 캐시
let allThemes = [];

/* ── 결과 렌더링 ── */
function renderResults(reservations) {
  const tbody = $("#resultRows");
  tbody.innerHTML = "";

  if (!reservations.length) {
    tbody.innerHTML = '<tr><td colspan="7" class="muted" style="text-align:center;">조건에 맞는 예약이 없습니다.</td></tr>';
    $("#resultSummary").textContent = "0 건";
    $("#resultSection").style.display = "";
    return;
  }

  reservations.forEach((r) => {
    const tr = document.createElement("tr");
    const status = r.status ?? "RESERVED";
    const actionCell = status === "RESERVED"
      ? `<td style="text-align:right;">
           <button class="ghost btn-edit" data-id="${r.id}"
             data-date="${escapeHtml(r.date)}" data-time-id="${r.time?.id ?? ""}"
             data-time-label="${escapeHtml(r.time?.startAt ?? "")}"
             data-theme-id="${r.theme?.id ?? ""}" data-theme-name="${escapeHtml(r.theme?.name ?? "")}"
             type="button">수정</button>
           <button class="ghost btn-cancel" data-id="${r.id}" type="button"
             style="color:var(--danger); margin-left:6px;">취소</button>
         </td>`
      : `<td></td>`;

    tr.innerHTML = `
      <td class="num">${r.id}</td>
      <td>${escapeHtml(r.memberName ?? "")}</td>
      <td>${escapeHtml(r.date)}</td>
      <td>${escapeHtml(r.time?.startAt ?? "")}</td>
      <td>${escapeHtml(r.theme?.name ?? "")}</td>
      <td><span class="badge badge-${status}">${STATUS_LABEL[status] ?? status}</span></td>
      ${actionCell}
    `;
    tbody.appendChild(tr);
  });

  $("#resultSummary").textContent = `${reservations.length} 건`;
  $("#resultSection").style.display = "";
}

/* ── 조회 ── */
async function search() {
  const from    = $("#searchFrom").value;
  const to      = $("#searchTo").value;
  const themeId = $("#searchTheme").value;

  const params = new URLSearchParams();
  if (from)    params.append("from", from);
  if (to)      params.append("to", to);
  if (themeId) params.append("themeId", themeId);
  const qs = params.toString();

  try {
    let data = await api(`/reservations/my${qs ? "?" + qs : ""}`);

    // 선택된 지점에 해당하는 예약만 표시
    const selectedStore = Auth.getSelectedStore();
    if (selectedStore) {
      data = data.filter(r => r.theme?.storeId === selectedStore.id);
    }

    renderResults(data);
    setMessage("조회 완료.");
  } catch (e) {
    setMessage(e.message, true);
  }
}

/* ── 모달: 수정 ── */
let _editPinTimeId    = "";
let _editPinTimeLabel = "";

function openEditModal(btn) {
  _editPinTimeId    = btn.dataset.timeId;
  _editPinTimeLabel = btn.dataset.timeLabel;

  $("#editId").value   = btn.dataset.id;
  $("#editDate").value = btn.dataset.date;

  const themeSel = $("#editTheme");
  themeSel.innerHTML = '<option value="">테마 선택</option>';
  allThemes.forEach((t) => {
    const opt = document.createElement("option");
    opt.value = t.id;
    opt.textContent = t.name;
    themeSel.appendChild(opt);
  });
  themeSel.value = btn.dataset.themeId;

  loadEditTimes(btn.dataset.themeId, btn.dataset.date, _editPinTimeId, _editPinTimeLabel);
  $("#editModal").classList.remove("hidden");
}

async function loadEditTimes(themeId, date, pinTimeId = null, pinTimeLabel = null) {
  const sel = $("#editTime");
  if (!date || !themeId) {
    sel.innerHTML = '<option value="">날짜를 선택해 주세요</option>';
    return;
  }
  sel.innerHTML = '<option value="">불러오는 중…</option>';
  sel.disabled = true;
  try {
    const times = await api(`/themes/${themeId}/available-times?date=${date}`);
    sel.innerHTML = "";

    if (pinTimeId && !times.some((t) => String(t.id) === String(pinTimeId))) {
      const opt = document.createElement("option");
      opt.value = pinTimeId;
      opt.textContent = `${pinTimeLabel} (현재 예약)`;
      sel.appendChild(opt);
    }

    times.forEach((t) => {
      const opt = document.createElement("option");
      opt.value = t.id;
      opt.textContent = t.startAt;
      sel.appendChild(opt);
    });

    if (!times.length && !pinTimeId) {
      sel.innerHTML = '<option value="">예약 가능한 시간이 없습니다</option>';
    } else {
      sel.value = pinTimeId || (times[0]?.id ?? "");
    }
  } catch (_) {
    sel.innerHTML = '<option value="">시간 로딩 실패</option>';
  } finally {
    sel.disabled = false;
  }
}

async function saveEdit() {
  const id      = $("#editId").value;
  const date    = $("#editDate").value;
  const timeId  = Number($("#editTime").value);
  const themeId = Number($("#editTheme").value);

  if (!date || !timeId || !themeId) {
    setMessage("모든 항목을 입력해 주세요.", true);
    return;
  }

  try {
    await api(`/reservations/${id}`, {
      method: "PUT",
      body: JSON.stringify({ date, timeId, themeId }),
    });
    $("#editModal").classList.add("hidden");
    setMessage("예약이 수정되었습니다.");
    search();
  } catch (e) {
    setMessage(e.message, true);
  }
}

async function cancelReservation(id) {
  if (!confirm("예약을 취소하시겠습니까?")) return;
  try {
    await api(`/reservations/${id}`, { method: "PATCH" });
    setMessage("예약이 취소되었습니다.");
    search();
  } catch (e) {
    setMessage(e.message, true);
  }
}

/* ── 이벤트 ── */
$("#resultRows").addEventListener("click", (e) => {
  const editBtn   = e.target.closest(".btn-edit");
  const cancelBtn = e.target.closest(".btn-cancel");
  if (editBtn)   openEditModal(editBtn);
  if (cancelBtn) cancelReservation(cancelBtn.dataset.id);
});

$("#editDate").addEventListener("change", () => {
  loadEditTimes($("#editTheme").value, $("#editDate").value);
});
$("#editTheme").addEventListener("change", () => {
  loadEditTimes($("#editTheme").value, $("#editDate").value);
});

$("#editSaveBtn").addEventListener("click", saveEdit);
$("#editModalClose").addEventListener("click", () => {
  $("#editModal").classList.add("hidden");
});
$("#editModal").addEventListener("click", (e) => {
  if (e.target === $("#editModal")) $("#editModal").classList.add("hidden");
});

/* ── Init ── */
(async function init() {
  try {
    const allFetched = await api("/themes");
    const selectedStore = Auth.getSelectedStore();

    // 선택 지점의 테마만 드롭다운에 표시
    allThemes = selectedStore
      ? allFetched.filter(t => t.storeId === selectedStore.id)
      : allFetched;

    const sel = $("#searchTheme");
    allThemes.forEach((t) => {
      const opt = document.createElement("option");
      opt.value = t.id;
      opt.textContent = t.name;
      sel.appendChild(opt);
    });
  } catch (_) { /* 테마 로딩 실패는 무시 */ }

  // 로그인 상태면 바로 예약 목록 로드
  search();
})();

$("#searchBtn").addEventListener("click", search);
