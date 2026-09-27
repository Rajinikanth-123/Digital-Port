import { api } from "../api/api.js";

export function initContainersModule(container, toastFn) {
  let containersData = [];
  let shipmentsList = [];
  let pageNum = 0;
  let totalPages = 1;
  let currentSearch = "";

  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / CONTAINER MANAGEMENT</p>
        <h1>Container Management</h1>
        <p>Track ISO container numbers, dimensions, payload weights, yard location, status, and shipment assignments.</p>
      </div>
      <button class="button primary" id="btn-add-container">＋ Add Container</button>
    </section>

    <section class="stats-grid compact">
      <article class="stat-card">
        <div class="stat-icon orange">▣</div>
        <div><span>Total Containers</span><strong id="stat-total-containers">-</strong><small>Live count</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon teal">✓</div>
        <div><span>Available</span><strong id="stat-available-containers">-</strong><small>Ready for loading</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon blue">◷</div>
        <div><span>API Status</span><strong>Connected</strong><small>Spring Boot / MySQL</small></div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>Container Inventory</h2>
          <p>Search, filter, and track container inventory.</p>
        </div>
        <div class="toolbar">
          <input class="table-search" id="container-search-input" placeholder="Search containers..." aria-label="Search containers">
          <button class="button" id="btn-refresh-containers">↻ Refresh</button>
        </div>
      </div>
      <div class="table-wrap">
        <table id="containers-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Container Number</th>
              <th>Size / Type</th>
              <th>Weight (kg)</th>
              <th>Yard Location</th>
              <th>Status</th>
              <th>Linked Shipment</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="containers-table-body">
            <tr><td colspan="8" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading containers...</td></tr>
          </tbody>
        </table>
        <div class="empty-state" id="containers-empty-state" hidden>No container records found.</div>
      </div>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-top:16px; font-size:12px; color:var(--muted);">
        <span id="containers-page-info">Showing page 1</span>
        <div>
          <button class="button" id="btn-prev-page" disabled>Previous</button>
          <button class="button" id="btn-next-page" disabled style="margin-left:6px;">Next</button>
        </div>
      </div>
    </section>

    <!-- Modal for Create / Edit Container -->
    <div class="modal-overlay" id="container-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3 id="modal-title">Add New Container</h3>
          <button class="modal-close" id="modal-close-btn">&times;</button>
        </div>
        <div class="error-banner" id="modal-error" hidden></div>
        <form id="container-form">
          <input type="hidden" id="container-id">
          <div class="modal-body">
            <label class="field">
              <span>Container Number (ISO 6346) *</span>
              <input type="text" id="container-number" required placeholder="e.g. MSKU9012345">
            </label>
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
              <label class="field">
                <span>Size</span>
                <select id="container-size" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                  <option value="20FT">20 FT Standard</option>
                  <option value="40FT">40 FT Standard</option>
                  <option value="40HC">40 FT High Cube</option>
                  <option value="45HC">45 FT High Cube</option>
                </select>
              </label>
              <label class="field">
                <span>Type</span>
                <select id="container-type" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                  <option value="DRY">Dry Storage</option>
                  <option value="REEFER">Refrigerated (Reefer)</option>
                  <option value="OPEN_TOP">Open Top</option>
                  <option value="FLAT_RACK">Flat Rack</option>
                  <option value="TANK">ISO Tank</option>
                </select>
              </label>
            </div>
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
              <label class="field">
                <span>Payload Weight (kg)</span>
                <input type="number" step="any" id="container-weight" min="0" value="0" placeholder="e.g. 14500">
              </label>
              <label class="field">
                <span>Yard Location</span>
                <input type="text" id="container-location" placeholder="e.g. Yard Block B-12">
              </label>
            </div>
            <label class="field">
              <span>Status</span>
              <select id="container-status" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                <option value="AVAILABLE">AVAILABLE</option>
                <option value="LOADED">LOADED</option>
                <option value="IN_TRANSIT">IN_TRANSIT</option>
                <option value="RELEASED">RELEASED</option>
                <option value="CUSTOMS_HOLD">CUSTOMS_HOLD</option>
                <option value="MAINTENANCE">MAINTENANCE</option>
              </select>
            </label>
            <label class="field">
              <span>Link to Shipment (Optional)</span>
              <select id="container-shipment-id" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                <option value="">-- None (Unassigned) --</option>
              </select>
            </label>
          </div>
          <div class="modal-footer">
            <button type="button" class="button" id="modal-cancel-btn">Cancel</button>
            <button type="submit" class="button primary" id="modal-submit-btn">Save Container</button>
          </div>
        </form>
      </div>
    </div>
  `;

  const tbody = container.querySelector("#containers-table-body");
  const emptyState = container.querySelector("#containers-empty-state");
  const modal = container.querySelector("#container-modal");
  const modalTitle = container.querySelector("#modal-title");
  const modalError = container.querySelector("#modal-error");
  const containerForm = container.querySelector("#container-form");
  const containerIdInput = container.querySelector("#container-id");
  const containerNumberInput = container.querySelector("#container-number");
  const containerSizeInput = container.querySelector("#container-size");
  const containerTypeInput = container.querySelector("#container-type");
  const containerWeightInput = container.querySelector("#container-weight");
  const containerLocationInput = container.querySelector("#container-location");
  const containerStatusInput = container.querySelector("#container-status");
  const containerShipmentSelect = container.querySelector("#container-shipment-id");
  const submitBtn = container.querySelector("#modal-submit-btn");

  const statTotal = container.querySelector("#stat-total-containers");
  const statAvail = container.querySelector("#stat-available-containers");
  const pageInfo = container.querySelector("#containers-page-info");
  const prevBtn = container.querySelector("#btn-prev-page");
  const nextBtn = container.querySelector("#btn-next-page");

  async function loadShipmentsDropdown() {
    try {
      const res = await api.get("shipments", { size: 100 });
      shipmentsList = api.unwrap(res);
      containerShipmentSelect.innerHTML = `<option value="">-- None (Unassigned) --</option>` +
        shipmentsList.map(s => `<option value="${s.id}">${escapeHtml(s.trackingNumber || s.referenceNumber)} (${s.origin} → ${s.destination})</option>`).join("");
    } catch (e) {
      console.warn("Could not load shipments dropdown", e);
    }
  }

  async function loadContainers(page = 0, search = currentSearch) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading containers...</td></tr>`;
    emptyState.hidden = true;

    try {
      await loadShipmentsDropdown();
      const params = { page, size: 20 };
      if (search) params.search = search;

      const response = await api.get("containers", params);
      containersData = api.unwrap(response);
      pageNum = response.number !== undefined ? response.number : page;
      totalPages = response.totalPages || 1;

      renderTable(containersData);
      updateStats(response.totalElements !== undefined ? response.totalElements : containersData.length, containersData);

      pageInfo.textContent = `Page ${pageNum + 1} of ${totalPages} (${response.totalElements !== undefined ? response.totalElements : containersData.length} records)`;
      prevBtn.disabled = pageNum <= 0;
      nextBtn.disabled = pageNum >= totalPages - 1;
    } catch (error) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:#e07b65; padding: 24px;">Failed to load containers: ${escapeHtml(error.message)}</td></tr>`;
      toastFn(`Error: ${error.message}`);
    }
  }

  function updateStats(total, items) {
    statTotal.textContent = total;
    statAvail.textContent = items.filter(c => (c.status || "").toUpperCase() === "AVAILABLE").length;
  }

  function renderTable(items) {
    if (!items || items.length === 0) {
      tbody.innerHTML = "";
      emptyState.hidden = false;
      return;
    }

    emptyState.hidden = true;
    tbody.innerHTML = items.map(c => {
      const shipmentRef = c.shipment ? (c.shipment.trackingNumber || c.shipment.referenceNumber || `#${c.shipment.id}`) : "Unassigned";
      return `
        <tr>
          <td><strong>#${c.id}</strong></td>
          <td><strong>${escapeHtml(c.containerNumber)}</strong></td>
          <td>${escapeHtml(c.size || "40FT")} / ${escapeHtml(c.type || "DRY")}</td>
          <td>${(c.weightKg || 0).toLocaleString()} kg</td>
          <td><small>${escapeHtml(c.location || "-")}</small></td>
          <td><span class="status ${c.status === "AVAILABLE" ? "delivered" : "customs-hold"}">${escapeHtml(c.status || "AVAILABLE")}</span></td>
          <td><small>${escapeHtml(shipmentRef)}</small></td>
          <td>
            <button class="table-action btn-edit-container" data-id="${c.id}">Edit</button>
            <button class="table-action danger btn-delete-container" data-id="${c.id}">Delete</button>
          </td>
        </tr>
      `;
    }).join("");

    tbody.querySelectorAll(".btn-edit-container").forEach(btn => {
      btn.addEventListener("click", () => openEditModal(btn.dataset.id));
    });

    tbody.querySelectorAll(".btn-delete-container").forEach(btn => {
      btn.addEventListener("click", () => handleDelete(btn.dataset.id));
    });
  }

  function openCreateModal() {
    containerForm.reset();
    containerIdInput.value = "";
    containerSizeInput.value = "40FT";
    containerTypeInput.value = "DRY";
    containerStatusInput.value = "AVAILABLE";
    containerWeightInput.value = "0";
    containerLocationInput.value = "";
    modalTitle.textContent = "Add New Container";
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function openEditModal(id) {
    const c = containersData.find(item => String(item.id) === String(id));
    if (!c) return;

    containerForm.reset();
    containerIdInput.value = c.id;
    containerNumberInput.value = c.containerNumber || "";
    containerSizeInput.value = c.size || "40FT";
    containerTypeInput.value = c.type || "DRY";
    containerWeightInput.value = c.weightKg !== null && c.weightKg !== undefined ? c.weightKg : 0;
    containerLocationInput.value = c.location || "";
    containerStatusInput.value = c.status || "AVAILABLE";
    containerShipmentSelect.value = c.shipment ? c.shipment.id : "";

    modalTitle.textContent = `Edit Container #${c.id} - ${c.containerNumber}`;
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function closeModal() { modal.classList.remove("show"); }

  async function handleFormSubmit(e) {
    e.preventDefault();
    modalError.hidden = true;

    const id = containerIdInput.value;
    const shipmentIdVal = containerShipmentSelect.value;
    const weightVal = parseFloat(containerWeightInput.value || "0");

    if (weightVal < 0) {
      modalError.textContent = "Payload weight cannot be negative.";
      modalError.hidden = false;
      return;
    }

    const payload = {
      containerNumber: containerNumberInput.value.trim().toUpperCase(),
      size: containerSizeInput.value,
      type: containerTypeInput.value,
      weightKg: weightVal,
      location: containerLocationInput.value.trim(),
      status: containerStatusInput.value,
      shipmentId: shipmentIdVal ? parseInt(shipmentIdVal, 10) : null
    };

    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="loading-spinner"></span> Saving...`;

    try {
      if (id) {
        await api.put(`containers/${id}`, payload);
        toastFn(`Container "${payload.containerNumber}" updated.`);
      } else {
        await api.post("containers", payload);
        toastFn(`Container "${payload.containerNumber}" created.`);
      }
      closeModal();
      loadContainers(pageNum, currentSearch);
    } catch (error) {
      modalError.textContent = error.message;
      modalError.hidden = false;
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Save Container";
    }
  }

  async function handleDelete(id) {
    const c = containersData.find(item => String(item.id) === String(id));
    const name = c ? c.containerNumber : `#${id}`;

    if (!confirm(`Are you sure you want to delete Container ${name}?`)) return;

    try {
      await api.delete(`containers/${id}`);
      toastFn(`Container ${name} deleted.`);
      loadContainers(pageNum, currentSearch);
    } catch (error) {
      alert(`Delete failed: ${error.message}`);
    }
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  container.querySelector("#btn-add-container").addEventListener("click", openCreateModal);
  container.querySelector("#btn-refresh-containers").addEventListener("click", () => loadContainers(pageNum, currentSearch));
  container.querySelector("#modal-close-btn").addEventListener("click", closeModal);
  container.querySelector("#modal-cancel-btn").addEventListener("click", closeModal);
  containerForm.addEventListener("submit", handleFormSubmit);

  prevBtn.addEventListener("click", () => { if (pageNum > 0) loadContainers(pageNum - 1, currentSearch); });
  nextBtn.addEventListener("click", () => { if (pageNum < totalPages - 1) loadContainers(pageNum + 1, currentSearch); });

  let searchTimeout = null;
  const searchInput = container.querySelector("#container-search-input");
  searchInput.addEventListener("input", () => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
      currentSearch = searchInput.value.trim();
      loadContainers(0, currentSearch);
    }, 300);
  });

  loadContainers(0);
}
