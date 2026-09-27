import { api } from "../api/api.js";

export function initCargoModule(container, toastFn) {
  let cargoData = [];
  let shipmentsList = [];
  let pageNum = 0;
  let totalPages = 1;
  let currentSearch = "";

  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / CARGO MANAGEMENT</p>
        <h1>Cargo Management</h1>
        <p>Manage cargo manifest inventory, commodity types, weight measurements, and shipment assignments.</p>
      </div>
      <button class="button primary" id="btn-add-cargo">＋ Add Cargo</button>
    </section>

    <section class="stats-grid compact">
      <article class="stat-card">
        <div class="stat-icon purple">◈</div>
        <div><span>Cargo Items</span><strong id="stat-total-cargo">-</strong><small>Live count</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon teal">✓</div>
        <div><span>Total Weight</span><strong id="stat-total-weight">-</strong><small>Metric Tonnes</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon orange">◷</div>
        <div><span>API Status</span><strong>Connected</strong><small>Spring Boot / MySQL</small></div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>Cargo Manifest</h2>
          <p>Search and manage manifest cargo items.</p>
        </div>
        <div class="toolbar">
          <input class="table-search" id="cargo-search-input" placeholder="Search cargo..." aria-label="Search cargo">
          <button class="button" id="btn-refresh-cargo">↻ Refresh</button>
        </div>
      </div>
      <div class="table-wrap">
        <table id="cargo-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Description</th>
              <th>Commodity Type</th>
              <th>Quantity</th>
              <th>Weight (kg)</th>
              <th>Shipment Link</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="cargo-table-body">
            <tr><td colspan="7" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading cargo...</td></tr>
          </tbody>
        </table>
        <div class="empty-state" id="cargo-empty-state" hidden>No cargo records found.</div>
      </div>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-top:16px; font-size:12px; color:var(--muted);">
        <span id="cargo-page-info">Showing page 1</span>
        <div>
          <button class="button" id="btn-prev-page" disabled>Previous</button>
          <button class="button" id="btn-next-page" disabled style="margin-left:6px;">Next</button>
        </div>
      </div>
    </section>

    <!-- Modal for Create / Edit Cargo -->
    <div class="modal-overlay" id="cargo-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3 id="modal-title">Add New Cargo</h3>
          <button class="modal-close" id="modal-close-btn">&times;</button>
        </div>
        <div class="error-banner" id="modal-error" hidden></div>
        <form id="cargo-form">
          <input type="hidden" id="cargo-id">
          <div class="modal-body">
            <label class="field">
              <span>Description *</span>
              <input type="text" id="cargo-description" required placeholder="e.g. Industrial Electronics Parts">
            </label>
            <label class="field">
              <span>Parent Shipment *</span>
              <select id="cargo-shipment-id" required style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);"></select>
            </label>
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
              <label class="field">
                <span>Commodity / Type</span>
                <input type="text" id="cargo-type" placeholder="e.g. General Cargo, Perishable">
              </label>
              <label class="field">
                <span>Quantity *</span>
                <input type="number" id="cargo-quantity" value="1" min="1" required>
              </label>
            </div>
            <label class="field">
              <span>Total Weight (kg) *</span>
              <input type="number" step="any" id="cargo-weight" required min="0" value="5000">
            </label>
          </div>
          <div class="modal-footer">
            <button type="button" class="button" id="modal-cancel-btn">Cancel</button>
            <button type="submit" class="button primary" id="modal-submit-btn">Save Cargo</button>
          </div>
        </form>
      </div>
    </div>
  `;

  const tbody = container.querySelector("#cargo-table-body");
  const emptyState = container.querySelector("#cargo-empty-state");
  const modal = container.querySelector("#cargo-modal");
  const modalTitle = container.querySelector("#modal-title");
  const modalError = container.querySelector("#modal-error");
  const cargoForm = container.querySelector("#cargo-form");
  const cargoIdInput = container.querySelector("#cargo-id");
  const cargoDescInput = container.querySelector("#cargo-description");
  const cargoShipmentSelect = container.querySelector("#cargo-shipment-id");
  const cargoTypeInput = container.querySelector("#cargo-type");
  const cargoQuantityInput = container.querySelector("#cargo-quantity");
  const cargoWeightInput = container.querySelector("#cargo-weight");
  const submitBtn = container.querySelector("#modal-submit-btn");

  const statTotal = container.querySelector("#stat-total-cargo");
  const statWeight = container.querySelector("#stat-total-weight");
  const pageInfo = container.querySelector("#cargo-page-info");
  const prevBtn = container.querySelector("#btn-prev-page");
  const nextBtn = container.querySelector("#btn-next-page");

  async function loadShipmentsDropdown() {
    try {
      const res = await api.get("shipments", { size: 100 });
      shipmentsList = api.unwrap(res);
      cargoShipmentSelect.innerHTML = `<option value="">-- Select Shipment --</option>` +
        shipmentsList.map(s => `<option value="${s.id}">${escapeHtml(s.trackingNumber || s.referenceNumber)} (${s.origin} → ${s.destination})</option>`).join("");
    } catch (e) {
      console.warn("Could not load shipments dropdown for cargo", e);
    }
  }

  async function loadCargo(page = 0, search = currentSearch) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading cargo...</td></tr>`;
    emptyState.hidden = true;

    try {
      await loadShipmentsDropdown();
      const params = { page, size: 20 };
      if (search) params.search = search;

      const response = await api.get("cargo", params);
      cargoData = api.unwrap(response);
      pageNum = response.number !== undefined ? response.number : page;
      totalPages = response.totalPages || 1;

      renderTable(cargoData);
      updateStats(response.totalElements !== undefined ? response.totalElements : cargoData.length, cargoData);

      pageInfo.textContent = `Page ${pageNum + 1} of ${totalPages} (${response.totalElements !== undefined ? response.totalElements : cargoData.length} records)`;
      prevBtn.disabled = pageNum <= 0;
      nextBtn.disabled = pageNum >= totalPages - 1;
    } catch (error) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#e07b65; padding: 24px;">Failed to load cargo: ${escapeHtml(error.message)}</td></tr>`;
      toastFn(`Error: ${error.message}`);
    }
  }

  function updateStats(total, items) {
    statTotal.textContent = total;
    const totalKg = items.reduce((acc, c) => acc + (c.weightKg || 0), 0);
    statWeight.textContent = (totalKg / 1000).toFixed(1) + " MT";
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
          <td><strong>${escapeHtml(c.description)}</strong></td>
          <td><span class="status delivered">${escapeHtml(c.cargoType || "GENERAL")}</span></td>
          <td>${c.quantity || 1}</td>
          <td>${(c.weightKg || 0).toLocaleString()} kg</td>
          <td><small>${escapeHtml(shipmentRef)}</small></td>
          <td>
            <button class="table-action btn-edit-cargo" data-id="${c.id}">Edit</button>
            <button class="table-action danger btn-delete-cargo" data-id="${c.id}">Delete</button>
          </td>
        </tr>
      `;
    }).join("");

    tbody.querySelectorAll(".btn-edit-cargo").forEach(btn => {
      btn.addEventListener("click", () => openEditModal(btn.dataset.id));
    });

    tbody.querySelectorAll(".btn-delete-cargo").forEach(btn => {
      btn.addEventListener("click", () => handleDelete(btn.dataset.id));
    });
  }

  function openCreateModal() {
    cargoForm.reset();
    cargoIdInput.value = "";
    cargoQuantityInput.value = "1";
    cargoWeightInput.value = "5000";
    modalTitle.textContent = "Add New Cargo";
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function openEditModal(id) {
    const c = cargoData.find(item => String(item.id) === String(id));
    if (!c) return;

    cargoForm.reset();
    cargoIdInput.value = c.id;
    cargoDescInput.value = c.description || "";
    cargoShipmentSelect.value = c.shipment ? c.shipment.id : "";
    cargoTypeInput.value = c.cargoType || "";
    cargoQuantityInput.value = c.quantity || 1;
    cargoWeightInput.value = c.weightKg || 0;

    modalTitle.textContent = `Edit Cargo #${c.id} - ${c.description}`;
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function closeModal() { modal.classList.remove("show"); }

  async function handleFormSubmit(e) {
    e.preventDefault();
    modalError.hidden = true;

    const id = cargoIdInput.value;
    const shipmentId = cargoShipmentSelect.value;
    if (!shipmentId) {
      modalError.textContent = "Please select a parent shipment.";
      modalError.hidden = false;
      return;
    }

    const qty = parseInt(cargoQuantityInput.value || "1", 10);
    const weight = parseFloat(cargoWeightInput.value || "0");

    if (qty < 1) {
      modalError.textContent = "Quantity must be at least 1.";
      modalError.hidden = false;
      return;
    }

    if (weight < 0) {
      modalError.textContent = "Weight cannot be negative.";
      modalError.hidden = false;
      return;
    }

    const payload = {
      description: cargoDescInput.value.trim(),
      shipmentId: parseInt(shipmentId, 10),
      shipment: { id: parseInt(shipmentId, 10) },
      cargoType: cargoTypeInput.value.trim() || "GENERAL",
      quantity: qty,
      weightKg: weight
    };

    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="loading-spinner"></span> Saving...`;

    try {
      if (id) {
        await api.put(`cargo/${id}`, payload);
        toastFn(`Cargo updated successfully.`);
      } else {
        await api.post("cargo", payload);
        toastFn(`Cargo created successfully.`);
      }
      closeModal();
      loadCargo(pageNum, currentSearch);
    } catch (error) {
      modalError.textContent = error.message;
      modalError.hidden = false;
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Save Cargo";
    }
  }

  async function handleDelete(id) {
    const c = cargoData.find(item => String(item.id) === String(id));
    const name = c ? c.description : `#${id}`;

    if (!confirm(`Are you sure you want to delete Cargo "${name}"?`)) return;

    try {
      await api.delete(`cargo/${id}`);
      toastFn(`Cargo item deleted.`);
      loadCargo(pageNum, currentSearch);
    } catch (error) {
      alert(`Delete failed: ${error.message}`);
    }
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  container.querySelector("#btn-add-cargo").addEventListener("click", openCreateModal);
  container.querySelector("#btn-refresh-cargo").addEventListener("click", () => loadCargo(pageNum, currentSearch));
  container.querySelector("#modal-close-btn").addEventListener("click", closeModal);
  container.querySelector("#modal-cancel-btn").addEventListener("click", closeModal);
  cargoForm.addEventListener("submit", handleFormSubmit);

  prevBtn.addEventListener("click", () => { if (pageNum > 0) loadCargo(pageNum - 1, currentSearch); });
  nextBtn.addEventListener("click", () => { if (pageNum < totalPages - 1) loadCargo(pageNum + 1, currentSearch); });

  let searchTimeout = null;
  const searchInput = container.querySelector("#cargo-search-input");
  searchInput.addEventListener("input", () => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
      currentSearch = searchInput.value.trim();
      loadCargo(0, currentSearch);
    }, 300);
  });

  loadCargo(0);
}
