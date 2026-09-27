import { api } from "../api/api.js";

export function initInvoicesModule(container, toastFn) {
  let invoicesData = [];
  let shipmentsList = [];
  let pageNum = 0;
  let totalPages = 1;
  let statusFilterVal = "";

  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / INVOICES</p>
        <h1>Invoices & Commercial Billing</h1>
        <p>Manage port dues, customer billing, itemized charges, and payment status tracking.</p>
      </div>
      <button class="button primary" id="btn-add-invoice">＋ Issue Invoice</button>
    </section>

    <section class="stats-grid compact">
      <article class="stat-card">
        <div class="stat-icon purple">$</div>
        <div><span>Total Invoiced</span><strong id="stat-total-invoiced">$0.00</strong><small>Billed total</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon teal">✓</div>
        <div><span>Paid Invoices</span><strong id="stat-paid-count">-</strong><small>Settled</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon orange">◷</div>
        <div><span>API Status</span><strong>Connected</strong><small>Spring Boot / MySQL</small></div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>Invoice Registry</h2>
          <p>Search, filter, and download customer billing statements.</p>
        </div>
        <div class="toolbar" style="display:flex; gap:10px; align-items:center;">
          <select id="invoice-status-filter" style="padding:9px 12px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text); font-size:13px;">
            <option value="">All Payment Statuses</option>
            <option value="PENDING">PENDING</option>
            <option value="PARTIALLY_PAID">PARTIALLY_PAID</option>
            <option value="PAID">PAID</option>
            <option value="OVERDUE">OVERDUE</option>
            <option value="CANCELLED">CANCELLED</option>
          </select>
          <input class="table-search" id="invoice-search-input" placeholder="Search invoice or customer..." aria-label="Search invoices">
          <button class="button" id="btn-refresh-invoices">↻ Refresh</button>
        </div>
      </div>
      <div class="table-wrap">
        <table id="invoices-table">
          <thead>
            <tr>
              <th data-sort>ID</th>
              <th data-sort>Invoice No. ↕</th>
              <th data-sort>Customer Name ↕</th>
              <th>Charges ($)</th>
              <th>Taxes ($)</th>
              <th>Total ($)</th>
              <th>Due Date</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="invoices-table-body">
            <tr><td colspan="9" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading invoices...</td></tr>
          </tbody>
        </table>
        <div class="empty-state" id="invoices-empty-state" hidden>No invoice records found.</div>
      </div>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-top:16px; font-size:12px; color:var(--muted);">
        <span id="invoices-page-info">Showing page 1</span>
        <div>
          <button class="button" id="btn-prev-page" disabled>Previous</button>
          <button class="button" id="btn-next-page" disabled style="margin-left:6px;">Next</button>
        </div>
      </div>
    </section>

    <!-- Modal for Create / Edit Invoice -->
    <div class="modal-overlay" id="invoice-modal">
      <div class="modal-card" style="max-width: 680px; width: 95%;">
        <div class="modal-header">
          <h3 id="modal-title">Issue Customer Invoice</h3>
          <button class="modal-close" id="modal-close-btn">&times;</button>
        </div>
        <div class="error-banner" id="modal-error" hidden></div>
        <form id="invoice-form">
          <input type="hidden" id="invoice-id">
          <div class="modal-body">
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
              <label class="field">
                <span>Invoice Number (Auto if blank)</span>
                <input type="text" id="invoice-number" placeholder="e.g. INV-2026-0045">
              </label>
              <label class="field">
                <span>Customer Name *</span>
                <input type="text" id="invoice-customer" required placeholder="e.g. Global Maritime Corp">
              </label>
            </div>
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
              <label class="field">
                <span>Linked Shipment (Optional)</span>
                <select id="invoice-shipment-id" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);"></select>
              </label>
              <label class="field">
                <span>Currency</span>
                <select id="invoice-currency" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                  <option value="USD">USD ($)</option>
                  <option value="EUR">EUR (€)</option>
                  <option value="GBP">GBP (£)</option>
                </select>
              </label>
            </div>

            <!-- Itemized Line Items Container -->
            <div style="margin-top:12px; padding:12px; background:var(--surface-subtle); border-radius:8px; border:1px solid var(--line);">
              <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
                <strong style="font-size:13px;">Itemized Service Line Items</strong>
                <button type="button" class="button" id="btn-add-line-item" style="padding:4px 10px; font-size:12px;">＋ Add Item</button>
              </div>
              <div id="line-items-container">
                <!-- Line Item Rows Dynamic Injection -->
              </div>
            </div>

            <div style="display:grid; grid-template-columns: 1fr 1fr 1fr; gap:10px; margin-top:12px;">
              <label class="field">
                <span>Subtotal Charges ($)</span>
                <input type="number" step="0.01" id="invoice-charges" value="2500.00">
              </label>
              <label class="field">
                <span>Taxes ($)</span>
                <input type="number" step="0.01" id="invoice-taxes" value="250.00">
              </label>
              <label class="field">
                <span>Discount ($)</span>
                <input type="number" step="0.01" id="invoice-discount" value="0.00">
              </label>
            </div>
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
              <label class="field">
                <span>Due Date *</span>
                <input type="date" id="invoice-duedate" required>
              </label>
              <label class="field">
                <span>Payment Status</span>
                <select id="invoice-status" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                  <option value="PENDING">PENDING</option>
                  <option value="PARTIALLY_PAID">PARTIALLY_PAID</option>
                  <option value="PAID">PAID</option>
                  <option value="OVERDUE">OVERDUE</option>
                  <option value="CANCELLED">CANCELLED</option>
                </select>
              </label>
            </div>
            <label class="field" style="margin-top:8px;">
              <span>Invoice Notes / Payment Instructions</span>
              <textarea id="invoice-notes" rows="2" placeholder="e.g. Payment due within 30 days via wire transfer." style="padding:8px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);"></textarea>
            </label>
          </div>
          <div class="modal-footer">
            <button type="button" class="button" id="modal-cancel-btn">Cancel</button>
            <button type="submit" class="button primary" id="modal-submit-btn">Save Invoice</button>
          </div>
        </form>
      </div>
    </div>
  `;

  const tbody = container.querySelector("#invoices-table-body");
  const emptyState = container.querySelector("#invoices-empty-state");
  const modal = container.querySelector("#invoice-modal");
  const modalTitle = container.querySelector("#modal-title");
  const modalError = container.querySelector("#modal-error");
  const invoiceForm = container.querySelector("#invoice-form");
  const invoiceIdInput = container.querySelector("#invoice-id");
  const invoiceNumberInput = container.querySelector("#invoice-number");
  const customerInput = container.querySelector("#invoice-customer");
  const shipmentSelect = container.querySelector("#invoice-shipment-id");
  const currencySelect = container.querySelector("#invoice-currency");
  const lineItemsContainer = container.querySelector("#line-items-container");
  const addLineItemBtn = container.querySelector("#btn-add-line-item");
  const chargesInput = container.querySelector("#invoice-charges");
  const taxesInput = container.querySelector("#invoice-taxes");
  const discountInput = container.querySelector("#invoice-discount");
  const dueDateInput = container.querySelector("#invoice-duedate");
  const statusInput = container.querySelector("#invoice-status");
  const notesInput = container.querySelector("#invoice-notes");
  const submitBtn = container.querySelector("#modal-submit-btn");

  const statInvoiced = container.querySelector("#stat-total-invoiced");
  const statPaid = container.querySelector("#stat-paid-count");
  const pageInfo = container.querySelector("#invoices-page-info");
  const prevBtn = container.querySelector("#btn-prev-page");
  const nextBtn = container.querySelector("#btn-next-page");
  const statusFilterSelect = container.querySelector("#invoice-status-filter");

  async function loadShipmentsDropdown() {
    try {
      const res = await api.get("shipments", { size: 100 });
      shipmentsList = api.unwrap(res);
      shipmentSelect.innerHTML = `<option value="">-- None (Unassigned) --</option>` +
        shipmentsList.map(s => `<option value="${s.id}">${escapeHtml(s.trackingNumber)} (${s.origin} → ${s.destination})</option>`).join("");
    } catch (e) {
      console.warn("Could not load shipments dropdown for invoices", e);
    }
  }

  async function loadInvoices(page = 0) {
    tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading invoices...</td></tr>`;
    emptyState.hidden = true;

    try {
      await loadShipmentsDropdown();
      const searchVal = container.querySelector("#invoice-search-input").value.trim();
      const params = { page, size: 20 };
      if (searchVal) params.search = searchVal;
      if (statusFilterVal) params.status = statusFilterVal;

      const response = await api.get("invoices", params);
      invoicesData = api.unwrap(response);
      pageNum = response.number !== undefined ? response.number : page;
      totalPages = response.totalPages || 1;

      renderTable(invoicesData);
      updateStats(invoicesData);

      pageInfo.textContent = `Page ${pageNum + 1} of ${totalPages} (${response.totalElements || invoicesData.length} records)`;
      prevBtn.disabled = pageNum <= 0;
      nextBtn.disabled = pageNum >= totalPages - 1;
    } catch (error) {
      tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; color:#e07b65; padding: 24px;">Failed to load invoices: ${error.message}</td></tr>`;
      toastFn(`Error: ${error.message}`);
    }
  }

  function updateStats(items) {
    const totalSum = items.reduce((acc, inv) => acc + (inv.total || (((inv.charges || 0) + (inv.taxes || 0)) - (inv.discount || 0))), 0);
    statInvoiced.textContent = `$${totalSum.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    statPaid.textContent = items.filter(i => i.paymentStatus === "PAID").length;
  }

  function renderTable(items) {
    if (!items || items.length === 0) {
      tbody.innerHTML = "";
      emptyState.hidden = false;
      return;
    }

    emptyState.hidden = true;
    tbody.innerHTML = items.map(inv => {
      const chg = (inv.charges || 0).toFixed(2);
      const tx = (inv.taxes || 0).toFixed(2);
      const disc = (inv.discount || 0).toFixed(2);
      const tot = (inv.total !== undefined ? inv.total : ((inv.charges || 0) + (inv.taxes || 0) - (inv.discount || 0))).toFixed(2);

      let statusClass = "delivered";
      if (inv.paymentStatus === "PENDING") statusClass = "customs-hold";
      if (inv.paymentStatus === "PARTIALLY_PAID") statusClass = "in-transit";
      if (inv.paymentStatus === "OVERDUE") statusClass = "delayed";
      if (inv.paymentStatus === "CANCELLED") statusClass = "exception";

      return `
        <tr>
          <td><strong>#${inv.id}</strong></td>
          <td><strong>${escapeHtml(inv.invoiceNumber)}</strong></td>
          <td>${escapeHtml(inv.customerName)}</td>
          <td>$${chg}</td>
          <td>$${tx}</td>
          <td><strong>$${tot}</strong></td>
          <td><small>${escapeHtml(inv.dueDate || "-")}</small></td>
          <td><span class="status ${statusClass}">${escapeHtml(inv.paymentStatus || "PENDING")}</span></td>
          <td style="display:flex; gap:4px;">
            <button class="table-action btn-pdf-invoice" data-id="${inv.id}">📥 PDF</button>
            <button class="table-action btn-edit-invoice" data-id="${inv.id}">Edit</button>
            <button class="table-action danger btn-cancel-invoice" data-id="${inv.id}" ${inv.paymentStatus === "CANCELLED" || inv.paymentStatus === "PAID" ? "disabled" : ""}>Cancel</button>
          </td>
        </tr>
      `;
    }).join("");

    tbody.querySelectorAll(".btn-pdf-invoice").forEach(btn => {
      btn.addEventListener("click", () => handleDownloadPdf(btn.dataset.id));
    });

    tbody.querySelectorAll(".btn-edit-invoice").forEach(btn => {
      btn.addEventListener("click", () => openEditModal(btn.dataset.id));
    });

    tbody.querySelectorAll(".btn-cancel-invoice").forEach(btn => {
      btn.addEventListener("click", () => handleCancel(btn.dataset.id));
    });
  }

  function addLineItemRow(desc = "", qty = 1, price = 0.00) {
    const row = document.createElement("div");
    row.className = "line-item-row";
    row.style.cssText = "display:grid; grid-template-columns: 3fr 1fr 1.5fr auto; gap:6px; margin-bottom:6px; align-items:center;";
    row.innerHTML = `
      <input type="text" class="item-desc" placeholder="Item description" value="${escapeHtml(desc)}" required style="padding:6px; border:1px solid var(--line); border-radius:6px; font-size:12px;">
      <input type="number" class="item-qty" placeholder="Qty" value="${qty}" step="any" min="1" required style="padding:6px; border:1px solid var(--line); border-radius:6px; font-size:12px;">
      <input type="number" class="item-price" placeholder="Price" value="${price}" step="0.01" min="0" required style="padding:6px; border:1px solid var(--line); border-radius:6px; font-size:12px;">
      <button type="button" class="btn-remove-line" style="background:none; border:none; color:var(--danger); cursor:pointer; font-weight:bold; font-size:14px;">&times;</button>
    `;

    row.querySelector(".btn-remove-line").addEventListener("click", () => {
      row.remove();
      recalculateLineTotals();
    });

    row.querySelectorAll("input").forEach(input => {
      input.addEventListener("input", recalculateLineTotals);
    });

    lineItemsContainer.appendChild(row);
  }

  function recalculateLineTotals() {
    let subtotal = 0;
    lineItemsContainer.querySelectorAll(".line-item-row").forEach(row => {
      const qty = parseFloat(row.querySelector(".item-qty").value) || 0;
      const price = parseFloat(row.querySelector(".item-price").value) || 0;
      subtotal += (qty * price);
    });
    if (lineItemsContainer.children.length > 0) {
      chargesInput.value = subtotal.toFixed(2);
    }
  }

  addLineItemBtn.addEventListener("click", () => addLineItemRow("", 1, 500.00));

  function openCreateModal() {
    invoiceForm.reset();
    invoiceIdInput.value = "";
    lineItemsContainer.innerHTML = "";
    addLineItemRow("Port Handling & Vessel Discharge Fees", 1, 2500.00);

    statusInput.value = "PENDING";
    currencySelect.value = "USD";
    chargesInput.value = "2500.00";
    taxesInput.value = "250.00";
    discountInput.value = "0.00";

    const defaultDue = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
    dueDateInput.value = defaultDue;

    modalTitle.textContent = "Issue Commercial Customer Invoice";
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function openEditModal(id) {
    const inv = invoicesData.find(item => String(item.id) === String(id));
    if (!inv) return;

    invoiceForm.reset();
    invoiceIdInput.value = inv.id;
    invoiceNumberInput.value = inv.invoiceNumber || "";
    customerInput.value = inv.customerName || "";
    shipmentSelect.value = inv.shipment ? inv.shipment.id : "";
    currencySelect.value = inv.currency || "USD";
    chargesInput.value = inv.charges || 0;
    taxesInput.value = inv.taxes || 0;
    discountInput.value = inv.discount || 0;
    dueDateInput.value = inv.dueDate || "";
    statusInput.value = inv.paymentStatus || "PENDING";
    notesInput.value = inv.notes || "";

    lineItemsContainer.innerHTML = "";
    if (inv.items && inv.items.length > 0) {
      inv.items.forEach(it => addLineItemRow(it.description, it.quantity, it.unitPrice));
    } else {
      addLineItemRow("Port Service Standard Charges", 1, inv.charges || 0);
    }

    modalTitle.textContent = `Edit Invoice #${inv.id} - ${inv.invoiceNumber}`;
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function closeModal() { modal.classList.remove("show"); }

  async function handleFormSubmit(e) {
    e.preventDefault();
    modalError.hidden = true;

    const id = invoiceIdInput.value;
    const shipmentIdVal = shipmentSelect.value;

    const itemsPayload = [];
    lineItemsContainer.querySelectorAll(".line-item-row").forEach(row => {
      itemsPayload.push({
        description: row.querySelector(".item-desc").value.trim(),
        quantity: parseFloat(row.querySelector(".item-qty").value) || 1,
        unitPrice: parseFloat(row.querySelector(".item-price").value) || 0
      });
    });

    const payload = {
      invoiceNumber: invoiceNumberInput.value.trim().toUpperCase() || null,
      customerName: customerInput.value.trim(),
      shipmentId: shipmentIdVal ? parseInt(shipmentIdVal, 10) : null,
      currency: currencySelect.value,
      charges: parseFloat(chargesInput.value || "0"),
      taxes: parseFloat(taxesInput.value || "0"),
      discount: parseFloat(discountInput.value || "0"),
      dueDate: dueDateInput.value,
      paymentStatus: statusInput.value,
      notes: notesInput.value.trim(),
      items: itemsPayload
    };

    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="loading-spinner"></span> Saving...`;

    try {
      if (id) {
        await api.put(`invoices/${id}`, payload);
        toastFn(`Invoice updated successfully.`);
      } else {
        await api.post("invoices", payload);
        toastFn(`Invoice issued successfully.`);
      }
      closeModal();
      loadInvoices(pageNum);
    } catch (error) {
      modalError.textContent = error.message;
      modalError.hidden = false;
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Save Invoice";
    }
  }

  async function handleCancel(id) {
    if (!confirm(`Are you sure you want to cancel Invoice #${id}?`)) return;

    try {
      await api.put(`invoices/${id}/cancel`, {});
      toastFn(`Invoice #${id} cancelled.`);
      loadInvoices(pageNum);
    } catch (error) {
      alert(`Cancel failed: ${error.message}`);
    }
  }

  async function handleDownloadPdf(id) {
    try {
      const blob = await api.downloadBlob(`invoices/${id}/pdf`);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `Invoice_${id}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
      toastFn(`Invoice PDF downloaded successfully.`);
    } catch (error) {
      toastFn(`Failed to download PDF: ${error.message}`);
    }
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  container.querySelector("#btn-add-invoice").addEventListener("click", openCreateModal);
  container.querySelector("#btn-refresh-invoices").addEventListener("click", () => loadInvoices(pageNum));
  container.querySelector("#modal-close-btn").addEventListener("click", closeModal);
  container.querySelector("#modal-cancel-btn").addEventListener("click", closeModal);
  invoiceForm.addEventListener("submit", handleFormSubmit);

  statusFilterSelect.addEventListener("change", (e) => {
    statusFilterVal = e.target.value;
    loadInvoices(0);
  });

  prevBtn.addEventListener("click", () => { if (pageNum > 0) loadInvoices(pageNum - 1); });
  nextBtn.addEventListener("click", () => { if (pageNum < totalPages - 1) loadInvoices(pageNum + 1); });

  const searchInput = container.querySelector("#invoice-search-input");
  let searchTimeout;
  searchInput.addEventListener("input", () => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => loadInvoices(0), 300);
  });

  loadInvoices(0);
}
