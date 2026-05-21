/**
 * 관리자 예약 관리 (static/admin/reservation.html)
 *
 * API
 *  GET   /admin/reservations?memberId=&from=&to=&themeId= : 필터 조회
 *  POST  /admin/reservations                              : 예약 추가 (관리자 본인 계정으로 생성)
 *  PUT   /admin/reservations/{id}                         : 예약 수정 (RESERVED만)
 *  PATCH /admin/reservations/{id}                         : 예약 취소 (RESERVED만)
 *  GET   /admin/times                                     : 시간 슬롯 목록
 *  GET   /themes                                          : 테마 목록
 */
const $ = (sel) => document.querySelector(sel);

/* URL params (지점 필터) */
const _urlParams  = new URLSearchParams(location.search);
const _storeId    = _urlParams.get('storeId') ? Number(_urlParams.get('storeId')) : null;
const _storeName  = _urlParams.get('storeName') || null;

/* 페이지 타이틀 업데이트 */
if (_storeName) {
  const eyebrow = document.getElementById('heroEyebrow');
  const title   = document.getElementById('heroTitle');
  if (eyebrow) eyebrow.textContent = `// Roomescape · Admin · ${_storeName} · 예약`;
  if (title)   title.textContent   = `${_storeName} 예약 관리`;
}

const state = { themes: [], times: [] };
let cachedList = [];

const STATUS_LABEL = { RESERVED: "예약됨", CANCELED: "취소됨", COMPLETED: "이용완료" };

/* ── API ── */
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

/* ── Selectors ── */
function fillSelect(selectEl, items, placeholder, labelFn) {
  const current = selectEl.value;
  selectEl.innerHTML = `<option value="">${placeholder}</option>`;
  items.forEach((item) => {
    const opt = document.createElement("option");
    opt.value = item.id;
    opt.textContent = labelFn(item);
    selectEl.appendChild(opt);
  });
  if (current) selectEl.value = current;
}

async function loadSelectors() {
  const [themes, times] = await Promise.all([api("/themes"), api("/admin/times")]);
  state.themes = themes;
  state.times = times;

  const themeLabel = (t) => t.name;
  const timeLabel  = (t) => t.startAt;

  fillSelect($("#filterTheme"), themes, "전체 테마", themeLabel);
  fillSelect(document.querySelector('#createForm select[name="themeId"]'), themes, "테마 선택", themeLabel);
  fillSelect(document.querySelector('#createForm select[name="timeId"]'), times, "시간 선택", timeLabel);
  fillSelect($("#editTheme"), themes, "테마 선택", themeLabel);
}

/* ── 수정 모달 — 예약 가능 시간 동적 조회 ── */
let _adminPinTimeId    = "";
let _adminPinTimeLabel = "";

