import { api } from "../api/api.js";

export function initShipmentsModule(container, toastFn) {
  let shipmentsData = [];
  let vesselsList = [];
  let pageNum = 0;
  let totalPages = 1;
  let currentSearch = "";
  let currentStatusFilter = "";

  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / SHIPMENT TRACKING</p>
        <h1>Shipment Tracking</h1>
        <p>Monitor international shipments, origin/destination routes, container payloads, and tracking history timeline.</p>
      </div>
      <button class="button primary" id="btn-add-shipment">＋ Add Shipment</button>
    </section>

    <section class="stats-grid compact">
      <article class="stat-card">
        <div class="stat-icon teal">⇄</div>
        <div><span>Active Shipments</span><strong id="stat-total-shipments">-</strong><small>Live count</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon blue">✓</div>
        <div><span>In Transit</span><strong id="stat-in-transit">-</strong><small>En route</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon orange">◷</div>
        <div><span>API Status</span><strong>Connected</strong><small>Spring Boot / MySQL</small></div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>Shipment Registry</h2>
          <p>Search, filter, and track shipment movements.</p>
        </div>
        <div class="toolbar">
          <input class="table-search" id="shipment-search-input" placeholder="Search shipments..." aria-label="Search shipments">
          <select id="shipment-status-filter" aria-label="Filter status" style="padding:9px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
            <option value="">All Statuses</option>
            <option value="REGISTERED">REGISTERED</option>
            <option value="IN_TRANSIT">IN_TRANSIT</option>
            <option value="CUSTOMS_HOLD">CUSTOMS_HOLD</option>
            <option value="DELIVERED">DELIVERED</option>
          </select>
          <button class="button" id="btn-refresh-shipments">↻ Refresh</button>
        </div>
      </div>
      <div class="table-wrap">
        <table id="shipments-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tracking / Ref</th>
              <th>Origin</th>
              <th>Destination</th>
              <th>Vessel</th>
              <th>Containers</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="shipments-table-body">
            <tr><td colspan="8" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading shipments...</td></tr>
          </tbody>
        </table>
        <div class="empty-state" id="shipments-empty-state" hidden>No shipment records found.</div>
      </div>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-top:16px; font-size:12px; color:var(--muted);">
        <span id="shipments-page-info">Showing page 1</span>
        <div>
          <button class="button" id="btn-prev-page" disabled>Previous</button>
          <button class="button" id="btn-next-page" disabled style="margin-left:6px;">Next</button>
        </div>
      </div>
    </section>

    <!-- Modal for Create / Edit Shipment -->
    <div class="modal-overlay" id="shipment-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3 id="modal-title">Add New Shipment</h3>
          <button class="modal-close" id="modal-close-btn">&times;</button>
        </div>
        <div class="error-banner" id="modal-error" hidden></div>
        <form id="shipment-form">
          <input type="hidden" id="shipment-id">
          <div class="modal-body">
            <label class="field">
              <span>Reference / Tracking Number *</span>
              <input type="text" id="shipment-ref" required placeholder="e.g. DPS-2026-0089">
            </label>
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
              <label class="field">
                <span>Origin Port / City *</span>
                <input type="text" id="shipment-origin" required placeholder="e.g. Shanghai">
              </label>
              <label class="field">
                <span>Destination Port / City *</span>
                <input type="text" id="shipment-destination" required placeholder="e.g. Rotterdam">
              </label>
            </div>
            <label class="field">
              <span>Vessel Assignment (Optional)</span>
              <select id="shipment-vessel-id" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);"></select>
            </label>
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
              <label class="field">
                <span>Container Count</span>
                <input type="number" id="shipment-containers" value="1" min="0">
              </label>
              <label class="field">
                <span>Status</span>
                <select id="shipment-status" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                  <option value="REGISTERED">REGISTERED</option>
                  <option value="IN_TRANSIT">IN_TRANSIT</option>
                  <option value="CUSTOMS_HOLD">CUSTOMS_HOLD</option>
                  <option value="DELIVERED">DELIVERED</option>
                </select>
              </label>
            </div>
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
              <label class="field">
                <span>Shipper Name</span>
                <input type="text" id="shipment-shipper" placeholder="e.g. Global Exports Ltd">
              </label>
              <label class="field">
                <span>Consignee Name</span>
                <input type="text" id="shipment-consignee" placeholder="e.g. Euro Imports GmbH">
              </label>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="button" id="modal-cancel-btn">Cancel</button>
            <button type="submit" class="button primary" id="modal-submit-btn">Save Shipment</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal for Shipment Tracking Timeline -->
    <div class="modal-overlay" id="tracking-modal">
      <div class="modal-card" style="max-width:540px;">
        <div class="modal-header">
          <h3 id="tracking-modal-title">Shipment Tracking Timeline</h3>
          <button class="modal-close" id="tracking-close-btn">&times;</button>
        </div>
        <div class="modal-body" id="tracking-timeline-body" style="padding:16px 0;">
          <p style="text-align:center; color:var(--muted);">Loading tracking history...</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="button" id="tracking-done-btn">Close</button>
        </div>
      </div>
    </div>
  `;

  const tbody = container.querySelector("#shipments-table-body");
  const emptyState = container.querySelector("#shipments-empty-state");
  const modal = container.querySelector("#shipment-modal");
  const modalTitle = container.querySelector("#modal-title");
  const modalError = container.querySelector("#modal-error");
  const shipmentForm = container.querySelector("#shipment-form");
  const shipmentIdInput = container.querySelector("#shipment-id");
  const shipmentRefInput = container.querySelector("#shipment-ref");
  const shipmentOriginInput = container.querySelector("#shipment-origin");
  const shipmentDestInput = container.querySelector("#shipment-destination");
  const shipmentVesselSelect = container.querySelector("#shipment-vessel-id");
  const shipmentContainersInput = container.querySelector("#shipment-containers");
  const shipmentStatusInput = container.querySelector("#shipment-status");
  const shipmentShipperInput = container.querySelector("#shipment-shipper");
  const shipmentConsigneeInput = container.querySelector("#shipment-consignee");
  const submitBtn = container.querySelector("#modal-submit-btn");

  const trackingModal = container.querySelector("#tracking-modal");
  const trackingTitle = container.querySelector("#tracking-modal-title");
  const trackingBody = container.querySelector("#tracking-timeline-body");

  const statTotal = container.querySelector("#stat-total-shipments");
  const statInTransit = container.querySelector("#stat-in-transit");
  const pageInfo = container.querySelector("#shipments-page-info");
  const prevBtn = container.querySelector("#btn-prev-page");
  const nextBtn = container.querySelector("#btn-next-page");
  const statusFilter = container.querySelector("#shipment-status-filter");

  async function loadVesselsDropdown() {
    try {
      const res = await api.get("vessels", { size: 100 });
      vesselsList = api.unwrap(res);
      shipmentVesselSelect.innerHTML = `<option value="">-- None (Unassigned) --</option>` +
        vesselsList.map(v => `<option value="${v.id}">${escapeHtml(v.name)} (${v.imoNumber})</option>`).join("");
    } catch (e) {
      console.warn("Could not load vessels dropdown for shipments", e);
    }
  }

  async function loadShipments(page = 0, search = currentSearch, status = currentStatusFilter) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading shipments...</td></tr>`;
    emptyState.hidden = true;

    try {
      await loadVesselsDropdown();
      const params = { page, size: 20 };
      if (search) params.search = search;
      if (status) params.status = status;

      const response = await api.get("shipments", params);
      shipmentsData = api.unwrap(response);
      pageNum = response.number !== undefined ? response.number : page;
      totalPages = response.totalPages || 1;

      renderTable(shipmentsData);
      updateStats(response.totalElements !== undefined ? response.totalElements : shipmentsData.length, shipmentsData);

      pageInfo.textContent = `Page ${pageNum + 1} of ${totalPages} (${response.totalElements !== undefined ? response.totalElements : shipmentsData.length} records)`;
      prevBtn.disabled = pageNum <= 0;
      nextBtn.disabled = pageNum >= totalPages - 1;
    } catch (error) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:#e07b65; padding: 24px;">Failed to load shipments: ${escapeHtml(error.message)}</td></tr>`;
      toastFn(`Error: ${error.message}`);
    }
  }

  function updateStats(total, items) {
    statTotal.textContent = total;
    statInTransit.textContent = items.filter(s => (s.status || "").toUpperCase() === "IN_TRANSIT").length;
  }

  function renderTable(items) {
    if (!items || items.length === 0) {
      tbody.innerHTML = "";
      emptyState.hidden = false;
      return;
    }

    emptyState.hidden = true;
    tbody.innerHTML = items.map(s => {
      const tracking = s.trackingNumber || s.referenceNumber || `#${s.id}`;
      const vesselName = s.vessel ? s.vessel.name : "Unassigned";
      let statusClass = "delivered";
      if (s.status === "IN_TRANSIT") statusClass = "";
      if (s.status === "CUSTOMS_HOLD") statusClass = "customs-hold";
      if (s.status === "REGISTERED") statusClass = "at-berth";

      return `
        <tr>
          <td><strong>#${s.id}</strong></td>
          <td><strong>${escapeHtml(tracking)}</strong></td>
          <td>${escapeHtml(s.origin)}</td>
          <td>${escapeHtml(s.destination)}</td>
          <td><small>${escapeHtml(vesselName)}</small></td>
          <td>${s.containerCount || 0}</td>
          <td><span class="status ${statusClass}">${escapeHtml(s.status || "REGISTERED")}</span></td>
          <td>
            <button class="table-action btn-track-shipment" data-id="${s.id}" data-ref="${escapeHtml(tracking)}">📍 History</button>
            <button class="table-action btn-edit-shipment" data-id="${s.id}">Edit</button>
            <button class="table-action danger btn-delete-shipment" data-id="${s.id}">Delete</button>
          </td>
        </tr>
      `;
    }).join("");

    tbody.querySelectorAll(".btn-track-shipment").forEach(btn => {
      btn.addEventListener("click", () => openTrackingModal(btn.dataset.id, btn.dataset.ref));
    });

    tbody.querySelectorAll(".btn-edit-shipment").forEach(btn => {
      btn.addEventListener("click", () => openEditModal(btn.dataset.id));
    });

    tbody.querySelectorAll(".btn-delete-shipment").forEach(btn => {
      btn.addEventListener("click", () => handleDelete(btn.dataset.id));
    });
  }

  async function openTrackingModal(id, ref) {
    trackingTitle.textContent = `Tracking History - ${ref}`;
    trackingBody.innerHTML = `<p style="text-align:center; padding: 20px;"><span class="loading-spinner"></span> Loading history...</p>`;
    trackingModal.classList.add("show");

    try {
      const history = await api.get(`shipments/${id}/tracking`);
      if (!history || history.length === 0) {
        trackingBody.innerHTML = `<p style="text-align:center; color:var(--muted); padding:20px;">No tracking history logged for this shipment.</p>`;
        return;
      }

      trackingBody.innerHTML = `
        <div style="border-left: 2px solid var(--accent); margin-left: 24px; padding-left: 16px;">
          ${history.map(item => `
            <div style="margin-bottom: 16px; position: relative;">
              <div style="position: absolute; left: -22px; top: 4px; width: 10px; height: 10px; border-radius: 50%; background: var(--accent);"></div>
              <div style="font-weight: 600; font-size: 13px;">${escapeHtml(item.action)}</div>
              <div style="font-size: 11px; color: var(--muted);">${new Date(item.createdAt).toLocaleString()}</div>
              <div style="font-size: 12px; margin-top: 4px; background: var(--surface-light, #1e2530); padding: 6px 10px; border-radius: 6px;"><code>${escapeHtml(item.details)}</code></div>
            </div>
          `).join("")}
        </div>
      `;
    } catch (err) {
      trackingBody.innerHTML = `<p style="text-align:center; color:#e07b65; padding:20px;">Failed to load history: ${escapeHtml(err.message)}</p>`;
    }
  }

  function openCreateModal() {
    shipmentForm.reset();
    shipmentIdInput.value = "";
    shipmentStatusInput.value = "REGISTERED";
    shipmentContainersInput.value = "1";
    shipmentShipperInput.value = "";
    shipmentConsigneeInput.value = "";
    modalTitle.textContent = "Add New Shipment";
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function openEditModal(id) {
    const s = shipmentsData.find(item => String(item.id) === String(id));
    if (!s) return;

    shipmentForm.reset();
    shipmentIdInput.value = s.id;
    shipmentRefInput.value = s.referenceNumber || s.trackingNumber || "";
    shipmentOriginInput.value = s.origin || "";
    shipmentDestInput.value = s.destination || "";
    shipmentVesselSelect.value = s.vessel ? s.vessel.id : "";
    shipmentContainersInput.value = s.containerCount || 1;
    shipmentStatusInput.value = s.status || "REGISTERED";
    shipmentShipperInput.value = s.shipper || "";
    shipmentConsigneeInput.value = s.consignee || "";

    modalTitle.textContent = `Edit Shipment #${s.id} - ${s.referenceNumber || s.trackingNumber}`;
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function closeModal() { modal.classList.remove("show"); }

  async function handleFormSubmit(e) {
    e.preventDefault();
    modalError.hidden = true;

    const id = shipmentIdInput.value;
    const vesselIdVal = shipmentVesselSelect.value;
    const payload = {
      referenceNumber: shipmentRefInput.value.trim(),
      trackingNumber: shipmentRefInput.value.trim(),
      origin: shipmentOriginInput.value.trim(),
      destination: shipmentDestInput.value.trim(),
      shipper: shipmentShipperInput.value.trim(),
      consignee: shipmentConsigneeInput.value.trim(),
      status: shipmentStatusInput.value,
      containerCount: parseInt(shipmentContainersInput.value || "1", 10),
      vesselId: vesselIdVal ? parseInt(vesselIdVal, 10) : null
    };

    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="loading-spinner"></span> Saving...`;

    try {
      if (id) {
        await api.put(`shipments/${id}`, payload);
        toastFn(`Shipment updated successfully.`);
      } else {
        await api.post("shipments", payload);
        toastFn(`Shipment created successfully.`);
      }
      closeModal();
      loadShipments(pageNum, currentSearch, currentStatusFilter);
    } catch (error) {
      modalError.textContent = error.message;
      modalError.hidden = false;
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Save Shipment";
    }
  }

  async function handleDelete(id) {
    const s = shipmentsData.find(item => String(item.id) === String(id));
    const name = s ? (s.trackingNumber || s.referenceNumber || `#${id}`) : `#${id}`;

    if (!confirm(`Are you sure you want to delete Shipment ${name}?`)) return;

    try {
      await api.delete(`shipments/${id}`);
      toastFn(`Shipment ${name} deleted.`);
      loadShipments(pageNum, currentSearch, currentStatusFilter);
    } catch (error) {
      if (error.status === 409) {
        alert(`Cannot delete Shipment ${name}: ${error.message}`);
      } else {
        alert(`Delete failed: ${error.message}`);
      }
    }
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  container.querySelector("#btn-add-shipment").addEventListener("click", openCreateModal);
  container.querySelector("#btn-refresh-shipments").addEventListener("click", () => loadShipments(pageNum, currentSearch, currentStatusFilter));
  container.querySelector("#modal-close-btn").addEventListener("click", closeModal);
  container.querySelector("#modal-cancel-btn").addEventListener("click", closeModal);
  shipmentForm.addEventListener("submit", handleFormSubmit);

  container.querySelector("#tracking-close-btn").addEventListener("click", () => trackingModal.classList.remove("show"));
  container.querySelector("#tracking-done-btn").addEventListener("click", () => trackingModal.classList.remove("show"));

  statusFilter.addEventListener("change", () => {
    currentStatusFilter = statusFilter.value;
    loadShipments(0, currentSearch, currentStatusFilter);
  });

  prevBtn.addEventListener("click", () => { if (pageNum > 0) loadShipments(pageNum - 1, currentSearch, currentStatusFilter); });
  nextBtn.addEventListener("click", () => { if (pageNum < totalPages - 1) loadShipments(pageNum + 1, currentSearch, currentStatusFilter); });

  let searchTimeout = null;
  const searchInput = container.querySelector("#shipment-search-input");
  searchInput.addEventListener("input", () => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
      currentSearch = searchInput.value.trim();
      loadShipments(0, currentSearch, currentStatusFilter);
    }, 300);
  });

  loadShipments(0);
}
