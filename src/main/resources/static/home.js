/**
 * 홈 페이지 (static/index.html)
 *
 * API
 *  GET /stores         : 지점 목록
 *  GET /themes         : 전체 테마 목록 (storeId, storeName 포함)
 *  GET /themes/popular : 인기 테마 (최근 7일)
 */
const $ = (sel) => document.querySelector(sel);

async function api(path) {
  const res = await fetch(path, { headers: { "Content-Type": "application/json" } });
  if (!res.ok) {
    let msg;
    try { const b = await res.json(); msg = b.message || JSON.stringify(b); }
    catch (_) { msg = `요청 실패 (HTTP ${res.status})`; }
    throw new Error(msg);
  }
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

/* ── 지점 선택 오버레이 ── */
async function initStoreSelector() {
  const overlay = $("#storeOverlay");
  const selected = Auth.getSelectedStore();

  if (selected) {
    // 이미 선택된 지점 있으면 바로 숨김
    overlay.classList.add("hidden");
    updateHeroForStore(selected);
    return;
  }

  // 지점 목록 로드
  try {
    const stores = await api("/stores");
    renderStoreButtons(stores);
  } catch (e) {
    $("#storeBtnGrid").innerHTML = `<p style="color:var(--danger); grid-column:1/-1;">지점 정보를 불러올 수 없습니다: ${escapeHtml(e.message)}</p>`;
  }
}

function renderStoreButtons(stores) {
  const grid = $("#storeBtnGrid");
  const icons = ["🏙️", "🎭", "🌆", "🏬", "🎪"];

  if (!stores.length) {
    grid.innerHTML = '<p style="color:var(--text-2); grid-column:1/-1;">등록된 지점이 없습니다.</p>';
    return;
  }

  grid.innerHTML = stores.map((store, i) => `
    <button class="store-btn" data-store-id="${store.id}" data-store-name="${escapeHtml(store.name)}" type="button">
      <span class="store-icon">${icons[i % icons.length]}</span>
      <span>${escapeHtml(store.name)}</span>
      <span class="store-sub">${escapeHtml(store.description)}</span>
    </button>
  `).join("");

  grid.querySelectorAll(".store-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const store = { id: Number(btn.dataset.storeId), name: btn.dataset.storeName };
      Auth.setSelectedStore(store);
      $("#storeOverlay").classList.add("hidden");
      updateHeroForStore(store);
      loadContent();
    });
  });
}

function updateHeroForStore(store) {
  $("#heroEyebrow").textContent = `// Roomescape · ${store.name}`;
  $("#heroTitle").textContent = "방을 고르고, 시간을 잡아라.";
  $("#heroDesc").textContent = `${store.name}의 테마를 선택하면 날짜·시간 예약 페이지로 이동한다.`;
  $("#themeSectionTitle").textContent = `${store.name} 테마`;
}

/* ── Theme grid ── */
function renderThemes(themes) {
  const root = $("#themeGrid");
  root.innerHTML = "";

  if (!themes.length) {
    root.innerHTML = '<p class="chip-empty">이 지점에 등록된 테마가 없습니다.</p>';
    return;
  }

  themes.forEach((theme) => {
    const card = document.createElement("a");
    card.className = "theme-card";
    card.href = `/theme.html?id=${theme.id}`;
    card.innerHTML = `
      <div class="thumb" style="background-image: url('${escapeHtml(theme.thumbnailUrl)}');"></div>
      <div class="body">
        <div class="name">${escapeHtml(theme.name)}</div>
        <div class="desc">${escapeHtml(theme.description)}</div>
      </div>
    `;
    root.appendChild(card);
  });
}

/* ── Popular themes ── */
function renderPopular(themes) {
  const list = $("#popularThemes");
  list.innerHTML = "";

  if (!themes.length) {
    list.innerHTML = '<li><span class="rank-name muted">최근 7일 예약이 없습니다.</span></li>';
    return;
  }

  themes.forEach((theme) => {
    const li = document.createElement("li");
    li.innerHTML = `
      <span class="rank-name">
        <a href="/theme.html?id=${theme.id}" style="color: inherit;">${escapeHtml(theme.name)}</a>
      </span>
      <span class="rank-desc"> · ${escapeHtml(theme.description)}</span>
    `;
    list.appendChild(li);
  });
}

/* ── 컨텐츠 로드 (지점 선택 후) ── */
async function loadContent() {
  const store = Auth.getSelectedStore();
  const storeId = store?.id;

  try {
    const [allThemes, popular] = await Promise.all([
      api("/themes"),
      api("/themes/popular"),
    ]);

    // 선택 지점으로 필터
    const themes  = storeId ? allThemes.filter(t => t.storeId === storeId) : allThemes;
    const popFiltered = storeId ? popular.filter(t => t.storeId === storeId) : popular;

    renderThemes(themes);
    renderPopular(popFiltered);
  } catch (e) {
    setMessage(e.message, true);
  }
}

/* ── Init ── */
(async function init() {
  Auth.initNav('navActions', { showStoreBadge: true });
  await initStoreSelector();

  // 지점이 이미 선택돼 있으면 바로 컨텐츠 로드
  if (Auth.getSelectedStore()) {
    loadContent();
  }
})();

$("#refreshPopular").addEventListener("click", async () => {
  const store = Auth.getSelectedStore();
  const storeId = store?.id;
  try {
    const popular = await api("/themes/popular");
    const popFiltered = storeId ? popular.filter(t => t.storeId === storeId) : popular;
    renderPopular(popFiltered);
    setMessage("인기 테마를 갱신했습니다.");
  } catch (e) {
    setMessage(e.message, true);
  }
});