async function loadAdminEditTimes(themeId, date, pinTimeId = null, pinTimeLabel = null) {
  const sel = $("#editTime");
  if (!themeId || !date) {
    sel.innerHTML = '<option value="">테마와 날짜를 먼저 선택하세요</option>';
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

/* ── List ── */
function renderReservations(list) {
  const tbody = $("#reservationRows");
  tbody.innerHTML = "";

  if (!list.length) {
    tbody.innerHTML = '<tr><td colspan="7" class="muted" style="text-align:center;">조건에 맞는 예약이 없습니다.</td></tr>';
    $("#listSummary").textContent = "0 건";
    return;
  }

  list.forEach((r) => {
    const status = r.status ?? "RESERVED";
    const canEdit = status === "RESERVED";
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td class="num">${r.id}</td>
      <td>${escapeHtml(r.memberName ?? "")}</td>
      <td>${escapeHtml(r.date)}</td>
      <td>${escapeHtml(r.time?.startAt ?? "")}</td>
      <td>${escapeHtml(r.theme?.name ?? "")}</td>
      <td><span class="badge badge-${status}">${STATUS_LABEL[status] ?? status}</span></td>
      <td class="actions" style="display:flex; gap:6px; justify-content:flex-end; flex-wrap:wrap;">
        ${canEdit ? `<button class="ghost" data-edit-id="${r.id}" style="font-size:12px;" type="button">수정</button>` : ""}
        ${canEdit ? `<button class="danger" data-cancel-id="${r.id}" style="font-size:12px;" type="button">취소</button>` : ""}
      </td>
    `;
    tbody.appendChild(tr);
  });

  $("#listSummary").textContent = `${list.length} 건`;
}

function buildQuery() {
  const from    = $("#filterFrom").value;
  const to      = $("#filterTo").value;
  const themeId = $("#filterTheme").value;
  const params  = new URLSearchParams();
  if (from)     params.append("from", from);
  if (to)       params.append("to", to);
  if (themeId)  params.append("themeId", themeId);
  const qs = params.toString();
  return qs ? `?${qs}` : "";
}

async function refreshList() {
  cachedList = await api(`/admin/reservations${buildQuery()}`);
  applyNameFilter();
}

function applyNameFilter() {
  const keyword = ($("#filterMemberName").value ?? "").trim().toLowerCase();
  let filtered = keyword
    ? cachedList.filter((r) => (r.memberName ?? "").toLowerCase().includes(keyword))
    : cachedList;
  // 지점 필터 적용
  if (_storeId) {
    filtered = filtered.filter(r => r.theme?.storeId === _storeId);
  }
  renderReservations(filtered);
}

/* ── Member search (create form) ── */
function renderMemberSearchResult(members) {
  const box = $("#memberSearchResult");
  if (!members.length) {
    box.style.display = "block";
    box.innerHTML = `<div style="padding:10px 14px; font-size:13px; color:var(--muted);">검색 결과가 없습니다.</div>`;
    return;
  }
  box.style.display = "block";
  box.innerHTML = members.map((m) => `
    <div data-member-id="${m.id}" data-member-name="${escapeHtml(m.name)}"
         style="padding:10px 14px; cursor:pointer; font-size:14px; border-bottom:1px solid var(--border);"
         onmouseover="this.style.background='var(--surface-hover)'"
         onmouseout="this.style.background=''">
      ${escapeHtml(m.name)} <span style="color:var(--muted); font-size:12px;">(${escapeHtml(m.loginId)})</span>
    </div>
  `).join("");
}

function selectMember(id, name) {
  $("#selectedMemberId").value = id;
  $("#selectedMemberLabel").textContent = `선택된 회원: ${name}`;
  $("#memberSearchResult").style.display = "none";
  $("#memberSearchInput").value = name;
}

$("#memberSearchBtn").addEventListener("click", async () => {
  const name = $("#memberSearchInput").value.trim();
  if (!name) { setMessage("검색할 이름을 입력해 주세요.", true); return; }
  try {
    const members = await api(`/admin/members?name=${encodeURIComponent(name)}`);
    renderMemberSearchResult(members);
  } catch (e) {
    setMessage(e.message, true);
  }
});

$("#memberSearchInput").addEventListener("keydown", (e) => {
  if (e.key === "Enter") { e.preventDefault(); $("#memberSearchBtn").click(); }
});

$("#memberSearchResult").addEventListener("click", (e) => {
  const row = e.target.closest("[data-member-id]");
  if (row) selectMember(row.dataset.memberId, row.dataset.memberName);
});

/* ── Create ── */
$("#createBtn").addEventListener("click", async () => {
  const form     = new FormData($("#createForm"));
  const memberId = $("#selectedMemberId").value;
  const payload = {
    date:     form.get("date"),
    timeId:   form.get("timeId")  ? Number(form.get("timeId"))  : null,
    themeId:  form.get("themeId") ? Number(form.get("themeId")) : null,
    memberId: memberId ? Number(memberId) : null,
  };
  if (!payload.memberId) {
    setMessage("회원을 검색하여 선택해 주세요.", true);
    return;
  }
  if (!payload.date || !payload.timeId || !payload.themeId) {
    setMessage("모든 필드를 입력해 주세요.", true);
    return;
  }
  try {
    const created = await api("/admin/reservations", { method: "POST", body: JSON.stringify(payload) });
    $("#createForm").reset();
    $("#selectedMemberId").value = "";
    $("#selectedMemberLabel").textContent = "";
    $("#memberSearchResult").style.display = "none";
    await refreshList();
    setMessage(`추가 완료 · #${created.id} ${created.memberName ?? ""} / ${created.date} ${created.time?.startAt ?? ""}`);
  } catch (e) {
    setMessage(e.message, true);
  }
});

/* ── Table row actions ── */
$("#reservationRows").addEventListener("click", async (e) => {
  const editBtn   = e.target.closest("button[data-edit-id]");
  const cancelBtn = e.target.closest("button[data-cancel-id]");

  if (editBtn) {
    openEditModal(Number(editBtn.dataset.editId));
    return;
  }
  if (cancelBtn) {
    const id = cancelBtn.dataset.cancelId;
    if (!confirm(`#${id} 예약을 취소하시겠습니까?`)) return;
    try {
      await api(`/admin/reservations/${id}`, { method: "PATCH" });
      await refreshList();
      setMessage(`#${id} 예약을 취소했습니다.`);
    } catch (e) {
      setMessage(e.message, true);
    }
  }
});

/* ── Edit modal ── */
function openEditModal(id) {
  const r = cachedList.find((x) => x.id === id);
  if (!r) return;

  _adminPinTimeId    = String(r.time?.id    ?? "");
  _adminPinTimeLabel = r.time?.startAt ?? "";

  $("#editId").value    = r.id;
  $("#editDate").value  = r.date;
  $("#editTheme").value = r.theme?.id ?? "";
  $("#editModalTitle").textContent = `예약 #${r.id} 수정 (예약자: ${r.memberName ?? ""})`;

  loadAdminEditTimes(r.theme?.id, r.date, _adminPinTimeId, _adminPinTimeLabel);
  $("#editModal").classList.remove("hidden");
}

function closeEditModal() {
  $("#editModal").classList.add("hidden");
}

$("#editDate").addEventListener("change", () => {
  loadAdminEditTimes($("#editTheme").value, $("#editDate").value);
});

$("#editTheme").addEventListener("change", () => {
  loadAdminEditTimes($("#editTheme").value, $("#editDate").value);
});

$("#editModalClose").addEventListener("click", closeEditModal);
$("#editModal").addEventListener("click", (e) => {
  if (e.target === $("#editModal")) closeEditModal();
});

$("#editSaveBtn").addEventListener("click", async () => {
  const id = Number($("#editId").value);
  const payload = {
    date:    $("#editDate").value,
    timeId:  Number($("#editTime").value)  || null,
    themeId: Number($("#editTheme").value) || null,
  };
  if (!payload.date || !payload.timeId || !payload.themeId) {
    setMessage("모든 필드를 입력해 주세요.", true);
    return;
  }
  try {
    await api(`/admin/reservations/${id}`, { method: "PUT", body: JSON.stringify(payload) });
    closeEditModal();
    await refreshList();
    setMessage(`#${id} 예약을 수정했습니다.`);
  } catch (e) {
    setMessage(e.message, true);
  }
});

/* ── Filter controls ── */
$("#applyFilter").addEventListener("click", async () => {
  try { await refreshList(); setMessage("조회 완료."); }
  catch (e) { setMessage(e.message, true); }
});

$("#resetFilter").addEventListener("click", async () => {
  $("#filterMemberName").value = "";
  $("#filterFrom").value       = "";
  $("#filterTo").value         = "";
  $("#filterTheme").value      = "";
  try { await refreshList(); setMessage("필터를 초기화했습니다."); }
  catch (e) { setMessage(e.message, true); }
});

$("#refreshList").addEventListener("click", async () => {
  try { await refreshList(); setMessage("새로고침 완료."); }
  catch (e) { setMessage(e.message, true); }
});

/* ── Init ── */
(async function init() {
  try {
    await loadSelectors();
    await refreshList();
    setMessage("초기 로딩 완료.");
  } catch (e) {
    setMessage(e.message, true);
  }
})();
