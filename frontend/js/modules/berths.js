import { api } from "../api/api.js";

export function initBerthsModule(container, toastFn) {
  let berthsData = [];
  let terminalsList = [];
  let vesselsList = [];
  let allocationsData = [];

  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / BERTH MANAGEMENT</p>
        <h1>Berth & Allocation Management</h1>
        <p>Schedule berth availability and allocate berths without scheduling conflicts.</p>
      </div>
      <div>
        <button class="button" id="btn-add-berth">＋ Add Berth</button>
        <button class="button primary" id="btn-add-allocation" style="margin-left:6px;">＋ Allocate Berth</button>
      </div>
    </section>

    <section class="stats-grid compact">
      <article class="stat-card">
        <div class="stat-icon blue">◫</div>
        <div><span>Total Berths</span><strong id="stat-total-berths">-</strong><small>Active berths</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon teal">✓</div>
        <div><span>Available Berths</span><strong id="stat-avail-berths">-</strong><small>Ready for docking</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon purple">◉</div>
        <div><span>Active Allocations</span><strong id="stat-total-allocations">-</strong><small>Scheduled vessel calls</small></div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>Berth Allocations</h2>
          <p>Conflict-checked vessel berth scheduling windows.</p>
        </div>
        <button class="button" id="btn-refresh-allocations">↻ Refresh Allocations</button>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Berth Name</th>
              <th>Allocated Vessel</th>
              <th>Start Time (UTC)</th>
              <th>End Time (UTC)</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="allocations-table-body">
            <tr><td colspan="7" style="text-align:center; padding: 20px;"><span class="loading-spinner"></span> Loading allocations...</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>Berth Infrastructure</h2>
          <p>Physical berths registered across terminals.</p>
        </div>
        <button class="button" id="btn-refresh-berths">↻ Refresh Berths</button>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Berth Name</th>
              <th>Terminal</th>
              <th>Max Length (m)</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="berths-table-body">
            <tr><td colspan="6" style="text-align:center; padding: 20px;"><span class="loading-spinner"></span> Loading berths...</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Modal for Create / Edit Berth -->
    <div class="modal-overlay" id="berth-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3 id="berth-modal-title">Add New Berth</h3>
          <button class="modal-close" id="berth-modal-close">&times;</button>
        </div>
        <div class="error-banner" id="berth-modal-error" hidden></div>
        <form id="berth-form">
          <input type="hidden" id="berth-id">
          <div class="modal-body">
            <label class="field">
              <span>Berth Name *</span>
              <input type="text" id="berth-name" required placeholder="e.g. Pier 4 North">
            </label>
            <label class="field">
              <span>Parent Terminal *</span>
              <select id="berth-terminal" required style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);"></select>
            </label>
            <label class="field">
              <span>Max Length (meters) *</span>
              <input type="number" step="any" id="berth-max-length" required min="1" value="350">
            </label>
            <label class="field">
              <span>Status</span>
              <select id="berth-status" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                <option value="AVAILABLE">AVAILABLE</option>
                <option value="OCCUPIED">OCCUPIED</option>
                <option value="MAINTENANCE">MAINTENANCE</option>
              </select>
            </label>
          </div>
          <div class="modal-footer">
            <button type="button" class="button" id="berth-modal-cancel">Cancel</button>
            <button type="submit" class="button primary" id="berth-modal-submit">Save Berth</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal for Create Berth Allocation -->
    <div class="modal-overlay" id="allocation-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3>Allocate Berth to Vessel</h3>
          <button class="modal-close" id="alloc-modal-close">&times;</button>
        </div>
        <div class="error-banner" id="alloc-modal-error" hidden></div>
        <form id="allocation-form">
          <div class="modal-body">
            <label class="field">
              <span>Select Registered Berth</span>
              <select id="alloc-berth-select" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                <option value="">-- Or enter custom berth name below --</option>
              </select>
            </label>
            <label class="field">
              <span>Berth Identifier / Name *</span>
              <input type="text" id="alloc-berth-name" required placeholder="e.g. Pier 4 North">
            </label>
            <label class="field">
              <span>Target Vessel *</span>
              <select id="alloc-vessel-id" required style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);"></select>
            </label>
            <label class="field">
              <span>Arrival / Start Time *</span>
              <input type="datetime-local" id="alloc-start-time" required>
            </label>
            <label class="field">
              <span>Departure / End Time *</span>
              <input type="datetime-local" id="alloc-end-time" required>
            </label>
          </div>
          <div class="modal-footer">
            <button type="button" class="button" id="alloc-modal-cancel">Cancel</button>
            <button type="submit" class="button primary" id="alloc-modal-submit">Allocate Berth</button>
          </div>
        </form>
      </div>
    </div>
  `;

  const berthsTbody = container.querySelector("#berths-table-body");
  const allocsTbody = container.querySelector("#allocations-table-body");

  const berthModal = container.querySelector("#berth-modal");
  const berthForm = container.querySelector("#berth-form");
  const berthError = container.querySelector("#berth-modal-error");
  const berthTerminalSelect = container.querySelector("#berth-terminal");

  const allocModal = container.querySelector("#allocation-modal");
  const allocForm = container.querySelector("#allocation-form");
  const allocError = container.querySelector("#alloc-modal-error");
  const allocBerthSelect = container.querySelector("#alloc-berth-select");
  const allocBerthNameInput = container.querySelector("#alloc-berth-name");
  const allocVesselSelect = container.querySelector("#alloc-vessel-id");
  const allocSubmitBtn = container.querySelector("#alloc-modal-submit");

  async function loadDropdowns() {
    try {
      const [termRes, vesRes] = await Promise.all([
        api.get("terminals", { size: 100 }),
        api.get("vessels", { size: 100 })
      ]);
      terminalsList = api.unwrap(termRes);
      vesselsList = api.unwrap(vesRes);

      berthTerminalSelect.innerHTML = `<option value="">-- Select Terminal --</option>` +
        terminalsList.map(t => `<option value="${t.id}">${escapeHtml(t.name)} (${t.code})</option>`).join("");

      allocVesselSelect.innerHTML = `<option value="">-- Select Vessel --</option>` +
        vesselsList.map(v => `<option value="${v.id}">${escapeHtml(v.name)} (${v.imoNumber})</option>`).join("");
    } catch (e) {
      console.warn("Could not load berths dropdowns", e);
    }
  }

  function updateBerthSelectOptions() {
    allocBerthSelect.innerHTML = `<option value="">-- Or enter custom berth name below --</option>` +
      berthsData.map(b => `<option value="${b.id}" data-name="${escapeHtml(b.name)}">${escapeHtml(b.name)} (${b.terminal ? escapeHtml(b.terminal.name) : "Unassigned"})</option>`).join("");
  }

  async function loadData() {
    await loadDropdowns();
    await Promise.all([loadBerths(), loadAllocations()]);
  }

  async function loadBerths() {
    berthsTbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 20px;"><span class="loading-spinner"></span> Loading berths...</td></tr>`;
    try {
      const res = await api.get("berths", { size: 100 });
      berthsData = api.unwrap(res);

      container.querySelector("#stat-total-berths").textContent = berthsData.length;
      container.querySelector("#stat-avail-berths").textContent = berthsData.filter(b => b.status === "AVAILABLE").length;
      updateBerthSelectOptions();

      if (berthsData.length === 0) {
        berthsTbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--muted); padding:20px;">No berths found.</td></tr>`;
        return;
      }

      berthsTbody.innerHTML = berthsData.map(b => `
        <tr>
          <td><strong>#${b.id}</strong></td>
          <td><strong>${escapeHtml(b.name)}</strong></td>
          <td>${escapeHtml(b.terminal ? b.terminal.name : "Unassigned")}</td>
          <td>${b.maxLengthM ? b.maxLengthM + " m" : "-"}</td>
          <td><span class="status ${b.status === "AVAILABLE" ? "delivered" : "delayed"}">${escapeHtml(b.status || "AVAILABLE")}</span></td>
          <td>
            <button class="table-action btn-edit-berth" data-id="${b.id}">Edit</button>
            <button class="table-action danger btn-delete-berth" data-id="${b.id}">Delete</button>
          </td>
        </tr>
      `).join("");

      berthsTbody.querySelectorAll(".btn-edit-berth").forEach(btn => {
        btn.addEventListener("click", () => openEditBerthModal(btn.dataset.id));
      });
      berthsTbody.querySelectorAll(".btn-delete-berth").forEach(btn => {
        btn.addEventListener("click", () => handleDeleteBerth(btn.dataset.id));
      });
    } catch (e) {
      berthsTbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:#e07b65;">Failed to load berths: ${escapeHtml(e.message)}</td></tr>`;
    }
  }

  async function loadAllocations() {
    allocsTbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 20px;"><span class="loading-spinner"></span> Loading allocations...</td></tr>`;
    try {
      const res = await api.get("berth-allocations");
      allocationsData = api.unwrap(res);

      container.querySelector("#stat-total-allocations").textContent = allocationsData.filter(a => a.status !== "CANCELLED").length;

      if (allocationsData.length === 0) {
        allocsTbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:var(--muted); padding:20px;">No active berth allocations.</td></tr>`;
        return;
      }

      allocsTbody.innerHTML = allocationsData.map(a => `
        <tr>
          <td><strong>#${a.id}</strong></td>
          <td><strong>${escapeHtml(a.berth || (a.berthEntity ? a.berthEntity.name : "Unassigned"))}</strong></td>
          <td>${escapeHtml(a.vessel ? a.vessel.name : "Unknown Vessel")}</td>
          <td><small>${a.startTime ? new Date(a.startTime).toLocaleString() : "-"}</small></td>
          <td><small>${a.endTime ? new Date(a.endTime).toLocaleString() : "-"}</small></td>
          <td><span class="status ${a.status === "SCHEDULED" ? "delivered" : "delayed"}">${escapeHtml(a.status || "SCHEDULED")}</span></td>
          <td>
            <button class="table-action danger btn-delete-alloc" data-id="${a.id}">Cancel</button>
          </td>
        </tr>
      `).join("");

      allocsTbody.querySelectorAll(".btn-delete-alloc").forEach(btn => {
        btn.addEventListener("click", () => handleDeleteAlloc(btn.dataset.id));
      });
    } catch (e) {
      allocsTbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#e07b65;">Failed to load allocations: ${escapeHtml(e.message)}</td></tr>`;
    }
  }

  function openCreateBerthModal() {
    berthForm.reset();
    container.querySelector("#berth-id").value = "";
    container.querySelector("#berth-modal-title").textContent = "Add New Berth";
    berthError.hidden = true;
    berthModal.classList.add("show");
  }

  function openEditBerthModal(id) {
    const b = berthsData.find(item => String(item.id) === String(id));
    if (!b) return;

    berthForm.reset();
    container.querySelector("#berth-id").value = b.id;
    container.querySelector("#berth-name").value = b.name || "";
    berthTerminalSelect.value = b.terminal ? b.terminal.id : "";
    container.querySelector("#berth-max-length").value = b.maxLengthM || "350";
    container.querySelector("#berth-status").value = b.status || "AVAILABLE";

    container.querySelector("#berth-modal-title").textContent = `Edit Berth #${b.id} - ${b.name}`;
    berthError.hidden = true;
    berthModal.classList.add("show");
  }

  async function handleBerthSubmit(e) {
    e.preventDefault();
    berthError.hidden = true;

    const id = container.querySelector("#berth-id").value;
    const termId = berthTerminalSelect.value;
    if (!termId) {
      berthError.textContent = "Please select a terminal.";
      berthError.hidden = false;
      return;
    }

    const payload = {
      name: container.querySelector("#berth-name").value.trim(),
      terminalId: parseInt(termId, 10),
      maxLengthM: parseFloat(container.querySelector("#berth-max-length").value || "350"),
      status: container.querySelector("#berth-status").value
    };

    try {
      if (id) {
        await api.put(`berths/${id}`, payload);
        toastFn(`Berth "${payload.name}" updated successfully.`);
      } else {
        await api.post("berths", payload);
        toastFn(`Berth "${payload.name}" created successfully.`);
      }
      berthModal.classList.remove("show");
      loadBerths();
    } catch (err) {
      berthError.textContent = err.message;
      berthError.hidden = false;
    }
  }

  async function handleDeleteBerth(id) {
    if (!confirm(`Are you sure you want to delete Berth #${id}?`)) return;
    try {
      await api.delete(`berths/${id}`);
      toastFn(`Berth #${id} deleted.`);
      loadBerths();
    } catch (err) {
      if (err.status === 409) {
        alert(`Cannot delete Berth #${id}: ${err.message}`);
      } else {
        alert(`Delete failed: ${err.message}`);
      }
    }
  }

  async function handleDeleteAlloc(id) {
    if (!confirm(`Are you sure you want to cancel Berth Allocation #${id}?`)) return;
    try {
      await api.delete(`berth-allocations/${id}`);
      toastFn(`Berth Allocation #${id} removed.`);
      loadAllocations();
    } catch (err) {
      alert(`Cancel failed: ${err.message}`);
    }
  }

  function openAllocModal() {
    allocForm.reset();
    allocError.hidden = true;

    const now = new Date();
    const later = new Date(now.getTime() + 24 * 60 * 60 * 1000);
    container.querySelector("#alloc-start-time").value = now.toISOString().slice(0, 16);
    container.querySelector("#alloc-end-time").value = later.toISOString().slice(0, 16);

    allocModal.classList.add("show");
  }

  allocBerthSelect.addEventListener("change", () => {
    const selected = allocBerthSelect.options[allocBerthSelect.selectedIndex];
    if (selected && selected.dataset.name) {
      allocBerthNameInput.value = selected.dataset.name;
    }
  });

  async function handleAllocSubmit(e) {
    e.preventDefault();
    allocError.hidden = true;

    const vesselId = allocVesselSelect.value;
    if (!vesselId) {
      allocError.textContent = "Please select a target vessel.";
      allocError.hidden = false;
      return;
    }

    const startVal = container.querySelector("#alloc-start-time").value;
    const endVal = container.querySelector("#alloc-end-time").value;

    if (!startVal || !endVal) {
      allocError.textContent = "Please select both start and end times.";
      allocError.hidden = false;
      return;
    }

    const startDate = new Date(startVal);
    const endDate = new Date(endVal);

    if (startDate >= endDate) {
      allocError.textContent = "Arrival / Start time must be before Departure / End time.";
      allocError.hidden = false;
      return;
    }

    const berthIdVal = allocBerthSelect.value ? parseInt(allocBerthSelect.value, 10) : null;
    const payload = {
      berthId: berthIdVal,
      berth: allocBerthNameInput.value.trim(),
      vesselId: parseInt(vesselId, 10),
      startTime: startDate.toISOString(),
      endTime: endDate.toISOString()
    };

    allocSubmitBtn.disabled = true;
    allocSubmitBtn.innerHTML = `<span class="loading-spinner"></span> Allocating...`;

    try {
      await api.post("berth-allocations", payload);
      toastFn(`Berth allocated to vessel successfully.`);
      allocModal.classList.remove("show");
      loadAllocations();
    } catch (err) {
      allocError.textContent = err.message;
      allocError.hidden = false;
    } finally {
      allocSubmitBtn.disabled = false;
      allocSubmitBtn.textContent = "Allocate Berth";
    }
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  container.querySelector("#btn-add-berth").addEventListener("click", openCreateBerthModal);
  container.querySelector("#btn-add-allocation").addEventListener("click", openAllocModal);
  container.querySelector("#btn-refresh-berths").addEventListener("click", loadBerths);
  container.querySelector("#btn-refresh-allocations").addEventListener("click", loadAllocations);

  container.querySelector("#berth-modal-close").addEventListener("click", () => berthModal.classList.remove("show"));
  container.querySelector("#berth-modal-cancel").addEventListener("click", () => berthModal.classList.remove("show"));
  berthForm.addEventListener("submit", handleBerthSubmit);

  container.querySelector("#alloc-modal-close").addEventListener("click", () => allocModal.classList.remove("show"));
  container.querySelector("#alloc-modal-cancel").addEventListener("click", () => allocModal.classList.remove("show"));
  allocForm.addEventListener("submit", handleAllocSubmit);

  loadData();
}
