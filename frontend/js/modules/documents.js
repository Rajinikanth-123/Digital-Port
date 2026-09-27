import { api } from "../api/api.js";

export function initDocumentsModule(container, toastFn) {
  let documentsData = [];
  let shipmentsList = [];
  let customsList = [];
  let pageNum = 0;
  let totalPages = 1;
  let currentDocTypeFilter = "ALL";

  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / DOCUMENTS</p>
        <h1>Shipping & Customs Documents</h1>
        <p>Secure document repository for Bills of Lading, Customs Declarations, and Manifests.</p>
      </div>
      <button class="button primary" id="btn-upload-doc">⬆ Upload Document</button>
    </section>

    <section class="stats-grid compact">
      <article class="stat-card">
        <div class="stat-icon blue">📁</div>
        <div><span>Total Documents</span><strong id="stat-total-docs">-</strong><small>Stored records</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon teal">📄</div>
        <div><span>Bills of Lading</span><strong id="stat-bol-count">-</strong><small>Shipping manifests</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon orange">🛃</div>
        <div><span>Customs Filings</span><strong id="stat-customs-doc-count">-</strong><small>Declarations</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon green">🔒</div>
        <div><span>Access Control</span><strong>RBAC Secure</strong><small>Local Storage Engine</small></div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>Document Index</h2>
          <p>Search, preview, and download verified shipping documentation.</p>
        </div>
        <div class="toolbar" style="display:flex; gap:10px; align-items:center;">
          <select id="doc-type-filter" style="padding:8px 12px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text); font-size:13px;" aria-label="Filter by Document Type">
            <option value="ALL">All Document Types</option>
            <option value="BILL_OF_LADING">Bill of Lading</option>
            <option value="CARGO_MANIFEST">Cargo Manifest</option>
            <option value="CUSTOMS_DECLARATION">Customs Declaration</option>
            <option value="COMMERCIAL_INVOICE">Commercial Invoice</option>
            <option value="CERTIFICATE_OF_ORIGIN">Certificate of Origin</option>
            <option value="PACKING_LIST">Packing List</option>
            <option value="OTHER">Other</option>
          </select>
          <input class="table-search" id="doc-search-input" placeholder="Search file name or type..." aria-label="Search documents">
          <button class="button" id="btn-refresh-docs">↻ Refresh</button>
        </div>
      </div>
      <div class="table-wrap">
        <table id="docs-table">
          <thead>
            <tr>
              <th data-sort>ID</th>
              <th data-sort>File Name ↕</th>
              <th data-sort>Type ↕</th>
              <th>MIME / Size</th>
              <th>Linked Entity</th>
              <th>Uploaded By</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="docs-table-body">
            <tr><td colspan="7" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading document repository...</td></tr>
          </tbody>
        </table>
        <div class="empty-state" id="docs-empty-state" hidden>No document records found matching your query.</div>
      </div>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-top:16px; font-size:12px; color:var(--muted);">
        <span id="docs-page-info">Showing page 1</span>
        <div>
          <button class="button" id="btn-prev-page" disabled>Previous</button>
          <button class="button" id="btn-next-page" disabled style="margin-left:6px;">Next</button>
        </div>
      </div>
    </section>

    <!-- Modal for Uploading Document -->
    <div class="modal-overlay" id="doc-upload-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3 id="modal-title">Upload Shipping Document</h3>
          <button class="modal-close" id="modal-close-btn">&times;</button>
        </div>
        <div class="error-banner" id="modal-error" hidden></div>
        <form id="doc-upload-form">
          <div class="modal-body">
            <label class="field">
              <span>Select File *</span>
              <input type="file" id="doc-file-input" required style="padding:10px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
              <small style="color:var(--muted); margin-top:4px; display:block;">Allowed formats: PDF, PNG, JPG, DOCX, XLSX, CSV, TXT (Max size: 10 MB)</small>
            </label>
            <label class="field">
              <span>Document Type *</span>
              <select id="doc-type-input" required style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
                <option value="BILL_OF_LADING">Bill of Lading</option>
                <option value="CARGO_MANIFEST">Cargo Manifest</option>
                <option value="CUSTOMS_DECLARATION">Customs Declaration</option>
                <option value="COMMERCIAL_INVOICE">Commercial Invoice</option>
                <option value="CERTIFICATE_OF_ORIGIN">Certificate of Origin</option>
                <option value="PACKING_LIST">Packing List</option>
                <option value="OTHER">Other</option>
              </select>
            </label>
            <label class="field">
              <span>Linked Shipment (Optional)</span>
              <select id="doc-shipment-id" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);"></select>
            </label>
            <label class="field">
              <span>Linked Customs Clearance (Optional)</span>
              <select id="doc-customs-id" style="padding:11px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);"></select>
            </label>
          </div>
          <div class="modal-footer">
            <button type="button" class="button" id="modal-cancel-btn">Cancel</button>
            <button type="submit" class="button primary" id="modal-submit-btn">⬆ Start Upload</button>
          </div>
        </form>
      </div>
    </div>
  `;

  const tbody = container.querySelector("#docs-table-body");
  const emptyState = container.querySelector("#docs-empty-state");
  const modal = container.querySelector("#doc-upload-modal");
  const modalError = container.querySelector("#modal-error");
  const uploadForm = container.querySelector("#doc-upload-form");
  const fileInput = container.querySelector("#doc-file-input");
  const typeSelect = container.querySelector("#doc-type-input");
  const shipmentSelect = container.querySelector("#doc-shipment-id");
  const customsSelect = container.querySelector("#doc-customs-id");
  const submitBtn = container.querySelector("#modal-submit-btn");

  const statTotal = container.querySelector("#stat-total-docs");
  const statBol = container.querySelector("#stat-bol-count");
  const statCustoms = container.querySelector("#stat-customs-doc-count");

  const pageInfo = container.querySelector("#docs-page-info");
  const prevBtn = container.querySelector("#btn-prev-page");
  const nextBtn = container.querySelector("#btn-next-page");
  const typeFilterSelect = container.querySelector("#doc-type-filter");

  async function loadDropdowns() {
    try {
      const shipRes = await api.get("shipments", { size: 100 });
      shipmentsList = api.unwrap(shipRes);
      shipmentSelect.innerHTML = `<option value="">-- None (Unassigned) --</option>` +
        shipmentsList.map(s => `<option value="${s.id}">${escapeHtml(s.trackingNumber)} (${s.origin} → ${s.destination})</option>`).join("");

      const custRes = await api.get("customs-clearances", { size: 100 });
      customsList = api.unwrap(custRes);
      customsSelect.innerHTML = `<option value="">-- None (Unassigned) --</option>` +
        customsList.map(c => `<option value="${c.id}">${escapeHtml(c.declarationNumber)} (${c.status})</option>`).join("");
    } catch (e) {
      console.warn("Could not load dropdowns for documents module", e);
    }
  }

  async function loadDocs(page = 0) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading document repository...</td></tr>`;
    emptyState.hidden = true;

    try {
      await loadDropdowns();
      const params = { page, size: 20 };
      if (currentDocTypeFilter && currentDocTypeFilter !== "ALL") {
        params.docType = currentDocTypeFilter;
      }

      const response = await api.get("documents", params);
      documentsData = api.unwrap(response);
      pageNum = response.number !== undefined ? response.number : page;
      totalPages = response.totalPages || 1;

      renderTable(documentsData);
      updateStats(response.totalElements || documentsData.length, documentsData);

      pageInfo.textContent = `Page ${pageNum + 1} of ${totalPages} (${response.totalElements || documentsData.length} records)`;
      prevBtn.disabled = pageNum <= 0;
      nextBtn.disabled = pageNum >= totalPages - 1;
    } catch (error) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#e07b65; padding: 24px;">Failed to load document records: ${error.message}</td></tr>`;
      toastFn(`Error: ${error.message}`);
    }
  }

  function updateStats(total, items) {
    statTotal.textContent = total;
    statBol.textContent = items.filter(d => (d.documentType || "").includes("BILL_OF_LADING")).length;
    statCustoms.textContent = items.filter(d => (d.documentType || "").includes("CUSTOMS_DECLARATION")).length;
  }

  function renderTable(items) {
    if (!items || items.length === 0) {
      tbody.innerHTML = "";
      emptyState.hidden = false;
      return;
    }

    emptyState.hidden = true;
    tbody.innerHTML = items.map(d => {
      let linkedRef = "Unassigned";
      if (d.shipment) {
        linkedRef = `Shipment ${d.shipment.trackingNumber || '#' + d.shipment.id}`;
      } else if (d.customsClearance) {
        linkedRef = `Customs ${d.customsClearance.declarationNumber}`;
      }

      const formattedSize = formatBytes(d.fileSizeBytes);
      const uploader = d.uploadedBy ? (d.uploadedBy.fullName || d.uploadedBy.email) : "System";

      return `
        <tr>
          <td><strong>#${d.id}</strong></td>
          <td>
            <strong>${escapeHtml(d.fileName)}</strong>
            ${d.checksumSha256 ? `<br><small style="color:var(--muted); font-size:10px;">SHA256: ${d.checksumSha256.substring(0, 12)}...</small>` : ''}
          </td>
          <td><span class="status delivered">${escapeHtml(d.documentType || "DOCUMENT")}</span></td>
          <td><small>${escapeHtml(d.mimeType || "application/octet-stream")}</small><br><strong>${formattedSize}</strong></td>
          <td><small>${escapeHtml(linkedRef)}</small></td>
          <td><small>${escapeHtml(uploader)}</small></td>
          <td>
            <button class="table-action btn-download-doc" data-id="${d.id}" style="background:var(--accent-dim); color:var(--accent); border:1px solid var(--accent);">📥 Download</button>
            <button class="table-action danger btn-delete-doc" data-id="${d.id}">Delete</button>
          </td>
        </tr>
      `;
    }).join("");

    tbody.querySelectorAll(".btn-download-doc").forEach(btn => {
      btn.addEventListener("click", () => handleDownload(btn.dataset.id));
    });

    tbody.querySelectorAll(".btn-delete-doc").forEach(btn => {
      btn.addEventListener("click", () => handleDelete(btn.dataset.id));
    });
  }

  function openUploadModal() {
    uploadForm.reset();
    typeSelect.value = "BILL_OF_LADING";
    modalError.hidden = true;
    modalError.textContent = "";
    modal.classList.add("show");
  }

  function closeModal() { modal.classList.remove("show"); }

  async function handleUploadSubmit(e) {
    e.preventDefault();
    modalError.hidden = true;

    const files = fileInput.files;
    if (!files || files.length === 0) {
      modalError.textContent = "Please select a file to upload.";
      modalError.hidden = false;
      return;
    }

    const file = files[0];
    if (file.size > 10 * 1024 * 1024) {
      modalError.textContent = "Selected file size exceeds maximum limit of 10 MB.";
      modalError.hidden = false;
      return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("documentType", typeSelect.value);
    if (shipmentSelect.value) formData.append("shipmentId", shipmentSelect.value);
    if (customsSelect.value) formData.append("customsClearanceId", customsSelect.value);

    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="loading-spinner"></span> Uploading...`;

    try {
      // Use native fetch with credentials for multipart upload
      const response = await fetch("/api/documents/upload", {
        method: "POST",
        body: formData,
        headers: getCsrfHeader()
      });

      if (!response.ok) {
        let errMessage = "Upload failed";
        try {
          const json = await response.json();
          errMessage = json.message || json.error || errMessage;
        } catch (_) {}
        throw new Error(errMessage);
      }

      toastFn(`File '${file.name}' uploaded successfully!`);
      closeModal();
      loadDocs(pageNum);
    } catch (error) {
      modalError.textContent = error.message;
      modalError.hidden = false;
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "⬆ Start Upload";
    }
  }

  async function handleDownload(id) {
    const d = documentsData.find(item => String(item.id) === String(id));
    const fileName = d ? d.fileName : `document_${id}.bin`;

    try {
      toastFn(`Initiating download for ${fileName}...`);
      const response = await fetch(`/api/documents/${id}/download`, {
        method: "GET",
        headers: getCsrfHeader()
      });

      if (!response.ok) {
        throw new Error("Failed to download document. HTTP status " + response.status);
      }

      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = downloadUrl;
      a.download = fileName;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(downloadUrl);
      toastFn(`Download complete: ${fileName}`);
    } catch (error) {
      alert(`Download error: ${error.message}`);
    }
  }

  async function handleDelete(id) {
    if (!confirm(`Are you sure you want to delete Document #${id}? This will remove the file from storage.`)) return;

    try {
      await api.delete(`documents/${id}`);
      toastFn(`Document #${id} deleted.`);
      loadDocs(pageNum);
    } catch (error) {
      alert(`Delete failed: ${error.message}`);
    }
  }

  function getCsrfHeader() {
    const cookies = document.cookie.split(';');
    for (let c of cookies) {
      const [name, val] = c.trim().split('=');
      if (name === 'XSRF-TOKEN') {
        return { 'X-XSRF-TOKEN': decodeURIComponent(val) };
      }
    }
    return {};
  }

  function formatBytes(bytes) {
    if (!bytes || bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  container.querySelector("#btn-upload-doc").addEventListener("click", openUploadModal);
  container.querySelector("#btn-refresh-docs").addEventListener("click", () => loadDocs(pageNum));
  container.querySelector("#modal-close-btn").addEventListener("click", closeModal);
  container.querySelector("#modal-cancel-btn").addEventListener("click", closeModal);
  uploadForm.addEventListener("submit", handleUploadSubmit);

  typeFilterSelect.addEventListener("change", () => {
    currentDocTypeFilter = typeFilterSelect.value;
    loadDocs(0);
  });

  prevBtn.addEventListener("click", () => { if (pageNum > 0) loadDocs(pageNum - 1); });
  nextBtn.addEventListener("click", () => { if (pageNum < totalPages - 1) loadDocs(pageNum + 1); });

  const searchInput = container.querySelector("#doc-search-input");
  searchInput.addEventListener("input", () => {
    const query = searchInput.value.toLowerCase().trim();
    if (!query) { renderTable(documentsData); return; }
    renderTable(documentsData.filter(d =>
      (d.fileName && d.fileName.toLowerCase().includes(query)) ||
      (d.documentType && d.documentType.toLowerCase().includes(query))
    ));
  });

  loadDocs(0);
}
