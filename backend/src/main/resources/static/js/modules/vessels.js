import { api } from "../api/api.js";

export function initVesselsModule(container, toastFn) {
  let vesselsData = [];
  let pageNum = 0;
  let totalPages = 1;
  let currentSearch = "";

  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / VESSEL MANAGEMENT & TRACKING</p>
        <h1>Vessel Management & Location Tracking</h1>
        <p>Maintain vessel fleet registry, record position updates (Schedule-Based / Manual Update), and inspect historical position logs.</p>
      </div>
      <button class="button primary" id="btn-add-vessel">＋ Add Vessel</button>
    </section>

    <section class="stats-grid compact">
      <article class="stat-card">
        <div class="stat-icon purple">◉</div>
        <div><span>Registered Vessels</span><strong id="stat-total-vessels">-</strong><small>Fleet size</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon blue">✓</div>
        <div><span>Active Fleet</span><strong id="stat-active-vessels">-</strong><small>In service</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon teal">📍</div>
        <div><span>Tracking Provider</span><strong>Manual / Schedule</strong><small>Verified coordinates</small></div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>Vessel Fleet Registry</h2>
          <p>Search, update coordinates, and track vessel movements.</p>
        </div>
        <div class="toolbar">
          <input class="table-search" id="vessel-search-input" placeholder="Search by name, IMO, type..." aria-label="Search vessels">
          <button class="button" id="btn-refresh-vessels">↻ Refresh</button>
        </div>
      </div>
      <div class="table-wrap">
        <table id="vessels-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Vessel Name</th>
              <th>IMO Number</th>
              <th>Type</th>
              <th>Flag / Operator</th>
              <th>Location / Coordinates</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="vessels-table-body">
            <tr><td colspan="8" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading vessels...</td></tr>
          </tbody>
        </table>
        <div class="empty-state" id="vessels-empty-state" hidden>No vessel records found.</div>
      </div>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-top:16px; font-size:12px; color:var(--muted);">
        <span id="vessels-page-info">Showing page 1</span>
        <div>
          <button class="button" id="btn-prev-page" disabled>Previous</button>
          <button class="button" id="btn-next-page" disabled style="margin-left:6px;">Next</button>
        </div>
      </div>
    </section>

    <!-- Modal for Create / Edit Vessel -->
    <div class="modal-overlay" id="vessel-modal">
      <div class="modal-card" style="max-width:640px;">
        <div class="modal-header">
          <h3 id="modal-title">Add New Vessel</h3>
          <button class="modal-close" id="modal-close-btn">&times;</button>
        </div>
        <div class="error-banner" id="modal-error" hidden></div>
        <form id="vessel-form">
          <input type="hidden" id="vessel-id">
          <div class="modal-body" style="display:grid; grid-template-columns:1fr 1fr; gap:12px;">
            <label class="field" style="grid-column: span 2;">
              <span>Vessel Name *</span>
              <input type="text" id="vessel-name" required placeholder="e.g. MV Ocean Meridian">
            </label>
            <label class="field">
              <span>IMO Number *</span>
              <input type="text" id="vessel-imo" required placeholder="e.g. IMO 9784321">
            </label>
            <label class="field">
              <span>Vessel Type</span>
              <select id="vessel-type" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                <option value="CONTAINER">Container Ship</option>
                <option value="BULK_CARRIER">Bulk Carrier</option>
                <option value="TANKER">Tanker</option>
                <option value="CARGO">General Cargo</option>
                <option value="RO_RO">Ro-Ro Passenger</option>
              </select>
            </label>
            <label class="field">
              <span>Flag</span>
              <input type="text" id="vessel-flag" placeholder="e.g. Panama, Marshall Islands">
            </label>
            <label class="field">
              <span>Operator Name</span>
              <input type="text" id="vessel-operator" placeholder="e.g. Maersk, MSC">
            </label>
            <label class="field">
              <span>Capacity (TEUs / Tons)</span>
              <input type="number" id="vessel-capacity" min="0" value="10000" placeholder="e.g. 18000">
            </label>
            <label class="field">
              <span>Status</span>
              <select id="vessel-status" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                <option value="ACTIVE">ACTIVE</option>
                <option value="IN_TRANSIT">IN TRANSIT</option>
                <option value="AT_BERTH">AT BERTH</option>
                <option value="MAINTENANCE">MAINTENANCE</option>
                <option value="INACTIVE">INACTIVE</option>
              </select>
            </label>
            <label class="field">
              <span>Length (meters)</span>
              <input type="number" step="any" id="vessel-length" placeholder="e.g. 399.9">
            </label>
            <label class="field">
              <span>Draft (meters)</span>
              <input type="number" step="any" id="vessel-draft" placeholder="e.g. 16.0">
            </label>
          </div>
          <div class="modal-footer">
            <button type="button" class="button" id="modal-cancel-btn">Cancel</button>
            <button type="submit" class="button primary" id="modal-submit-btn">Save Vessel</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal for Position / Location Tracking Update -->
    <div class="modal-overlay" id="tracking-update-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3 id="tracking-modal-title">Update Vessel Location</h3>
          <button class="modal-close" id="tracking-modal-close-btn">&times;</button>
        </div>
        <div style="background:rgba(0,180,216,0.08); color:var(--accent); padding:10px 14px; border-radius:8px; font-size:12px; margin-bottom:12px;">
          ℹ <b>Notice:</b> Tracking is <b>Schedule-Based / Manually Updated</b>. Please enter validated coordinates (-90° to 90° Lat, -180° to 180° Lng).
        </div>
        <div class="error-banner" id="tracking-modal-error" hidden></div>
        <form id="tracking-form">
          <input type="hidden" id="tracking-vessel-id">
          <div class="modal-body">
            <label class="field">
              <span>Current Port / Sea Location</span>
              <input type="text" id="tracking-location" placeholder="e.g. North Sea, Approach Channel, Pier 4">
            </label>
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
              <label class="field">
                <span>Latitude (-90 to 90)</span>
                <input type="number" step="any" id="tracking-lat" min="-90" max="90" placeholder="e.g. 51.9546">
              </label>
              <label class="field">
                <span>Longitude (-180 to 180)</span>
                <input type="number" step="any" id="tracking-lng" min="-180" max="180" placeholder="e.g. 4.0205">
              </label>
            </div>
            <label class="field">
              <span>Vessel Status</span>
              <select id="tracking-status" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                <option value="ACTIVE">ACTIVE</option>
                <option value="IN_TRANSIT">IN TRANSIT</option>
                <option value="AT_BERTH">AT BERTH</option>
                <option value="MAINTENANCE">MAINTENANCE</option>
                <option value="INACTIVE">INACTIVE</option>
              </select>
            </label>
          </div>
          <div class="modal-footer">
            <button type="button" class="button" id="tracking-cancel-btn">Cancel</button>
            <button type="submit" class="button primary" id="tracking-submit-btn">Update Position</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal for Tracking History Timeline -->
    <div class="modal-overlay" id="history-modal">
      <div class="modal-card" style="max-width:540px;">
        <div class="modal-header">
          <h3 id="history-modal-title">Vessel Tracking History</h3>
          <button class="modal-close" id="history-close-btn">&times;</button>
        </div>
        <div class="modal-body" id="history-timeline-body" style="padding:16px 0;">
          <p style="text-align:center; color:var(--muted);">Loading tracking history...</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="button" id="history-done-btn">Close</button>
        </div>
      </div>
    </div>
  `;

  const tbody = container.querySelector("#vessels-table-body");
  const emptyState = container.querySelector("#vessels-empty-state");
  const modal = container.querySelector("#vessel-modal");
  const modalTitle = container.querySelector("#modal-title");
  const modalError = container.querySelector("#modal-error");
  const vesselForm = container.querySelector("#vessel-form");
  const vesselIdInput = container.querySelector("#vessel-id");
  const vesselNameInput = container.querySelector("#vessel-name");
  const vesselImoInput = container.querySelector("#vessel-imo");
  const vesselTypeInput = container.querySelector("#vessel-type");
  const vesselFlagInput = container.querySelector("#vessel-flag");
  const vesselOperatorInput = container.querySelector("#vessel-operator");
  const vesselCapacityInput = container.querySelector("#vessel-capacity");
  const vesselStatusInput = container.querySelector("#vessel-status");
  const vesselLengthInput = container.querySelector("#vessel-length");
  const vesselDraftInput = container.querySelector("#vessel-draft");
  const submitBtn = container.querySelector("#modal-submit-btn");

  const trackingModal = container.querySelector("#tracking-update-modal");
  const trackingForm = container.querySelector("#tracking-form");
  const trackingVesselIdInput = container.querySelector("#tracking-vessel-id");
  const trackingLocationInput = container.querySelector("#tracking-location");
  const trackingLatInput = container.querySelector("#tracking-lat");
  const trackingLngInput = container.querySelector("#tracking-lng");
  const trackingStatusInput = container.querySelector("#tracking-status");
  const trackingError = container.querySelector("#tracking-modal-error");
  const trackingSubmitBtn = container.querySelector("#tracking-submit-btn");

  const historyModal = container.querySelector("#history-modal");
  const historyTitle = container.querySelector("#history-modal-title");
  const historyBody = container.querySelector("#history-timeline-body");

  const statTotal = container.querySelector("#stat-total-vessels");
  const statActive = container.querySelector("#stat-active-vessels");
  const pageInfo = container.querySelector("#vessels-page-info");
  const prevBtn = container.querySelector("#btn-prev-page");
  const nextBtn = container.querySelector("#btn-next-page");

  async function loadVessels(page = 0, search = currentSearch) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading vessels...</td></tr>`;
    emptyState.hidden = true;

    try {
      const params = { page, size: 20 };
      if (search) params.search = search;

      const response = await api.get("vessels", params);
      vesselsData = api.unwrap(response);
      pageNum = response.number !== undefined ? response.number : page;
      totalPages = response.totalPages || 1;

      renderTable(vesselsData);
      updateStats(response.totalElements !== undefined ? response.totalElements : vesselsData.length, vesselsData);

      pageInfo.textContent = `Page ${pageNum + 1} of ${totalPages} (${response.totalElements !== undefined ? response.totalElements : vesselsData.length} records)`;
      prevBtn.disabled = pageNum <= 0;
      nextBtn.disabled = pageNum >= totalPages - 1;
    } catch (error) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:#e07b65; padding: 24px;">Failed to load vessels: ${escapeHtml(error.message)}</td></tr>`;
      toastFn(`Error: ${error.message}`);
    }
  }

  function updateStats(total, items) {
    statTotal.textContent = total;
    statActive.textContent = items.filter(v => (v.status || "ACTIVE").toUpperCase() === "ACTIVE").length;
  }

  function renderTable(items) {
    if (!items || items.length === 0) {
      tbody.innerHTML = "";
      emptyState.hidden = false;
      return;
    }

    emptyState.hidden = true;
    tbody.innerHTML = items.map(v => {
      const locStr = v.currentLocation || "Sea Channel";
      const coordsStr = (v.latitude !== null && v.latitude !== undefined && v.longitude !== null && v.longitude !== undefined)
        ? `<small>(${v.latitude.toFixed(4)}, ${v.longitude.toFixed(4)})</small>`
        : `<small style="color:var(--muted);">(No GPS coords)</small>`;

      let statusClass = "delivered";
      if (v.status === "IN_TRANSIT") statusClass = "";
      if (v.status === "AT_BERTH") statusClass = "at-berth";
      if (v.status === "MAINTENANCE") statusClass = "customs-hold";

      return `
        <tr>
          <td><strong>#${v.id}</strong></td>
          <td><strong>${escapeHtml(v.name)}</strong></td>
          <td><span class="status at-berth">${escapeHtml(v.imoNumber)}</span></td>
          <td>${escapeHtml(v.vesselType || "CONTAINER")}</td>
          <td><small>${escapeHtml(v.flag || "-")} / ${escapeHtml(v.operatorName || "-")}</small></td>
          <td>${escapeHtml(locStr)}<br>${coordsStr}</td>
          <td><span class="status ${statusClass}">${escapeHtml(v.status || "ACTIVE")}</span></td>
          <td>
            <button class="table-action btn-track-vessel" data-id="${v.id}">📍 Track</button>
            <button class="table-action btn-history-vessel" data-id="${v.id}" data-name="${escapeHtml(v.name)}">📜 History</button>
            <button class="table-action btn-edit-vessel" data-id="${v.id}">Edit</button>
            <button class="table-action danger btn-delete-vessel" data-id="${v.id}">Delete</button>
          </td>
        </tr>
      `;
    }).join("");

    tbody.querySelectorAll(".btn-track-vessel").forEach(btn => {
      btn.addEventListener("click", () => openTrackingUpdateModal(btn.dataset.id));
    });

    tbody.querySelectorAll(".btn-history-vessel").forEach(btn => {
      btn.addEventListener("click", () => openHistoryModal(btn.dataset.id, btn.dataset.name));
    });

    tbody.querySelectorAll(".btn-edit-vessel").forEach(btn => {
      btn.addEventListener("click", () => openEditModal(btn.dataset.id));
    });

    tbody.querySelectorAll(".btn-delete-vessel").forEach(btn => {
      btn.addEventListener("click", () => handleDelete(btn.dataset.id));
    });
  }

  function openTrackingUpdateModal(id) {
    const v = vesselsData.find(item => String(item.id) === String(id));
    if (!v) return;

    trackingForm.reset();
    trackingVesselIdInput.value = v.id;
    trackingLocationInput.value = v.currentLocation || "";
    trackingLatInput.value = v.latitude !== null && v.latitude !== undefined ? v.latitude : "";
    trackingLngInput.value = v.longitude !== null && v.longitude !== undefined ? v.longitude : "";
    trackingStatusInput.value = v.status || "ACTIVE";

    container.querySelector("#tracking-modal-title").textContent = `Update Position: ${v.name} (${v.imoNumber})`;
    trackingError.hidden = true;
    trackingError.textContent = "";
    trackingModal.classList.add("show");
  }

  async function openHistoryModal(id, name) {
    historyTitle.textContent = `Tracking History - ${name}`;
    historyBody.innerHTML = `<p style="text-align:center; padding: 20px;"><span class="loading-spinner"></span> Loading history...</p>`;
    historyModal.classList.add("show");

    try {
      const history = await api.get(`vessels/${id}/tracking`);
      if (!history || history.length === 0) {
        historyBody.innerHTML = `<p style="text-align:center; color:var(--muted); padding:20px;">No historical tracking events logged for this vessel.</p>`;
        return;
      }

      historyBody.innerHTML = `
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
      historyBody.innerHTML = `<p style="text-align:center; color:#e07b65; padding:20px;">Failed to load history: ${escapeHtml(err.message)}</p>`;
    }
  }

  function openCreateModal() {
    vesselForm.reset();
    vesselIdInput.value = "";
    vesselTypeInput.value = "CONTAINER";
    vesselStatusInput.value = "ACTIVE";
    vesselCapacityInput.value = "10000";
    modalTitle.textContent = "Add New Vessel";
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function openEditModal(id) {
    const v = vesselsData.find(item => String(item.id) === String(id));
    if (!v) return;

    vesselForm.reset();
    vesselIdInput.value = v.id;
    vesselNameInput.value = v.name || "";
    vesselImoInput.value = v.imoNumber || "";
    vesselTypeInput.value = v.vesselType || "CONTAINER";
    vesselFlagInput.value = v.flag || "";
    vesselOperatorInput.value = v.operatorName || "";
    vesselCapacityInput.value = v.capacity !== null ? v.capacity : "0";
    vesselStatusInput.value = v.status || "ACTIVE";
    vesselLengthInput.value = v.length !== null && v.length !== undefined ? v.length : "";
    vesselDraftInput.value = v.draft !== null && v.draft !== undefined ? v.draft : "";

    modalTitle.textContent = `Edit Vessel #${v.id} - ${v.name}`;
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function closeModal() { modal.classList.remove("show"); }
  function closeTrackingModal() { trackingModal.classList.remove("show"); }

  async function handleTrackingSubmit(e) {
    e.preventDefault();
    trackingError.hidden = true;

    const id = trackingVesselIdInput.value;
    const latVal = trackingLatInput.value ? parseFloat(trackingLatInput.value) : null;
    const lngVal = trackingLngInput.value ? parseFloat(trackingLngInput.value) : null;

    if (latVal !== null && (latVal < -90 || latVal > 90)) {
      trackingError.textContent = "Latitude must be between -90 and 90 degrees.";
      trackingError.hidden = false;
      return;
    }
    if (lngVal !== null && (lngVal < -180 || lngVal > 180)) {
      trackingError.textContent = "Longitude must be between -180 and 180 degrees.";
      trackingError.hidden = false;
      return;
    }

    const payload = {
      latitude: latVal,
      longitude: lngVal,
      currentLocation: trackingLocationInput.value.trim(),
      status: trackingStatusInput.value
    };

    trackingSubmitBtn.disabled = true;
    trackingSubmitBtn.innerHTML = `<span class="loading-spinner"></span> Updating...`;

    try {
      await api.post(`vessels/${id}/tracking`, payload);
      toastFn(`Vessel tracking position updated.`);
      closeTrackingModal();
      loadVessels(pageNum, currentSearch);
    } catch (error) {
      trackingError.textContent = error.message;
      trackingError.hidden = false;
    } finally {
      trackingSubmitBtn.disabled = false;
      trackingSubmitBtn.textContent = "Update Position";
    }
  }

  async function handleFormSubmit(e) {
    e.preventDefault();
    modalError.hidden = true;

    const id = vesselIdInput.value;
    const payload = {
      name: vesselNameInput.value.trim(),
      imoNumber: vesselImoInput.value.trim(),
      vesselType: vesselTypeInput.value,
      flag: vesselFlagInput.value.trim(),
      operatorName: vesselOperatorInput.value.trim(),
      capacity: parseInt(vesselCapacityInput.value || "0", 10),
      status: vesselStatusInput.value,
      length: vesselLengthInput.value ? parseFloat(vesselLengthInput.value) : null,
      draft: vesselDraftInput.value ? parseFloat(vesselDraftInput.value) : null
    };

    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="loading-spinner"></span> Saving...`;

    try {
      if (id) {
        await api.put(`vessels/${id}`, payload);
        toastFn(`Vessel "${payload.name}" updated successfully.`);
      } else {
        await api.post("vessels", payload);
        toastFn(`Vessel "${payload.name}" created successfully.`);
      }
      closeModal();
      loadVessels(pageNum, currentSearch);
    } catch (error) {
      modalError.textContent = error.message;
      modalError.hidden = false;
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Save Vessel";
    }
  }

  async function handleDelete(id) {
    const v = vesselsData.find(item => String(item.id) === String(id));
    const name = v ? `${v.name} (${v.imoNumber})` : `#${id}`;

    if (!confirm(`Are you sure you want to delete Vessel ${name}?`)) return;

    try {
      await api.delete(`vessels/${id}`);
      toastFn(`Vessel ${name} deleted successfully.`);
      loadVessels(pageNum, currentSearch);
    } catch (error) {
      if (error.status === 409) {
        alert(`Cannot delete Vessel ${name}: ${error.message}`);
      } else {
        alert(`Delete failed: ${error.message}`);
      }
    }
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  container.querySelector("#btn-add-vessel").addEventListener("click", openCreateModal);
  container.querySelector("#btn-refresh-vessels").addEventListener("click", () => loadVessels(pageNum, currentSearch));
  container.querySelector("#modal-close-btn").addEventListener("click", closeModal);
  container.querySelector("#modal-cancel-btn").addEventListener("click", closeModal);
  vesselForm.addEventListener("submit", handleFormSubmit);

  container.querySelector("#tracking-modal-close-btn").addEventListener("click", closeTrackingModal);
  container.querySelector("#tracking-cancel-btn").addEventListener("click", closeTrackingModal);
  trackingForm.addEventListener("submit", handleTrackingSubmit);

  container.querySelector("#history-close-btn").addEventListener("click", () => historyModal.classList.remove("show"));
  container.querySelector("#history-done-btn").addEventListener("click", () => historyModal.classList.remove("show"));

  prevBtn.addEventListener("click", () => { if (pageNum > 0) loadVessels(pageNum - 1, currentSearch); });
  nextBtn.addEventListener("click", () => { if (pageNum < totalPages - 1) loadVessels(pageNum + 1, currentSearch); });

  let searchTimeout = null;
  const searchInput = container.querySelector("#vessel-search-input");
  searchInput.addEventListener("input", () => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
      currentSearch = searchInput.value.trim();
      loadVessels(0, currentSearch);
    }, 300);
  });

  loadVessels(0);
}
