import { api } from "../api/api.js";

export function initCustomsModule(container, toastFn) {
  let customsData = [];
  let shipmentsList = [];
  let pageNum = 0;
  let totalPages = 1;
  let currentStatusFilter = "ALL";

  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / CUSTOMS CLEARANCE</p>
        <h1>Customs Clearance & Workflows</h1>
        <p>Manage declarations, tariff documentation, officer inspections, approvals, and rejections.</p>
      </div>
      <button class="button primary" id="btn-add-customs">＋ File Declaration</button>
    </section>

    <section class="stats-grid compact">
      <article class="stat-card">
        <div class="stat-icon blue">📋</div>
        <div><span>Total Declarations</span><strong id="stat-total-customs">-</strong><small>Live records</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon teal">✓</div>
        <div><span>Approved & Cleared</span><strong id="stat-cleared">-</strong><small>Pass inspection</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon orange">◷</div>
        <div><span>In Review / Pending</span><strong id="stat-pending">-</strong><small>Officer examination</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon red" style="color:var(--danger, #e07b65);">❌</div>
        <div><span>Rejected / Holds</span><strong id="stat-rejected">-</strong><small>Requires action</small></div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>Customs Filing Index</h2>
          <p>Search, filter, and process customs clearance workflows.</p>
        </div>
        <div class="toolbar" style="display:flex; gap:10px; align-items:center;">
          <select id="customs-filter-status" style="padding:8px 12px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text); font-size:13px;" aria-label="Filter by status">
            <option value="ALL">All Statuses</option>
            <option value="PENDING">PENDING</option>
            <option value="IN_REVIEW">IN_REVIEW</option>
            <option value="APPROVED">APPROVED</option>
            <option value="REJECTED">REJECTED</option>
            <option value="ADDITIONAL_INFO_REQUIRED">INFO REQUIRED</option>
          </select>
          <input class="table-search" id="customs-search-input" placeholder="Search declaration no..." aria-label="Search customs">
          <button class="button" id="btn-refresh-customs">↻ Refresh</button>
        </div>
      </div>
      <div class="table-wrap">
        <table id="customs-table">
          <thead>
            <tr>
              <th data-sort>ID</th>
              <th data-sort>Declaration No. ↕</th>
              <th data-sort>Target Shipment ↕</th>
              <th>Workflow Status</th>
              <th>Assigned Officer</th>
              <th>Remarks / Inspection Notes</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="customs-table-body">
            <tr><td colspan="7" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading customs declarations...</td></tr>
          </tbody>
        </table>
        <div class="empty-state" id="customs-empty-state" hidden>No customs declarations found matching your filter criteria.</div>
      </div>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-top:16px; font-size:12px; color:var(--muted);">
        <span id="customs-page-info">Showing page 1</span>
        <div>
          <button class="button" id="btn-prev-page" disabled>Previous</button>
          <button class="button" id="btn-next-page" disabled style="margin-left:6px;">Next</button>
        </div>
      </div>
    </section>

    <!-- Modal for File / Edit Customs Declaration -->
    <div class="modal-overlay" id="customs-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3 id="modal-title">File Customs Declaration</h3>
          <button class="modal-close" id="modal-close-btn">&times;</button>
        </div>
        <div class="error-banner" id="modal-error" hidden></div>
        <form id="customs-form">
          <input type="hidden" id="customs-id">
          <div class="modal-body">
            <label class="field">
              <span>Declaration Number *</span>
              <input type="text" id="customs-declaration" required placeholder="e.g. CUST-2026-90412">
            </label>
            <label class="field">
              <span>Linked Shipment *</span>
              <select id="customs-shipment-id" required style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);"></select>
            </label>
            <label class="field">
              <span>Initial Status</span>
              <select id="customs-status" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                <option value="PENDING">PENDING</option>
                <option value="IN_REVIEW">IN_REVIEW</option>
                <option value="APPROVED">APPROVED</option>
                <option value="REJECTED">REJECTED</option>
                <option value="ADDITIONAL_INFO_REQUIRED">ADDITIONAL_INFO_REQUIRED</option>
              </select>
            </label>
            <label class="field">
              <span>Document & Tariff Details</span>
              <textarea id="customs-details" rows="3" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);" placeholder="Enter HS Codes, tariff duty assessment, or manifest summary..."></textarea>
            </label>
          </div>
          <div class="modal-footer">
            <button type="button" class="button" id="modal-cancel-btn">Cancel</button>
            <button type="submit" class="button primary" id="modal-submit-btn">Save Declaration</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal for Officer Review / Status Update -->
    <div class="modal-overlay" id="customs-review-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3 id="review-modal-title">Review Customs Clearance</h3>
          <button class="modal-close" id="review-modal-close-btn">&times;</button>
        </div>
        <div class="error-banner" id="review-modal-error" hidden></div>
        <form id="customs-review-form">
          <input type="hidden" id="review-customs-id">
          <div class="modal-body">
            <div style="background:var(--surface-hover, rgba(255,255,255,0.05)); padding:12px; border-radius:8px; margin-bottom:16px;">
              <div><strong>Declaration:</strong> <span id="review-decl-no">-</span></div>
              <div><strong>Shipment:</strong> <span id="review-shipment-ref">-</span></div>
            </div>
            <label class="field">
              <span>Workflow Decision / Status *</span>
              <select id="review-status" required style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                <option value="IN_REVIEW">IN_REVIEW (Under Officer Examination)</option>
                <option value="APPROVED">APPROVED (Customs Cleared)</option>
                <option value="REJECTED">REJECTED (Declined - Remarks Required)</option>
                <option value="ADDITIONAL_INFO_REQUIRED">ADDITIONAL INFO REQUIRED (Remarks Required)</option>
                <option value="PENDING">PENDING (Awaiting Queue)</option>
              </select>
            </label>
            <label class="field">
              <span>Officer Remarks / Rejection Reason *</span>
              <textarea id="review-remarks" rows="4" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);" placeholder="Enter detailed inspection findings, reasons for rejection, or requested documents..."></textarea>
              <small style="color:var(--muted); margin-top:4px; display:block;">Mandatory when status is set to REJECTED or ADDITIONAL INFO REQUIRED.</small>
            </label>
          </div>
          <div class="modal-footer">
            <button type="button" class="button" id="review-modal-cancel-btn">Cancel</button>
            <button type="submit" class="button primary" id="review-modal-submit-btn">Submit Officer Decision</button>
          </div>
        </form>
      </div>
    </div>
  `;

  const tbody = container.querySelector("#customs-table-body");
  const emptyState = container.querySelector("#customs-empty-state");
  const modal = container.querySelector("#customs-modal");
  const modalTitle = container.querySelector("#modal-title");
  const modalError = container.querySelector("#modal-error");
  const customsForm = container.querySelector("#customs-form");
  const customsIdInput = container.querySelector("#customs-id");
  const declarationInput = container.querySelector("#customs-declaration");
  const shipmentSelect = container.querySelector("#customs-shipment-id");
  const statusInput = container.querySelector("#customs-status");
  const detailsInput = container.querySelector("#customs-details");
  const submitBtn = container.querySelector("#modal-submit-btn");

  const reviewModal = container.querySelector("#customs-review-modal");
  const reviewDeclNo = container.querySelector("#review-decl-no");
  const reviewShipmentRef = container.querySelector("#review-shipment-ref");
  const reviewCustomsIdInput = container.querySelector("#review-customs-id");
  const reviewStatusInput = container.querySelector("#review-status");
  const reviewRemarksInput = container.querySelector("#review-remarks");
  const reviewModalError = container.querySelector("#review-modal-error");
  const reviewSubmitBtn = container.querySelector("#review-modal-submit-btn");

  const statTotal = container.querySelector("#stat-total-customs");
  const statCleared = container.querySelector("#stat-cleared");
  const statPending = container.querySelector("#stat-pending");
  const statRejected = container.querySelector("#stat-rejected");

  const pageInfo = container.querySelector("#customs-page-info");
  const prevBtn = container.querySelector("#btn-prev-page");
  const nextBtn = container.querySelector("#btn-next-page");
  const statusFilterSelect = container.querySelector("#customs-filter-status");

  async function loadShipmentsDropdown() {
    try {
      const res = await api.get("shipments", { size: 100 });
      shipmentsList = api.unwrap(res);
      shipmentSelect.innerHTML = `<option value="">-- Select Target Shipment --</option>` +
        shipmentsList.map(s => `<option value="${s.id}">${escapeHtml(s.trackingNumber)} (${s.origin} → ${s.destination})</option>`).join("");
    } catch (e) {
      console.warn("Could not load shipments dropdown for customs", e);
    }
  }

  async function loadCustoms(page = 0) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading customs declarations...</td></tr>`;
    emptyState.hidden = true;

    try {
      await loadShipmentsDropdown();
      const params = { page, size: 20 };
      if (currentStatusFilter && currentStatusFilter !== "ALL") {
        params.status = currentStatusFilter;
      }

      const response = await api.get("customs-clearances", params);
      customsData = api.unwrap(response);
      pageNum = response.number !== undefined ? response.number : page;
      totalPages = response.totalPages || 1;

      renderTable(customsData);
      updateStats(response.totalElements || customsData.length, customsData);

      pageInfo.textContent = `Page ${pageNum + 1} of ${totalPages} (${response.totalElements || customsData.length} records)`;
      prevBtn.disabled = pageNum <= 0;
      nextBtn.disabled = pageNum >= totalPages - 1;
    } catch (error) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#e07b65; padding: 24px;">Failed to load customs declarations: ${error.message}</td></tr>`;
      toastFn(`Error: ${error.message}`);
    }
  }

  function updateStats(total, items) {
    statTotal.textContent = total;
    statCleared.textContent = items.filter(c => c.status === "APPROVED" || c.status === "CLEARED").length;
    statPending.textContent = items.filter(c => c.status === "PENDING" || c.status === "IN_REVIEW").length;
    statRejected.textContent = items.filter(c => c.status === "REJECTED" || c.status === "ADDITIONAL_INFO_REQUIRED").length;
  }

  function renderTable(items) {
    if (!items || items.length === 0) {
      tbody.innerHTML = "";
      emptyState.hidden = false;
      return;
    }

    emptyState.hidden = true;
    tbody.innerHTML = items.map(c => {
      const shipmentRef = c.shipment ? (c.shipment.trackingNumber || `#${c.shipment.id}`) : "Unassigned";
      const officerName = c.assignedOfficer ? (c.assignedOfficer.fullName || c.assignedOfficer.email) : "Unassigned";

      let statusBadgeClass = "customs-hold";
      if (c.status === "APPROVED" || c.status === "CLEARED") statusBadgeClass = "delivered";
      if (c.status === "REJECTED") statusBadgeClass = "delayed";
      if (c.status === "ADDITIONAL_INFO_REQUIRED") statusBadgeClass = "in-transit";

      return `
        <tr>
          <td><strong>#${c.id}</strong></td>
          <td><strong>${escapeHtml(c.declarationNumber)}</strong></td>
          <td>${escapeHtml(shipmentRef)}</td>
          <td><span class="status ${statusBadgeClass}">${escapeHtml(c.status || "PENDING")}</span></td>
          <td><small>${escapeHtml(officerName)}</small></td>
          <td><small style="color:var(--muted); display:inline-block; max-width:220px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${escapeHtml(c.remarks || c.documentDetails || "-")}</small></td>
          <td>
            <button class="table-action btn-review-customs" data-id="${c.id}" style="background:var(--accent-dim); color:var(--accent); border:1px solid var(--accent);">Review</button>
            <button class="table-action btn-edit-customs" data-id="${c.id}">Edit</button>
            <button class="table-action danger btn-delete-customs" data-id="${c.id}">Delete</button>
          </td>
        </tr>
      `;
    }).join("");

    tbody.querySelectorAll(".btn-review-customs").forEach(btn => {
      btn.addEventListener("click", () => openReviewModal(btn.dataset.id));
    });

    tbody.querySelectorAll(".btn-edit-customs").forEach(btn => {
      btn.addEventListener("click", () => openEditModal(btn.dataset.id));
    });

    tbody.querySelectorAll(".btn-delete-customs").forEach(btn => {
      btn.addEventListener("click", () => handleDelete(btn.dataset.id));
    });
  }

  function openCreateModal() {
    customsForm.reset();
    customsIdInput.value = "";
    statusInput.value = "PENDING";
    modalTitle.textContent = "File Customs Declaration";
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function openEditModal(id) {
    const c = customsData.find(item => String(item.id) === String(id));
    if (!c) return;

    customsForm.reset();
    customsIdInput.value = c.id;
    declarationInput.value = c.declarationNumber || "";
    shipmentSelect.value = c.shipment ? c.shipment.id : "";
    statusInput.value = c.status || "PENDING";
    detailsInput.value = c.documentDetails || "";

    modalTitle.textContent = `Edit Declaration #${c.id} - ${c.declarationNumber}`;
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function openReviewModal(id) {
    const c = customsData.find(item => String(item.id) === String(id));
    if (!c) return;

    reviewCustomsIdInput.value = c.id;
    reviewDeclNo.textContent = c.declarationNumber;
    reviewShipmentRef.textContent = c.shipment ? (c.shipment.trackingNumber || `#${c.shipment.id}`) : "Unassigned";
    reviewStatusInput.value = c.status || "IN_REVIEW";
    reviewRemarksInput.value = c.remarks || "";
    reviewModalError.hidden = true;
    reviewModalError.textContent = "";
    reviewModal.classList.add("show");
  }

  function closeModal() { modal.classList.remove("show"); }
  function closeReviewModal() { reviewModal.classList.remove("show"); }

  async function handleFormSubmit(e) {
    e.preventDefault();
    modalError.hidden = true;

    const id = customsIdInput.value;
    const shipmentId = shipmentSelect.value;
    if (!shipmentId) {
      modalError.textContent = "Please select a target shipment.";
      modalError.hidden = false;
      return;
    }

    const payload = {
      declarationNumber: declarationInput.value.trim().toUpperCase(),
      shipmentId: parseInt(shipmentId, 10),
      status: statusInput.value,
      documentDetails: detailsInput.value.trim()
    };

    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="loading-spinner"></span> Saving...`;

    try {
      if (id) {
        await api.put(`customs-clearances/${id}`, payload);
        toastFn(`Declaration updated successfully.`);
      } else {
        await api.post("customs-clearances", payload);
        toastFn(`Declaration filed successfully.`);
      }
      closeModal();
      loadCustoms(pageNum);
    } catch (error) {
      modalError.textContent = error.message;
      modalError.hidden = false;
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Save Declaration";
    }
  }

  async function handleReviewSubmit(e) {
    e.preventDefault();
    reviewModalError.hidden = true;

    const id = reviewCustomsIdInput.value;
    const status = reviewStatusInput.value;
    const remarks = reviewRemarksInput.value.trim();

    if ((status === "REJECTED" || status === "ADDITIONAL_INFO_REQUIRED") && !remarks) {
      reviewModalError.textContent = "Officer remarks are required for REJECTED or ADDITIONAL INFO REQUIRED status.";
      reviewModalError.hidden = false;
      return;
    }

    reviewSubmitBtn.disabled = true;
    reviewSubmitBtn.innerHTML = `<span class="loading-spinner"></span> Processing...`;

    try {
      await api.put(`customs-clearances/${id}/status`, { status, remarks });
      toastFn(`Customs status updated to ${status}.`);
      closeReviewModal();
      loadCustoms(pageNum);
    } catch (error) {
      reviewModalError.textContent = error.message;
      reviewModalError.hidden = false;
    } finally {
      reviewSubmitBtn.disabled = false;
      reviewSubmitBtn.textContent = "Submit Officer Decision";
    }
  }

  async function handleDelete(id) {
    if (!confirm(`Are you sure you want to delete Declaration #${id}?`)) return;

    try {
      await api.delete(`customs-clearances/${id}`);
      toastFn(`Declaration #${id} deleted.`);
      loadCustoms(pageNum);
    } catch (error) {
      alert(`Delete failed: ${error.message}`);
    }
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  container.querySelector("#btn-add-customs").addEventListener("click", openCreateModal);
  container.querySelector("#btn-refresh-customs").addEventListener("click", () => loadCustoms(pageNum));
  container.querySelector("#modal-close-btn").addEventListener("click", closeModal);
  container.querySelector("#modal-cancel-btn").addEventListener("click", closeModal);
  customsForm.addEventListener("submit", handleFormSubmit);

  container.querySelector("#review-modal-close-btn").addEventListener("click", closeReviewModal);
  container.querySelector("#review-modal-cancel-btn").addEventListener("click", closeReviewModal);
  container.querySelector("#customs-review-form").addEventListener("submit", handleReviewSubmit);

  statusFilterSelect.addEventListener("change", () => {
    currentStatusFilter = statusFilterSelect.value;
    loadCustoms(0);
  });

  prevBtn.addEventListener("click", () => { if (pageNum > 0) loadCustoms(pageNum - 1); });
  nextBtn.addEventListener("click", () => { if (pageNum < totalPages - 1) loadCustoms(pageNum + 1); });

  const searchInput = container.querySelector("#customs-search-input");
  searchInput.addEventListener("input", () => {
    const query = searchInput.value.toLowerCase().trim();
    if (!query) { renderTable(customsData); return; }
    renderTable(customsData.filter(c =>
      (c.declarationNumber && c.declarationNumber.toLowerCase().includes(query)) ||
      (c.status && c.status.toLowerCase().includes(query))
    ));
  });

  loadCustoms(0);
}
