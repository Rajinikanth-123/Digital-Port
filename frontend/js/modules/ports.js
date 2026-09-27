import { api } from "../api/api.js";

export function initPortsModule(container, toastFn) {
  let portsData = [];
  let pageNum = 0;
  let totalPages = 1;
  let currentSearch = "";

  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / PORT MANAGEMENT</p>
        <h1>Port Management</h1>
        <p>Manage physical ports, UN/LOCODE identifiers, and geographic coordinates.</p>
      </div>
      <button class="button primary" id="btn-add-port">＋ Add Port</button>
    </section>

    <section class="stats-grid compact">
      <article class="stat-card">
        <div class="stat-icon teal">◈</div>
        <div><span>Total Ports</span><strong id="stat-total-ports">-</strong><small>Live count</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon blue">✓</div>
        <div><span>Active Countries</span><strong id="stat-active-countries">-</strong><small>Operating regions</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon orange">◷</div>
        <div><span>API Status</span><strong id="stat-api-status">Connected</strong><small>Spring Boot / MySQL</small></div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>Port Registry</h2>
          <p>Search and manage operating port locations.</p>
        </div>
        <div class="toolbar">
          <input class="table-search" id="port-search-input" placeholder="Search by name, code, country..." aria-label="Search ports">
          <button class="button" id="btn-refresh-ports">↻ Refresh</button>
        </div>
      </div>
      <div class="table-wrap">
        <table id="ports-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Port Name</th>
              <th>Code (UN/LOCODE)</th>
              <th>Country</th>
              <th>Timezone</th>
              <th>Coordinates</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="ports-table-body">
            <tr><td colspan="7" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading ports...</td></tr>
          </tbody>
        </table>
        <div class="empty-state" id="ports-empty-state" hidden>No port records found.</div>
      </div>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-top:16px; font-size:12px; color:var(--muted);">
        <span id="ports-page-info">Showing page 1</span>
        <div>
          <button class="button" id="btn-prev-page" disabled>Previous</button>
          <button class="button" id="btn-next-page" disabled style="margin-left:6px;">Next</button>
        </div>
      </div>
    </section>

    <!-- Modal for Create / Edit Port -->
    <div class="modal-overlay" id="port-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3 id="modal-title">Add New Port</h3>
          <button class="modal-close" id="modal-close-btn">&times;</button>
        </div>
        <div class="error-banner" id="modal-error" hidden></div>
        <form id="port-form">
          <input type="hidden" id="port-id">
          <div class="modal-body">
            <label class="field">
              <span>Port Name *</span>
              <input type="text" id="port-name" required placeholder="e.g. Port of Rotterdam">
            </label>
            <label class="field">
              <span>Port Code (UN/LOCODE) *</span>
              <input type="text" id="port-code" required placeholder="e.g. NLRTM">
            </label>
            <label class="field">
              <span>Country *</span>
              <input type="text" id="port-country" required placeholder="e.g. Netherlands">
            </label>
            <label class="field">
              <span>Timezone</span>
              <input type="text" id="port-timezone" value="UTC" placeholder="e.g. UTC, Europe/Amsterdam">
            </label>
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
              <label class="field">
                <span>Latitude</span>
                <input type="number" step="any" id="port-latitude" placeholder="e.g. 51.9244">
              </label>
              <label class="field">
                <span>Longitude</span>
                <input type="number" step="any" id="port-longitude" placeholder="e.g. 4.4777">
              </label>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="button" id="modal-cancel-btn">Cancel</button>
            <button type="submit" class="button primary" id="modal-submit-btn">Save Port</button>
          </div>
        </form>
      </div>
    </div>
  `;

  // DOM Elements
  const tbody = container.querySelector("#ports-table-body");
  const emptyState = container.querySelector("#ports-empty-state");
  const modal = container.querySelector("#port-modal");
  const modalTitle = container.querySelector("#modal-title");
  const modalError = container.querySelector("#modal-error");
  const portForm = container.querySelector("#port-form");
  const portIdInput = container.querySelector("#port-id");
  const portNameInput = container.querySelector("#port-name");
  const portCodeInput = container.querySelector("#port-code");
  const portCountryInput = container.querySelector("#port-country");
  const portTimezoneInput = container.querySelector("#port-timezone");
  const portLatInput = container.querySelector("#port-latitude");
  const portLngInput = container.querySelector("#port-longitude");
  const submitBtn = container.querySelector("#modal-submit-btn");

  const statTotal = container.querySelector("#stat-total-ports");
  const statCountries = container.querySelector("#stat-active-countries");
  const pageInfo = container.querySelector("#ports-page-info");
  const prevBtn = container.querySelector("#btn-prev-page");
  const nextBtn = container.querySelector("#btn-next-page");

  async function loadPorts(page = 0, search = currentSearch) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading ports...</td></tr>`;
    emptyState.hidden = true;

    try {
      const params = { page, size: 20 };
      if (search) params.search = search;

      const response = await api.get("ports", params);
      portsData = api.unwrap(response);
      pageNum = response.number !== undefined ? response.number : page;
      totalPages = response.totalPages || 1;

      renderTable(portsData);
      updateStats(response.totalElements !== undefined ? response.totalElements : portsData.length, portsData);

      pageInfo.textContent = `Page ${pageNum + 1} of ${totalPages} (${response.totalElements !== undefined ? response.totalElements : portsData.length} records)`;
      prevBtn.disabled = pageNum <= 0;
      nextBtn.disabled = pageNum >= totalPages - 1;
    } catch (error) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#e07b65; padding: 24px;">Failed to load ports: ${escapeHtml(error.message)}</td></tr>`;
      toastFn(`Error: ${error.message}`);
    }
  }

  function updateStats(total, items) {
    statTotal.textContent = total;
    const countries = new Set(items.map(p => p.country).filter(Boolean));
    statCountries.textContent = countries.size;
  }

  function renderTable(items) {
    if (!items || items.length === 0) {
      tbody.innerHTML = "";
      emptyState.hidden = false;
      return;
    }

    emptyState.hidden = true;
    tbody.innerHTML = items.map(port => `
      <tr>
        <td><strong>#${port.id}</strong></td>
        <td><strong>${escapeHtml(port.name)}</strong></td>
        <td><span class="status">${escapeHtml(port.code)}</span></td>
        <td>${escapeHtml(port.country)}</td>
        <td>${escapeHtml(port.timezone || "UTC")}</td>
        <td><small>${port.latitude !== null && port.latitude !== undefined ? port.latitude.toFixed(4) : "-"}, ${port.longitude !== null && port.longitude !== undefined ? port.longitude.toFixed(4) : "-"}</small></td>
        <td>
          <button class="table-action btn-edit-port" data-id="${port.id}">Edit</button>
          <button class="table-action danger btn-delete-port" data-id="${port.id}">Delete</button>
        </td>
      </tr>
    `).join("");

    tbody.querySelectorAll(".btn-edit-port").forEach(btn => {
      btn.addEventListener("click", () => openEditModal(btn.dataset.id));
    });

    tbody.querySelectorAll(".btn-delete-port").forEach(btn => {
      btn.addEventListener("click", () => handleDelete(btn.dataset.id));
    });
  }

  function openCreateModal() {
    portForm.reset();
    portIdInput.value = "";
    portTimezoneInput.value = "UTC";
    modalTitle.textContent = "Add New Port";
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function openEditModal(id) {
    const port = portsData.find(p => String(p.id) === String(id));
    if (!port) return;

    portForm.reset();
    portIdInput.value = port.id;
    portNameInput.value = port.name || "";
    portCodeInput.value = port.code || "";
    portCountryInput.value = port.country || "";
    portTimezoneInput.value = port.timezone || "UTC";
    portLatInput.value = port.latitude !== null && port.latitude !== undefined ? port.latitude : "";
    portLngInput.value = port.longitude !== null && port.longitude !== undefined ? port.longitude : "";

    modalTitle.textContent = `Edit Port #${port.id} - ${port.name}`;
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function closeModal() {
    modal.classList.remove("show");
  }

  async function handleFormSubmit(e) {
    e.preventDefault();
    modalError.hidden = true;

    const id = portIdInput.value;
    const payload = {
      name: portNameInput.value.trim(),
      code: portCodeInput.value.trim().toUpperCase(),
      country: portCountryInput.value.trim(),
      timezone: portTimezoneInput.value.trim() || "UTC",
      latitude: portLatInput.value ? parseFloat(portLatInput.value) : null,
      longitude: portLngInput.value ? parseFloat(portLngInput.value) : null
    };

    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="loading-spinner"></span> Saving...`;

    try {
      if (id) {
        await api.put(`ports/${id}`, payload);
        toastFn(`Port "${payload.name}" updated successfully.`);
      } else {
        await api.post("ports", payload);
        toastFn(`Port "${payload.name}" created successfully.`);
      }
      closeModal();
      loadPorts(pageNum, currentSearch);
    } catch (error) {
      modalError.textContent = error.message;
      modalError.hidden = false;
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Save Port";
    }
  }

  async function handleDelete(id) {
    const port = portsData.find(p => String(p.id) === String(id));
    const name = port ? `${port.name} (${port.code})` : `#${id}`;

    if (!confirm(`Are you sure you want to delete Port ${name}?`)) return;

    try {
      await api.delete(`ports/${id}`);
      toastFn(`Port ${name} deleted successfully.`);
      loadPorts(pageNum, currentSearch);
    } catch (error) {
      if (error.status === 409) {
        alert(`Cannot delete Port ${name}: ${error.message}`);
      } else {
        alert(`Delete failed: ${error.message}`);
      }
    }
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  container.querySelector("#btn-add-port").addEventListener("click", openCreateModal);
  container.querySelector("#btn-refresh-ports").addEventListener("click", () => loadPorts(pageNum, currentSearch));
  container.querySelector("#modal-close-btn").addEventListener("click", closeModal);
  container.querySelector("#modal-cancel-btn").addEventListener("click", closeModal);
  portForm.addEventListener("submit", handleFormSubmit);

  prevBtn.addEventListener("click", () => {
    if (pageNum > 0) loadPorts(pageNum - 1, currentSearch);
  });
  nextBtn.addEventListener("click", () => {
    if (pageNum < totalPages - 1) loadPorts(pageNum + 1, currentSearch);
  });

  let searchTimeout = null;
  const searchInput = container.querySelector("#port-search-input");
  searchInput.addEventListener("input", () => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
      currentSearch = searchInput.value.trim();
      loadPorts(0, currentSearch);
    }, 300);
  });

  loadPorts(0);
}
