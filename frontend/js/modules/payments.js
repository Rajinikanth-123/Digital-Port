import { api } from "../api/api.js";

export function initPaymentsModule(container, toastFn) {
  let paymentsData = [];
  let invoicesList = [];
  let pageNum = 0;
  let totalPages = 1;

  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / PAYMENTS</p>
        <h1>Payment Receipts</h1>
        <p>Record, audit, and verify payment receipts linked to customer invoices.</p>
      </div>
      <button class="button primary" id="btn-add-payment">＋ Record Payment</button>
    </section>

    <section class="stats-grid compact">
      <article class="stat-card">
        <div class="stat-icon teal">◒</div>
        <div><span>Total Collected</span><strong id="stat-total-paid">$0.00</strong><small>Settled payments</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon blue">✓</div>
        <div><span>Receipts</span><strong id="stat-receipt-count">-</strong><small>Live count</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon orange">◷</div>
        <div><span>API Status</span><strong>Connected</strong><small>Spring Boot / MySQL</small></div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>Payment Activity</h2>
          <p>Search and inspect recorded financial transactions.</p>
        </div>
        <div class="toolbar">
          <input class="table-search" id="payment-search-input" placeholder="Search reference or invoice..." aria-label="Search payments">
          <button class="button" id="btn-refresh-payments">↻ Refresh</button>
        </div>
      </div>
      <div class="table-wrap">
        <table id="payments-table">
          <thead>
            <tr>
              <th data-sort>ID</th>
              <th data-sort>Reference / Tx ↕</th>
              <th data-sort>Invoice ↕</th>
              <th>Customer</th>
              <th>Amount ($)</th>
              <th>Payment Method</th>
              <th>Paid At</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="payments-table-body">
            <tr><td colspan="8" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading payments...</td></tr>
          </tbody>
        </table>
        <div class="empty-state" id="payments-empty-state" hidden>No payment records found.</div>
      </div>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-top:16px; font-size:12px; color:var(--muted);">
        <span id="payments-page-info">Showing page 1</span>
        <div>
          <button class="button" id="btn-prev-page" disabled>Previous</button>
          <button class="button" id="btn-next-page" disabled style="margin-left:6px;">Next</button>
        </div>
      </div>
    </section>

    <!-- Modal for Record Payment -->
    <div class="modal-overlay" id="payment-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3 id="modal-title">Record Payment Receipt</h3>
          <button class="modal-close" id="modal-close-btn">&times;</button>
        </div>
        <div class="error-banner" id="modal-error" hidden></div>
        <form id="payment-form">
          <input type="hidden" id="payment-id">
          <div class="modal-body">
            <label class="field">
              <span>Target Invoice *</span>
              <select id="payment-invoice-id" required style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);"></select>
              <small id="invoice-balance-hint" style="color:var(--muted); font-size:11px; margin-top:2px; display:block;"></small>
            </label>
            <label class="field">
              <span>Payment Reference / Tx Code *</span>
              <input type="text" id="payment-reference" required placeholder="e.g. PAY-2026-9901">
            </label>
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
              <label class="field">
                <span>Amount ($) *</span>
                <input type="number" step="0.01" min="0.01" id="payment-amount" required value="1000.00">
              </label>
              <label class="field">
                <span>Payment Method</span>
                <select id="payment-method" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                  <option value="WIRE_TRANSFER">Wire Transfer</option>
                  <option value="CREDIT_CARD">Credit Card</option>
                  <option value="ACH">ACH Direct</option>
                  <option value="CHECK">Company Check</option>
                </select>
              </label>
            </div>
            <label class="field">
              <span>Paid Date / Time *</span>
              <input type="datetime-local" id="payment-paid-at" required>
            </label>
          </div>
          <div class="modal-footer">
            <button type="button" class="button" id="modal-cancel-btn">Cancel</button>
            <button type="submit" class="button primary" id="modal-submit-btn">Record Payment</button>
          </div>
        </form>
      </div>
    </div>
  `;

  const tbody = container.querySelector("#payments-table-body");
  const emptyState = container.querySelector("#payments-empty-state");
  const modal = container.querySelector("#payment-modal");
  const modalTitle = container.querySelector("#modal-title");
  const modalError = container.querySelector("#modal-error");
  const paymentForm = container.querySelector("#payment-form");
  const paymentIdInput = container.querySelector("#payment-id");
  const invoiceSelect = container.querySelector("#payment-invoice-id");
  const balanceHint = container.querySelector("#invoice-balance-hint");
  const referenceInput = container.querySelector("#payment-reference");
  const amountInput = container.querySelector("#payment-amount");
  const methodInput = container.querySelector("#payment-method");
  const paidAtInput = container.querySelector("#payment-paid-at");
  const submitBtn = container.querySelector("#modal-submit-btn");

  const statCollected = container.querySelector("#stat-total-paid");
  const statCount = container.querySelector("#stat-receipt-count");
  const pageInfo = container.querySelector("#payments-page-info");
  const prevBtn = container.querySelector("#btn-prev-page");
  const nextBtn = container.querySelector("#btn-next-page");

  async function loadInvoicesDropdown() {
    try {
      const res = await api.get("invoices", { size: 100 });
      invoicesList = api.unwrap(res);
      invoiceSelect.innerHTML = `<option value="">-- Select Target Invoice --</option>` +
        invoicesList.filter(inv => inv.paymentStatus !== "CANCELLED").map(inv => {
          const totalVal = inv.total !== undefined ? inv.total : ((inv.charges || 0) + (inv.taxes || 0) - (inv.discount || 0));
          return `<option value="${inv.id}">${escapeHtml(inv.invoiceNumber)} - ${escapeHtml(inv.customerName)} ($${totalVal.toFixed(2)} - ${inv.paymentStatus})</option>`;
        }).join("");
    } catch (e) {
      console.warn("Could not load invoices dropdown for payments", e);
    }
  }

  invoiceSelect.addEventListener("change", () => {
    const selectedId = invoiceSelect.value;
    const inv = invoicesList.find(i => String(i.id) === String(selectedId));
    if (inv) {
      const totalVal = inv.total !== undefined ? inv.total : ((inv.charges || 0) + (inv.taxes || 0) - (inv.discount || 0));
      balanceHint.textContent = `Invoice Billed Total: $${totalVal.toFixed(2)} | Current Status: ${inv.paymentStatus}`;
      amountInput.value = totalVal.toFixed(2);
    } else {
      balanceHint.textContent = "";
    }
  });

  async function loadPayments(page = 0) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading payments...</td></tr>`;
    emptyState.hidden = true;

    try {
      await loadInvoicesDropdown();
      const searchVal = container.querySelector("#payment-search-input").value.trim();
      const params = { page, size: 20 };
      if (searchVal) params.search = searchVal;

      const response = await api.get("payments", params);
      paymentsData = api.unwrap(response);
      pageNum = response.number !== undefined ? response.number : page;
      totalPages = response.totalPages || 1;

      renderTable(paymentsData);
      updateStats(response.totalElements || paymentsData.length, paymentsData);

      pageInfo.textContent = `Page ${pageNum + 1} of ${totalPages} (${response.totalElements || paymentsData.length} records)`;
      prevBtn.disabled = pageNum <= 0;
      nextBtn.disabled = pageNum >= totalPages - 1;
    } catch (error) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:#e07b65; padding: 24px;">Failed to load payments: ${error.message}</td></tr>`;
      toastFn(`Error: ${error.message}`);
    }
  }

  function updateStats(total, items) {
    statCount.textContent = total;
    const sum = items.reduce((acc, p) => acc + (p.amount || 0), 0);
    statCollected.textContent = `$${sum.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  function renderTable(items) {
    if (!items || items.length === 0) {
      tbody.innerHTML = "";
      emptyState.hidden = false;
      return;
    }

    emptyState.hidden = true;
    tbody.innerHTML = items.map(p => {
      const invNum = p.invoice ? `${p.invoice.invoiceNumber}` : "Unassigned";
      const custName = p.invoice ? p.invoice.customerName : "-";
      const paidStr = p.paidAt ? new Date(p.paidAt).toLocaleString() : "-";
      const amtStr = (p.amount || 0).toFixed(2);

      return `
        <tr>
          <td><strong>#${p.id}</strong></td>
          <td><strong>${escapeHtml(p.reference)}</strong></td>
          <td>${escapeHtml(invNum)}</td>
          <td>${escapeHtml(custName)}</td>
          <td><strong>$${amtStr}</strong></td>
          <td><span class="status delivered">${escapeHtml(p.paymentMethod || "WIRE_TRANSFER")}</span></td>
          <td><small>${paidStr}</small></td>
          <td>
            <button class="table-action danger btn-delete-payment" data-id="${p.id}">Delete</button>
          </td>
        </tr>
      `;
    }).join("");

    tbody.querySelectorAll(".btn-delete-payment").forEach(btn => {
      btn.addEventListener("click", () => handleDelete(btn.dataset.id));
    });
  }

  function openCreateModal() {
    paymentForm.reset();
    paymentIdInput.value = "";
    balanceHint.textContent = "";
    methodInput.value = "WIRE_TRANSFER";
    referenceInput.value = "PAY-" + Date.now().toString().slice(-6);
    amountInput.value = "1000.00";
    paidAtInput.value = new Date().toISOString().slice(0, 16);

    modalTitle.textContent = "Record Payment Receipt";
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function closeModal() { modal.classList.remove("show"); }

  async function handleFormSubmit(e) {
    e.preventDefault();
    modalError.hidden = true;

    const id = paymentIdInput.value;
    const invId = invoiceSelect.value;
    if (!invId) {
      modalError.textContent = "Please select a target invoice.";
      modalError.hidden = false;
      return;
    }

    const payload = {
      invoiceId: parseInt(invId, 10),
      reference: referenceInput.value.trim().toUpperCase(),
      amount: parseFloat(amountInput.value || "0"),
      paymentMethod: methodInput.value,
      paidAt: new Date(paidAtInput.value).toISOString()
    };

    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="loading-spinner"></span> Saving...`;

    try {
      if (id) {
        await api.put(`payments/${id}`, payload);
        toastFn(`Payment receipt updated successfully.`);
      } else {
        await api.post("payments", payload);
        toastFn(`Payment recorded successfully.`);
      }
      closeModal();
      loadPayments(pageNum);
    } catch (error) {
      modalError.textContent = error.message;
      modalError.hidden = false;
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Record Payment";
    }
  }

  async function handleDelete(id) {
    if (!confirm(`Are you sure you want to delete Payment Receipt #${id}?`)) return;

    try {
      await api.delete(`payments/${id}`);
      toastFn(`Payment receipt #${id} deleted.`);
      loadPayments(pageNum);
    } catch (error) {
      alert(`Delete failed: ${error.message}`);
    }
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  container.querySelector("#btn-add-payment").addEventListener("click", openCreateModal);
  container.querySelector("#btn-refresh-payments").addEventListener("click", () => loadPayments(pageNum));
  container.querySelector("#modal-close-btn").addEventListener("click", closeModal);
  container.querySelector("#modal-cancel-btn").addEventListener("click", closeModal);
  paymentForm.addEventListener("submit", handleFormSubmit);

  prevBtn.addEventListener("click", () => { if (pageNum > 0) loadPayments(pageNum - 1); });
  nextBtn.addEventListener("click", () => { if (pageNum < totalPages - 1) loadPayments(pageNum + 1); });

  const searchInput = container.querySelector("#payment-search-input");
  let searchTimeout;
  searchInput.addEventListener("input", () => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => loadPayments(0), 300);
  });

  loadPayments(0);
}
