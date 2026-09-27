import { api } from "../api/api.js";

export function initTerminalsModule(container, toastFn) {
  let terminalsData = [];
  let portsList = [];
  let pageNum = 0;
  let totalPages = 1;
  let currentSearch = "";

  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / TERMINAL MANAGEMENT</p>
        <h1>Terminal Management</h1>
        <p>Coordinate terminal capacity, operating types, and port assignments.</p>
      </div>
      <button class="button primary" id="btn-add-terminal">＋ Add Terminal</button>
    </section>

    <section class="stats-grid compact">
      <article class="stat-card">
        <div class="stat-icon blue">▤</div>
        <div><span>Total Terminals</span><strong id="stat-total-terminals">-</strong><small>Live count</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon teal">✓</div>
        <div><span>Total Capacity</span><strong id="stat-total-capacity">-</strong><small>TEUs / Units</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon orange">◷</div>
        <div><span>API Status</span><strong>Connected</strong><small>Spring Boot / MySQL</small></div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>Terminal Facilities</h2>
          <p>Search and manage terminal infrastructure per port.</p>
        </div>
        <div class="toolbar">
          <input class="table-search" id="terminal-search-input" placeholder="Search by name, code..." aria-label="Search terminals">
          <button class="button" id="btn-refresh-terminals">↻ Refresh</button>
        </div>
      </div>
      <div class="table-wrap">
        <table id="terminals-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Terminal Name</th>
              <th>Code</th>
              <th>Port</th>
              <th>Terminal Type</th>
              <th>Capacity</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="terminals-table-body">
            <tr><td colspan="7" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading terminals...</td></tr>
          </tbody>
        </table>
        <div class="empty-state" id="terminals-empty-state" hidden>No terminal records found.</div>
      </div>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-top:16px; font-size:12px; color:var(--muted);">
        <span id="terminals-page-info">Showing page 1</span>
        <div>
          <button class="button" id="btn-prev-page" disabled>Previous</button>
          <button class="button" id="btn-next-page" disabled style="margin-left:6px;">Next</button>
        </div>
      </div>
    </section>

    <!-- Modal for Create / Edit Terminal -->
    <div class="modal-overlay" id="terminal-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3 id="modal-title">Add New Terminal</h3>
          <button class="modal-close" id="modal-close-btn">&times;</button>
        </div>
        <div class="error-banner" id="modal-error" hidden></div>
        <form id="terminal-form">
          <input type="hidden" id="terminal-id">
          <div class="modal-body">
            <label class="field">
              <span>Terminal Name *</span>
              <input type="text" id="terminal-name" required placeholder="e.g. Container Terminal 1">
            </label>
            <label class="field">
              <span>Terminal Code *</span>
              <input type="text" id="terminal-code" required placeholder="e.g. CT1-RTM">
            </label>
            <label class="field">
              <span>Parent Port *</span>
              <select id="terminal-port" required style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                <option value="">-- Select Port --</option>
              </select>
            </label>
            <label class="field">
              <span>Terminal Type</span>
              <select id="terminal-type" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                <option value="CONTAINER">Container</option>
                <option value="BULK">Bulk</option>
                <option value="LIQUID">Liquid Cargo</option>
                <option value="MULTI_PURPOSE">Multi-Purpose</option>
              </select>
            </label>
            <label class="field">
              <span>Capacity (TEUs/Units)</span>
              <input type="number" id="terminal-capacity" value="50000" min="0" placeholder="e.g. 100000">
            </label>
          </div>
          <div class="modal-footer">
            <button type="button" class="button" id="modal-cancel-btn">Cancel</button>
            <button type="submit" class="button primary" id="modal-submit-btn">Save Terminal</button>
          </div>
        </form>
      </div>
    </div>
  `;

  const tbody = container.querySelector("#terminals-table-body");
  const emptyState = container.querySelector("#terminals-empty-state");
  const modal = container.querySelector("#terminal-modal");
  const modalTitle = container.querySelector("#modal-title");
  const modalError = container.querySelector("#modal-error");
  const terminalForm = container.querySelector("#terminal-form");
  const terminalIdInput = container.querySelector("#terminal-id");
  const terminalNameInput = container.querySelector("#terminal-name");
  const terminalCodeInput = container.querySelector("#terminal-code");
  const terminalPortInput = container.querySelector("#terminal-port");
  const terminalTypeInput = container.querySelector("#terminal-type");
  const terminalCapacityInput = container.querySelector("#terminal-capacity");
  const submitBtn = container.querySelector("#modal-submit-btn");

  const statTotal = container.querySelector("#stat-total-terminals");
  const statCapacity = container.querySelector("#stat-total-capacity");
  const pageInfo = container.querySelector("#terminals-page-info");
  const prevBtn = container.querySelector("#btn-prev-page");
  const nextBtn = container.querySelector("#btn-next-page");

  async function loadPortsDropdown() {
    try {
      const res = await api.get("ports", { size: 100 });
      portsList = api.unwrap(res);
      terminalPortInput.innerHTML = `<option value="">-- Select Port --</option>` +
        portsList.map(p => `<option value="${p.id}">${escapeHtml(p.name)} (${p.code})</option>`).join("");
    } catch (e) {
      console.warn("Could not load ports dropdown", e);
    }
  }

  async function loadTerminals(page = 0, search = currentSearch) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading terminals...</td></tr>`;
    emptyState.hidden = true;

    try {
      await loadPortsDropdown();
      const params = { page, size: 20 };
      if (search) params.search = search;

      const response = await api.get("terminals", params);
      terminalsData = api.unwrap(response);
      pageNum = response.number !== undefined ? response.number : page;
      totalPages = response.totalPages || 1;

      renderTable(terminalsData);
      updateStats(response.totalElements !== undefined ? response.totalElements : terminalsData.length, terminalsData);

      pageInfo.textContent = `Page ${pageNum + 1} of ${totalPages} (${response.totalElements !== undefined ? response.totalElements : terminalsData.length} records)`;
      prevBtn.disabled = pageNum <= 0;
      nextBtn.disabled = pageNum >= totalPages - 1;
    } catch (error) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#e07b65; padding: 24px;">Failed to load terminals: ${escapeHtml(error.message)}</td></tr>`;
      toastFn(`Error: ${error.message}`);
    }
  }

  function updateStats(total, items) {
    statTotal.textContent = total;
    const sum = items.reduce((acc, t) => acc + (t.capacity || 0), 0);
    statCapacity.textContent = sum.toLocaleString();
  }

  function renderTable(items) {
    if (!items || items.length === 0) {
      tbody.innerHTML = "";
      emptyState.hidden = false;
      return;
    }

    emptyState.hidden = true;
    tbody.innerHTML = items.map(t => {
      const portName = t.port ? t.port.name : "Unassigned";
      return `
        <tr>
          <td><strong>#${t.id}</strong></td>
          <td><strong>${escapeHtml(t.name)}</strong></td>
          <td><span class="status delivered">${escapeHtml(t.code)}</span></td>
          <td>${escapeHtml(portName)}</td>
          <td>${escapeHtml(t.terminalType || "CONTAINER")}</td>
          <td>${(t.capacity || 0).toLocaleString()}</td>
          <td>
            <button class="table-action btn-edit-terminal" data-id="${t.id}">Edit</button>
            <button class="table-action danger btn-delete-terminal" data-id="${t.id}">Delete</button>
          </td>
        </tr>
      `;
    }).join("");

    tbody.querySelectorAll(".btn-edit-terminal").forEach(btn => {
      btn.addEventListener("click", () => openEditModal(btn.dataset.id));
    });

    tbody.querySelectorAll(".btn-delete-terminal").forEach(btn => {
      btn.addEventListener("click", () => handleDelete(btn.dataset.id));
    });
  }

  function openCreateModal() {
    terminalForm.reset();
    terminalIdInput.value = "";
    terminalTypeInput.value = "CONTAINER";
    terminalCapacityInput.value = "50000";
    modalTitle.textContent = "Add New Terminal";
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function openEditModal(id) {
    const t = terminalsData.find(item => String(item.id) === String(id));
    if (!t) return;

    terminalForm.reset();
    terminalIdInput.value = t.id;
    terminalNameInput.value = t.name || "";
    terminalCodeInput.value = t.code || "";
    terminalPortInput.value = t.port ? t.port.id : "";
    terminalTypeInput.value = t.terminalType || "CONTAINER";
    terminalCapacityInput.value = t.capacity !== null ? t.capacity : "0";

    modalTitle.textContent = `Edit Terminal #${t.id} - ${t.name}`;
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function closeModal() { modal.classList.remove("show"); }

  async function handleFormSubmit(e) {
    e.preventDefault();
    modalError.hidden = true;

    const id = terminalIdInput.value;
    const portId = terminalPortInput.value;
    if (!portId) {
      modalError.textContent = "Please select a parent port.";
      modalError.hidden = false;
      return;
    }

    const payload = {
      name: terminalNameInput.value.trim(),
      code: terminalCodeInput.value.trim().toUpperCase(),
      portId: parseInt(portId, 10),
      terminalType: terminalTypeInput.value,
      capacity: parseInt(terminalCapacityInput.value || "0", 10)
    };

    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="loading-spinner"></span> Saving...`;

    try {
      if (id) {
        await api.put(`terminals/${id}`, payload);
        toastFn(`Terminal "${payload.name}" updated successfully.`);
      } else {
        await api.post("terminals", payload);
        toastFn(`Terminal "${payload.name}" created successfully.`);
      }
      closeModal();
      loadTerminals(pageNum, currentSearch);
    } catch (error) {
      modalError.textContent = error.message;
      modalError.hidden = false;
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Save Terminal";
    }
  }

  async function handleDelete(id) {
    const t = terminalsData.find(item => String(item.id) === String(id));
    const name = t ? `${t.name} (${t.code})` : `#${id}`;

    if (!confirm(`Are you sure you want to delete Terminal ${name}?`)) return;

    try {
      await api.delete(`terminals/${id}`);
      toastFn(`Terminal ${name} deleted successfully.`);
      loadTerminals(pageNum, currentSearch);
    } catch (error) {
      if (error.status === 409) {
        alert(`Cannot delete Terminal ${name}: ${error.message}`);
      } else {
        alert(`Delete failed: ${error.message}`);
      }
    }
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  container.querySelector("#btn-add-terminal").addEventListener("click", openCreateModal);
  container.querySelector("#btn-refresh-terminals").addEventListener("click", () => loadTerminals(pageNum, currentSearch));
  container.querySelector("#modal-close-btn").addEventListener("click", closeModal);
  container.querySelector("#modal-cancel-btn").addEventListener("click", closeModal);
  terminalForm.addEventListener("submit", handleFormSubmit);

  prevBtn.addEventListener("click", () => { if (pageNum > 0) loadTerminals(pageNum - 1, currentSearch); });
  nextBtn.addEventListener("click", () => { if (pageNum < totalPages - 1) loadTerminals(pageNum + 1, currentSearch); });

  let searchTimeout = null;
  const searchInput = container.querySelector("#terminal-search-input");
  searchInput.addEventListener("input", () => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
      currentSearch = searchInput.value.trim();
      loadTerminals(0, currentSearch);
    }, 300);
  });

  loadTerminals(0);
}
