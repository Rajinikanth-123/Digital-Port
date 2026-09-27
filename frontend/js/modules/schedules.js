import { api } from "../api/api.js";

export function initSchedulesModule(container, toastFn) {
  let schedulesData = [];
  let vesselsList = [];
  let berthsList = [];
  let pageNum = 0;
  let totalPages = 1;
  let currentSearch = "";
  let currentStatusFilter = "";

  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / SHIPPING SCHEDULES</p>
        <h1>Shipping Schedules</h1>
        <p>Manage vessel arrival and departure timetables, port calls, berth reservations, and schedule changes.</p>
      </div>
      <button class="button primary" id="btn-add-schedule">＋ Add Schedule</button>
    </section>

    <section class="stats-grid compact">
      <article class="stat-card">
        <div class="stat-icon blue">◷</div>
        <div><span>Total Schedules</span><strong id="stat-total-schedules">-</strong><small>Live count</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon teal">✓</div>
        <div><span>Upcoming Port Calls</span><strong id="stat-upcoming">-</strong><small>Scheduled</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon orange">⚠</div>
        <div><span>Delayed Calls</span><strong id="stat-delayed">-</strong><small>Requires attention</small></div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>Port Call Timetable</h2>
          <p>Search, filter, and coordinate vessel arrival/departure windows.</p>
        </div>
        <div class="toolbar">
          <input class="table-search" id="schedule-search-input" placeholder="Search vessel or IMO..." aria-label="Search schedules">
          <select id="schedule-status-filter" aria-label="Filter status" style="padding:9px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
            <option value="">All Statuses</option>
            <option value="SCHEDULED">SCHEDULED</option>
            <option value="BERTHED">BERTHED</option>
            <option value="DEPARTED">DEPARTED</option>
            <option value="DELAYED">DELAYED</option>
            <option value="CANCELLED">CANCELLED</option>
          </select>
          <button class="button" id="btn-refresh-schedules">↻ Refresh</button>
        </div>
      </div>
      <div class="table-wrap">
        <table id="schedules-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Vessel Name</th>
              <th>IMO Number</th>
              <th>Berth Location</th>
              <th>Estimated Arrival (ETA)</th>
              <th>Estimated Departure (ETD)</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="schedules-table-body">
            <tr><td colspan="8" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading schedules...</td></tr>
          </tbody>
        </table>
        <div class="empty-state" id="schedules-empty-state" hidden>No schedule records found.</div>
      </div>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-top:16px; font-size:12px; color:var(--muted);">
        <span id="schedules-page-info">Showing page 1</span>
        <div>
          <button class="button" id="btn-prev-page" disabled>Previous</button>
          <button class="button" id="btn-next-page" disabled style="margin-left:6px;">Next</button>
        </div>
      </div>
    </section>

    <!-- Modal for Create / Edit Schedule -->
    <div class="modal-overlay" id="schedule-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3 id="modal-title">Add Shipping Schedule</h3>
          <button class="modal-close" id="modal-close-btn">&times;</button>
        </div>
        <div class="error-banner" id="modal-error" hidden></div>
        <form id="schedule-form">
          <input type="hidden" id="schedule-id">
          <div class="modal-body">
            <label class="field">
              <span>Vessel Assignment *</span>
              <select id="schedule-vessel-id" required style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);"></select>
            </label>
            <label class="field">
              <span>Berth Reservation (Optional)</span>
              <select id="schedule-berth-id" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);"></select>
            </label>
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
              <label class="field">
                <span>Estimated Arrival (ETA) *</span>
                <input type="datetime-local" id="schedule-eta" required>
              </label>
              <label class="field">
                <span>Estimated Departure (ETD)</span>
                <input type="datetime-local" id="schedule-etd">
              </label>
            </div>
            <label class="field">
              <span>Status</span>
              <select id="schedule-status" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                <option value="SCHEDULED">SCHEDULED</option>
                <option value="BERTHED">BERTHED</option>
                <option value="DEPARTED">DEPARTED</option>
                <option value="DELAYED">DELAYED</option>
                <option value="CANCELLED">CANCELLED</option>
              </select>
            </label>
          </div>
          <div class="modal-footer">
            <button type="button" class="button" id="modal-cancel-btn">Cancel</button>
            <button type="submit" class="button primary" id="modal-submit-btn">Save Schedule</button>
          </div>
        </form>
      </div>
    </div>
  `;

  const tbody = container.querySelector("#schedules-table-body");
  const emptyState = container.querySelector("#schedules-empty-state");
  const modal = container.querySelector("#schedule-modal");
  const modalTitle = container.querySelector("#modal-title");
  const modalError = container.querySelector("#modal-error");
  const scheduleForm = container.querySelector("#schedule-form");
  const scheduleIdInput = container.querySelector("#schedule-id");
  const vesselSelect = container.querySelector("#schedule-vessel-id");
  const berthSelect = container.querySelector("#schedule-berth-id");
  const etaInput = container.querySelector("#schedule-eta");
  const etdInput = container.querySelector("#schedule-etd");
  const statusInput = container.querySelector("#schedule-status");
  const submitBtn = container.querySelector("#modal-submit-btn");

  const statTotal = container.querySelector("#stat-total-schedules");
  const statUpcoming = container.querySelector("#stat-upcoming");
  const statDelayed = container.querySelector("#stat-delayed");
  const pageInfo = container.querySelector("#schedules-page-info");
  const prevBtn = container.querySelector("#btn-prev-page");
  const nextBtn = container.querySelector("#btn-next-page");
  const statusFilter = container.querySelector("#schedule-status-filter");

  async function loadDropdowns() {
    try {
      const [vesRes, berRes] = await Promise.all([
        api.get("vessels", { size: 100 }),
        api.get("berths", { size: 100 })
      ]);
      vesselsList = api.unwrap(vesRes);
      berthsList = api.unwrap(berRes);

      vesselSelect.innerHTML = `<option value="">-- Select Vessel --</option>` +
        vesselsList.map(v => `<option value="${v.id}">${escapeHtml(v.name)} (${v.imoNumber})</option>`).join("");

      berthSelect.innerHTML = `<option value="">-- Select Berth --</option>` +
        berthsList.map(b => `<option value="${b.id}">${escapeHtml(b.name)}</option>`).join("");
    } catch (e) {
      console.warn("Could not load dropdowns for schedules", e);
    }
  }

  async function loadSchedules(page = 0, search = currentSearch, status = currentStatusFilter) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading schedules...</td></tr>`;
    emptyState.hidden = true;

    try {
      await loadDropdowns();
      const params = { page, size: 20 };
      if (search) params.search = search;
      if (status) params.status = status;

      const response = await api.get("schedules", params);
      schedulesData = api.unwrap(response);
      pageNum = response.number !== undefined ? response.number : page;
      totalPages = response.totalPages || 1;

      renderTable(schedulesData);
      updateStats(response.totalElements !== undefined ? response.totalElements : schedulesData.length, schedulesData);

      pageInfo.textContent = `Page ${pageNum + 1} of ${totalPages} (${response.totalElements !== undefined ? response.totalElements : schedulesData.length} records)`;
      prevBtn.disabled = pageNum <= 0;
      nextBtn.disabled = pageNum >= totalPages - 1;
    } catch (error) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:#e07b65; padding: 24px;">Failed to load schedules: ${escapeHtml(error.message)}</td></tr>`;
      toastFn(`Error: ${error.message}`);
    }
  }

  function updateStats(total, items) {
    statTotal.textContent = total;
    statUpcoming.textContent = items.filter(s => (s.status || "").toUpperCase() === "SCHEDULED").length;
    statDelayed.textContent = items.filter(s => (s.status || "").toUpperCase() === "DELAYED").length;
  }

  function renderTable(items) {
    if (!items || items.length === 0) {
      tbody.innerHTML = "";
      emptyState.hidden = false;
      return;
    }

    emptyState.hidden = true;
    tbody.innerHTML = items.map(s => {
      const vesselName = s.vessel ? s.vessel.name : "Unassigned";
      const vesselImo = s.vessel ? s.vessel.imoNumber : "-";
      const berthName = s.berth ? s.berth.name : "Unassigned";
      const eta = s.arrivalEstimated ? new Date(s.arrivalEstimated).toLocaleString() : "-";
      const etd = s.departureEstimated ? new Date(s.departureEstimated).toLocaleString() : "-";

      let statusClass = "delivered";
      if (s.status === "DELAYED") statusClass = "customs-hold";
      if (s.status === "BERTHED") statusClass = "at-berth";
      if (s.status === "CANCELLED") statusClass = "danger";

      return `
        <tr>
          <td><strong>#${s.id}</strong></td>
          <td><strong>${escapeHtml(vesselName)}</strong></td>
          <td><span class="status at-berth">${escapeHtml(vesselImo)}</span></td>
          <td>${escapeHtml(berthName)}</td>
          <td><small>${eta}</small></td>
          <td><small>${etd}</small></td>
          <td><span class="status ${statusClass}">${escapeHtml(s.status || "SCHEDULED")}</span></td>
          <td>
            <button class="table-action btn-edit-schedule" data-id="${s.id}">Edit</button>
            <button class="table-action danger btn-delete-schedule" data-id="${s.id}">Delete</button>
          </td>
        </tr>
      `;
    }).join("");

    tbody.querySelectorAll(".btn-edit-schedule").forEach(btn => {
      btn.addEventListener("click", () => openEditModal(btn.dataset.id));
    });

    tbody.querySelectorAll(".btn-delete-schedule").forEach(btn => {
      btn.addEventListener("click", () => handleDelete(btn.dataset.id));
    });
  }

  function openCreateModal() {
    scheduleForm.reset();
    scheduleIdInput.value = "";
    statusInput.value = "SCHEDULED";

    const now = new Date();
    const later = new Date(now.getTime() + 12 * 60 * 60 * 1000);
    etaInput.value = now.toISOString().slice(0, 16);
    etdInput.value = later.toISOString().slice(0, 16);

    modalTitle.textContent = "Add Shipping Schedule";
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function openEditModal(id) {
    const s = schedulesData.find(item => String(item.id) === String(id));
    if (!s) return;

    scheduleForm.reset();
    scheduleIdInput.value = s.id;
    vesselSelect.value = s.vessel ? s.vessel.id : "";
    berthSelect.value = s.berth ? s.berth.id : "";
    etaInput.value = s.arrivalEstimated ? new Date(s.arrivalEstimated).toISOString().slice(0, 16) : "";
    etdInput.value = s.departureEstimated ? new Date(s.departureEstimated).toISOString().slice(0, 16) : "";
    statusInput.value = s.status || "SCHEDULED";

    modalTitle.textContent = `Edit Schedule #${s.id}`;
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function closeModal() { modal.classList.remove("show"); }

  async function handleFormSubmit(e) {
    e.preventDefault();
    modalError.hidden = true;

    const id = scheduleIdInput.value;
    const vesselId = vesselSelect.value;
    if (!vesselId) {
      modalError.textContent = "Please select a vessel.";
      modalError.hidden = false;
      return;
    }

    const berthIdVal = berthSelect.value;
    const payload = {
      vesselId: parseInt(vesselId, 10),
      berthId: berthIdVal ? parseInt(berthIdVal, 10) : null,
      arrivalEstimated: etaInput.value ? new Date(etaInput.value).toISOString() : null,
      departureEstimated: etdInput.value ? new Date(etdInput.value).toISOString() : null,
      status: statusInput.value
    };

    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="loading-spinner"></span> Saving...`;

    try {
      if (id) {
        await api.put(`schedules/${id}`, payload);
        toastFn(`Schedule updated successfully.`);
      } else {
        await api.post("schedules", payload);
        toastFn(`Schedule created successfully.`);
      }
      closeModal();
      loadSchedules(pageNum, currentSearch, currentStatusFilter);
    } catch (error) {
      modalError.textContent = error.message;
      modalError.hidden = false;
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Save Schedule";
    }
  }

  async function handleDelete(id) {
    if (!confirm(`Are you sure you want to delete Schedule #${id}?`)) return;

    try {
      await api.delete(`schedules/${id}`);
      toastFn(`Schedule #${id} deleted.`);
      loadSchedules(pageNum, currentSearch, currentStatusFilter);
    } catch (error) {
      alert(`Delete failed: ${error.message}`);
    }
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  container.querySelector("#btn-add-schedule").addEventListener("click", openCreateModal);
  container.querySelector("#btn-refresh-schedules").addEventListener("click", () => loadSchedules(pageNum, currentSearch, currentStatusFilter));
  container.querySelector("#modal-close-btn").addEventListener("click", closeModal);
  container.querySelector("#modal-cancel-btn").addEventListener("click", closeModal);
  scheduleForm.addEventListener("submit", handleFormSubmit);

  statusFilter.addEventListener("change", () => {
    currentStatusFilter = statusFilter.value;
    loadSchedules(0, currentSearch, currentStatusFilter);
  });

  prevBtn.addEventListener("click", () => { if (pageNum > 0) loadSchedules(pageNum - 1, currentSearch, currentStatusFilter); });
  nextBtn.addEventListener("click", () => { if (pageNum < totalPages - 1) loadSchedules(pageNum + 1, currentSearch, currentStatusFilter); });

  let searchTimeout = null;
  const searchInput = container.querySelector("#schedule-search-input");
  searchInput.addEventListener("input", () => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
      currentSearch = searchInput.value.trim();
      loadSchedules(0, currentSearch, currentStatusFilter);
    }, 300);
  });

  loadSchedules(0);
}
