import { api, escapeHtml } from "../api.js";

const RECENT_SEARCHES_KEY = "digital_port_recent_searches";

export function initSearchModule(container, toast) {
  const urlParams = new URLSearchParams(window.location.search);
  const initialQuery = urlParams.get("q") || "";

  container.innerHTML = `
    <div class="search-workspace">
      <div class="card" style="margin-bottom: 24px; padding: 24px;">
        <h2 style="margin: 0 0 16px 0; font-size: 20px; font-weight: 700; display: flex; align-items: center; gap: 10px;">
          <span>⌕</span> Advanced Global Search
        </h2>
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; align-items: end;">
          <div style="grid-column: span 2;">
            <label style="display: block; font-size: 12px; font-weight: 600; color: var(--muted); margin-bottom: 6px;">SEARCH QUERY</label>
            <div style="position: relative;">
              <input type="text" id="search-input-q" class="input" placeholder="Search tracking #, vessel, container, invoice, user..." value="${escapeHtml(initialQuery)}" style="width: 100%; font-size: 15px; padding-left: 36px;">
              <span style="position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: var(--muted);">⌕</span>
            </div>
          </div>
          <div>
            <label style="display: block; font-size: 12px; font-weight: 600; color: var(--muted); margin-bottom: 6px;">MODULE</label>
            <select id="search-select-module" class="input" style="width: 100%;">
              <option value="ALL">All Permitted Modules</option>
            </select>
          </div>
          <div>
            <label style="display: block; font-size: 12px; font-weight: 600; color: var(--muted); margin-bottom: 6px;">STATUS</label>
            <select id="search-select-status" class="input" style="width: 100%;">
              <option value="ALL">All Statuses</option>
            </select>
          </div>
          <div>
            <label style="display: block; font-size: 12px; font-weight: 600; color: var(--muted); margin-bottom: 6px;">START DATE</label>
            <input type="date" id="search-date-start" class="input" style="width: 100%;">
          </div>
          <div>
            <label style="display: block; font-size: 12px; font-weight: 600; color: var(--muted); margin-bottom: 6px;">END DATE</label>
            <input type="date" id="search-date-end" class="input" style="width: 100%;">
          </div>
          <div>
            <label style="display: block; font-size: 12px; font-weight: 600; color: var(--muted); margin-bottom: 6px;">SORT BY</label>
            <select id="search-select-sortby" class="input" style="width: 100%;">
              <option value="eventDate">Date (Latest First)</option>
              <option value="title">Display Title</option>
              <option value="module">Module Type</option>
              <option value="status">Status</option>
              <option value="id">Record ID</option>
            </select>
          </div>
          <div>
            <label style="display: block; font-size: 12px; font-weight: 600; color: var(--muted); margin-bottom: 6px;">SORT ORDER</label>
            <select id="search-select-sortdir" class="input" style="width: 100%;">
              <option value="desc">Descending ↓</option>
              <option value="asc">Ascending ↑</option>
            </select>
          </div>
          <div style="display: flex; gap: 10px;">
            <button id="btn-submit-search" class="button primary" style="flex: 1;">Search</button>
            <button id="btn-clear-search" class="button ghost" style="flex: 1;">Reset</button>
          </div>
        </div>

        <!-- Recent Searches Section -->
        <div id="recent-searches-container" style="margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--line); display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
          <span style="font-size: 12px; font-weight: 600; color: var(--muted);">RECENT SEARCHES:</span>
          <div id="recent-search-chips" style="display: flex; gap: 8px; flex-wrap: wrap;"></div>
          <button id="btn-clear-history" style="background: none; border: none; color: var(--muted); font-size: 11px; cursor: pointer; margin-left: auto;">Clear History</button>
        </div>
      </div>

      <!-- Facets / Module Counts Chips -->
      <div id="module-facets-bar" style="display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap;"></div>

      <!-- Search Results Table Card -->
      <div class="card" style="padding: 24px;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <h3 id="search-results-summary" style="margin: 0; font-size: 16px; font-weight: 600;">Results</h3>
          <span id="search-results-count" class="badge muted">0 records found</span>
        </div>

        <div id="search-results-table-wrapper" class="table-container">
          <p style="color: var(--muted); text-align: center; padding: 40px 0;">Enter a query or select filters to search workspace records.</p>
        </div>

        <!-- Pagination -->
        <div id="search-pagination" style="display: flex; justify-content: space-between; align-items: center; margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--line);">
          <span id="pagination-info" style="font-size: 13px; color: var(--muted);">Page 1 of 1</span>
          <div style="display: flex; gap: 8px;">
            <button id="btn-page-prev" class="button ghost" disabled>← Previous</button>
            <button id="btn-page-next" class="button ghost" disabled>Next →</button>
          </div>
        </div>
      </div>
    </div>
  `;

  let currentPage = 0;
  const pageSize = 15;

  loadFilterOptions();
  renderRecentSearches();

  if (initialQuery) {
    saveRecentSearch(initialQuery);
    executeSearch();
  }

  // Event Listeners
  document.getElementById("btn-submit-search").addEventListener("click", () => {
    currentPage = 0;
    const q = document.getElementById("search-input-q").value.trim();
    if (q) saveRecentSearch(q);
    executeSearch();
  });

  document.getElementById("search-input-q").addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      currentPage = 0;
      const q = e.target.value.trim();
      if (q) saveRecentSearch(q);
      executeSearch();
    }
  });

  document.getElementById("btn-clear-search").addEventListener("click", () => {
    document.getElementById("search-input-q").value = "";
    document.getElementById("search-select-module").value = "ALL";
    document.getElementById("search-select-status").value = "ALL";
    document.getElementById("search-date-start").value = "";
    document.getElementById("search-date-end").value = "";
    document.getElementById("search-select-sortby").value = "eventDate";
    document.getElementById("search-select-sortdir").value = "desc";
    currentPage = 0;
    executeSearch();
  });

  document.getElementById("btn-page-prev").addEventListener("click", () => {
    if (currentPage > 0) {
      currentPage--;
      executeSearch();
    }
  });

  document.getElementById("btn-page-next").addEventListener("click", () => {
    currentPage++;
    executeSearch();
  });

  document.getElementById("btn-clear-history").addEventListener("click", () => {
    localStorage.removeItem(RECENT_SEARCHES_KEY);
    renderRecentSearches();
    if (toast) toast.show("Search history cleared", "info");
  });

  async function loadFilterOptions() {
    try {
      const filters = await api.get("search/filters");
      if (!filters) return;

      const modSelect = document.getElementById("search-select-module");
      if (filters.availableModules && modSelect) {
        modSelect.innerHTML = '<option value="ALL">All Permitted Modules</option>' +
          filters.availableModules.map(m => `<option value="${m}">${m.replace("_", " ")}</option>`).join("");
      }

      const statusSelect = document.getElementById("search-select-status");
      if (filters.availableStatuses && statusSelect) {
        statusSelect.innerHTML = '<option value="ALL">All Statuses</option>' +
          filters.availableStatuses.map(s => `<option value="${s}">${s}</option>`).join("");
      }
    } catch (e) {
      console.error("Failed to load filter options", e);
    }
  }

  async function executeSearch() {
    const q = document.getElementById("search-input-q").value.trim();
    const module = document.getElementById("search-select-module").value;
    const status = document.getElementById("search-select-status").value;
    const startDate = document.getElementById("search-date-start").value;
    const endDate = document.getElementById("search-date-end").value;
    const sortBy = document.getElementById("search-select-sortby").value;
    const sortDir = document.getElementById("search-select-sortdir").value;

    const wrapper = document.getElementById("search-results-table-wrapper");
    wrapper.innerHTML = `<div style="text-align: center; padding: 40px;"><div class="spinner"></div><p style="margin-top: 12px; color: var(--muted);">Searching records...</p></div>`;

    const params = new URLSearchParams({
      q: q,
      module: module,
      status: status,
      startDate: startDate,
      endDate: endDate,
      page: currentPage,
      size: pageSize,
      sortBy: sortBy,
      sortDir: sortDir
    });

    try {
      const data = await api.get("search?" + params.toString());
      renderResults(data);
    } catch (e) {
      wrapper.innerHTML = `<div style="text-align: center; padding: 40px; color: #e07b65;">
        <p style="font-weight: 600;">Search Request Failed</p>
        <p style="font-size: 13px;">${escapeHtml(e.message || "Unable to complete search operation.")}</p>
        <button id="btn-retry-search" class="button ghost" style="margin-top: 12px;">Retry Search</button>
      </div>`;
      document.getElementById("btn-retry-search")?.addEventListener("click", executeSearch);
    }
  }

  function renderResults(data) {
    const wrapper = document.getElementById("search-results-table-wrapper");
    const countBadge = document.getElementById("search-results-count");
    const summaryHeading = document.getElementById("search-results-summary");
    const facetsBar = document.getElementById("module-facets-bar");

    countBadge.textContent = `${data.totalElements} records found`;
    summaryHeading.textContent = data.query ? `Results for "${data.query}"` : "All Permitted Records";

    // Render Module Facet Chips
    if (data.moduleCounts) {
      facetsBar.innerHTML = Object.entries(data.moduleCounts).map(([mod, count]) => {
        const isSelected = document.getElementById("search-select-module").value === mod;
        return `<button class="facet-chip ${isSelected ? 'active' : ''}" data-module="${mod}" style="padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 600; cursor: pointer; border: 1px solid var(--line); background: ${isSelected ? 'var(--accent)' : 'var(--surface-light)'}; color: ${isSelected ? '#fff' : 'var(--text)'};">
          ${mod.replace("_", " ")} <span style="opacity: 0.8; font-weight: 400;">(${count})</span>
        </button>`;
      }).join("");

      facetsBar.querySelectorAll(".facet-chip").forEach(btn => {
        btn.addEventListener("click", () => {
          const mod = btn.dataset.module;
          document.getElementById("search-select-module").value = mod;
          currentPage = 0;
          executeSearch();
        });
      });
    }

    if (!data.items || data.items.length === 0) {
      wrapper.innerHTML = `<div style="text-align: center; padding: 40px; color: var(--muted);">
        <p style="font-size: 24px; margin-bottom: 8px;">🔍</p>
        <p style="font-weight: 600; font-size: 16px;">No matching records found</p>
        <p style="font-size: 13px;">Try adjusting your search query, clearing date filters, or expanding module selections.</p>
      </div>`;
      updatePagination(0, 0);
      return;
    }

    const rows = data.items.map(item => {
      const moduleIcons = {
        SHIPMENT: "📦", VESSEL: "🚢", CONTAINER: "📥", PORT: "⚓", TERMINAL: "🏢",
        CUSTOMS: "📋", DOCUMENT: "📄", INVOICE: "💳", PAYMENT: "💵", USER: "👤"
      };
      const icon = moduleIcons[item.module] || "📑";
      const formattedDate = item.eventDate ? new Date(item.eventDate).toLocaleDateString("en-US", { year: 'numeric', month: 'short', day: 'numeric' }) : "N/A";
      const statusBadge = getStatusBadge(item.status);

      return `
        <tr>
          <td style="width: 50px; text-align: center; font-size: 18px;">${icon}</td>
          <td>
            <a href="${item.detailUrl}" style="font-weight: 600; color: var(--accent); text-decoration: none;" class="search-result-link">
              ${escapeHtml(item.displayTitle)}
            </a>
            <div style="font-size: 12px; color: var(--muted); margin-top: 2px;">Ref: ${escapeHtml(item.referenceNumber || item.recordId)}</div>
          </td>
          <td><span class="badge" style="background: var(--surface-light); border: 1px solid var(--line); font-size: 11px;">${item.module}</span></td>
          <td>${statusBadge}</td>
          <td style="font-size: 13px; color: var(--muted);">${formattedDate}</td>
          <td style="font-size: 13px; max-width: 300px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtml(item.summary || "")}</td>
          <td style="text-align: right;">
            <a href="${item.detailUrl}" class="button ghost small" style="font-size: 12px;">View →</a>
          </td>
        </tr>
      `;
    }).join("");

    wrapper.innerHTML = `
      <table class="data-table">
        <thead>
          <tr>
            <th></th>
            <th>Title & Reference</th>
            <th>Module</th>
            <th>Status</th>
            <th>Date</th>
            <th>Summary</th>
            <th style="text-align: right;">Action</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    `;

    updatePagination(data.currentPage, data.totalPages);
  }

  function updatePagination(current, total) {
    const prevBtn = document.getElementById("btn-page-prev");
    const nextBtn = document.getElementById("btn-page-next");
    const info = document.getElementById("pagination-info");

    info.textContent = `Page ${total > 0 ? current + 1 : 0} of ${total}`;
    prevBtn.disabled = current <= 0;
    nextBtn.disabled = current >= total - 1 || total === 0;
  }

  function getStatusBadge(status) {
    if (!status) return `<span class="badge muted">N/A</span>`;
    const s = status.toUpperCase();
    if (["DELIVERED", "APPROVED", "PAID", "ACTIVE", "SUCCESS"].includes(s)) {
      return `<span class="badge success" style="background: rgba(46,125,50,0.15); color: #4caf50; border: 1px solid rgba(76,175,80,0.3);">${s}</span>`;
    }
    if (["IN_TRANSIT", "ISSUED", "PARTIAL", "PENDING"].includes(s)) {
      return `<span class="badge warning" style="background: rgba(245,124,0,0.15); color: #ff9800; border: 1px solid rgba(255,152,0,0.3);">${s}</span>`;
    }
    if (["CANCELLED", "REJECTED", "OVERDUE", "INACTIVE", "FAILURE"].includes(s)) {
      return `<span class="badge danger" style="background: rgba(211,47,47,0.15); color: #f44336; border: 1px solid rgba(244,67,54,0.3);">${s}</span>`;
    }
    return `<span class="badge muted">${s}</span>`;
  }

  function renderRecentSearches() {
    const container = document.getElementById("recent-search-chips");
    const section = document.getElementById("recent-searches-container");
    const history = getRecentSearches();

    if (!history || history.length === 0) {
      section.style.display = "none";
      return;
    }

    section.style.display = "flex";
    container.innerHTML = history.map(term => `
      <span class="recent-chip" data-term="${escapeHtml(term)}" style="padding: 4px 10px; border-radius: 14px; background: var(--surface-light); border: 1px solid var(--line); font-size: 12px; cursor: pointer;">
        🕒 ${escapeHtml(term)}
      </span>
    `).join("");

    container.querySelectorAll(".recent-chip").forEach(chip => {
      chip.addEventListener("click", () => {
        const term = chip.dataset.term;
        document.getElementById("search-input-q").value = term;
        currentPage = 0;
        executeSearch();
      });
    });
  }

  function getRecentSearches() {
    try {
      return JSON.parse(localStorage.getItem(RECENT_SEARCHES_KEY)) || [];
    } catch (e) {
      return [];
    }
  }

  function saveRecentSearch(term) {
    if (!term || term.trim().length < 2) return;
    const clean = term.trim();
    let history = getRecentSearches();
    history = history.filter(t => t.toLowerCase() !== clean.toLowerCase());
    history.unshift(clean);
    if (history.length > 8) history = history.slice(0, 8);
    try {
      localStorage.setItem(RECENT_SEARCHES_KEY, JSON.stringify(history));
      renderRecentSearches();
    } catch (e) {}
  }
}
