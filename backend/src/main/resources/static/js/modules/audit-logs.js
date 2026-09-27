import { api } from "../api/api.js";

let currentPage = 0;
let pageSize = 15;
let totalPages = 1;
let currentFilters = {};

export async function initAuditLogsModule(container, toast) {
  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">SYSTEM SECURITY & AUDIT</p>
        <h1>Audit Logs & System Activity</h1>
        <p>Comprehensive security activity logging, authentication events, and field-level change history.</p>
      </div>
    </section>

    <!-- Summary Metrics Cards -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px;">
      <div class="panel" style="padding: 18px; border-left: 4px solid var(--accent, #e07b65);">
        <small style="color: var(--muted); font-size: 11px; text-transform: uppercase; font-weight: 700;">Total Events</small>
        <h2 id="stat-total-events" style="margin: 6px 0 0 0; font-size: 26px;">--</h2>
      </div>
      <div class="panel" style="padding: 18px; border-left: 4px solid #2ecc71;">
        <small style="color: var(--muted); font-size: 11px; text-transform: uppercase; font-weight: 700;">Successful Actions</small>
        <h2 id="stat-success-events" style="margin: 6px 0 0 0; font-size: 26px; color: #2ecc71;">--</h2>
      </div>
      <div class="panel" style="padding: 18px; border-left: 4px solid #e74c3c;">
        <small style="color: var(--muted); font-size: 11px; text-transform: uppercase; font-weight: 700;">Failed Actions</small>
        <h2 id="stat-failure-events" style="margin: 6px 0 0 0; font-size: 26px; color: #e74c3c;">--</h2>
      </div>
      <div class="panel" style="padding: 18px; border-left: 4px solid #3498db;">
        <small style="color: var(--muted); font-size: 11px; text-transform: uppercase; font-weight: 700;">Active Actors</small>
        <h2 id="stat-active-actors" style="margin: 6px 0 0 0; font-size: 26px; color: #3498db;">--</h2>
      </div>
    </div>

    <!-- Filter Control Panel -->
    <section class="panel" style="margin-bottom: 24px; padding: 20px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px;">
        <h3 style="margin: 0; font-size: 16px;">Search & Filter Activity</h3>
        <button id="btn-audit-reset" class="btn-secondary" style="font-size: 12px; padding: 6px 12px;">Reset Filters</button>
      </div>

      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; align-items: end;">
        <div>
          <label style="font-size: 12px; color: var(--muted); display: block; margin-bottom: 4px;">Start Date</label>
          <input type="date" id="filter-start-date" style="width: 100%; padding: 8px; border: 1px solid var(--line); border-radius: 6px; background: var(--surface-light); color: var(--text);">
        </div>

        <div>
          <label style="font-size: 12px; color: var(--muted); display: block; margin-bottom: 4px;">End Date</label>
          <input type="date" id="filter-end-date" style="width: 100%; padding: 8px; border: 1px solid var(--line); border-radius: 6px; background: var(--surface-light); color: var(--text);">
        </div>

        <div>
          <label style="font-size: 12px; color: var(--muted); display: block; margin-bottom: 4px;">Module / Entity</label>
          <select id="filter-module" style="width: 100%; padding: 8px; border: 1px solid var(--line); border-radius: 6px; background: var(--surface-light); color: var(--text);">
            <option value="">All Modules</option>
            <option value="Auth">Auth & Login</option>
            <option value="User">User Management</option>
            <option value="Shipment">Shipments</option>
            <option value="Invoice">Invoices</option>
            <option value="Payment">Payments</option>
            <option value="CustomsClearance">Customs Clearance</option>
            <option value="Vessel">Vessels</option>
            <option value="Container">Containers</option>
            <option value="Port">Ports</option>
          </select>
        </div>

        <div>
          <label style="font-size: 12px; color: var(--muted); display: block; margin-bottom: 4px;">Outcome</label>
          <select id="filter-outcome" style="width: 100%; padding: 8px; border: 1px solid var(--line); border-radius: 6px; background: var(--surface-light); color: var(--text);">
            <option value="">All Outcomes</option>
            <option value="SUCCESS">Success</option>
            <option value="FAILURE">Failure</option>
          </select>
        </div>

        <div>
          <label style="font-size: 12px; color: var(--muted); display: block; margin-bottom: 4px;">Action Name</label>
          <input type="text" id="filter-action" placeholder="e.g. LOGIN_SUCCESS" style="width: 100%; padding: 8px; border: 1px solid var(--line); border-radius: 6px; background: var(--surface-light); color: var(--text);">
        </div>

        <div>
          <button id="btn-audit-search" class="btn-primary" style="width: 100%; padding: 9px; font-weight: 600;">Apply Filters</button>
        </div>
      </div>
    </section>

    <!-- Audit Log Table -->
    <section class="panel">
      <div class="panel-title" style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2 style="margin: 0; font-size: 18px;">Audit Trail</h2>
          <p style="margin: 4px 0 0 0; color: var(--muted); font-size: 12px;">Immutable, server-verified event audit history.</p>
        </div>
        <div id="pagination-info" style="font-size: 13px; color: var(--muted);"></div>
      </div>

      <div class="table-wrap" style="margin-top: 16px;">
        <table class="data-table" style="width: 100%; border-collapse: collapse;">
          <thead>
            <tr style="border-bottom: 2px solid var(--line); text-align: left; font-size: 12px; color: var(--muted);">
              <th style="padding: 10px;">Timestamp</th>
              <th style="padding: 10px;">Actor</th>
              <th style="padding: 10px;">Action</th>
              <th style="padding: 10px;">Module</th>
              <th style="padding: 10px;">Record ID</th>
              <th style="padding: 10px;">Outcome</th>
              <th style="padding: 10px;">Description</th>
              <th style="padding: 10px; text-align: right;">Details</th>
            </tr>
          </thead>
          <tbody id="audit-table-body">
            <tr>
              <td colspan="8" style="text-align: center; padding: 24px; color: var(--muted);">Loading audit logs...</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination Footer -->
      <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--line);">
        <button id="btn-prev-page" class="btn-secondary" style="padding: 6px 14px; font-size: 12px;">← Previous</button>
        <span id="page-indicator" style="font-size: 13px; font-weight: 600;">Page 1</span>
        <button id="btn-next-page" class="btn-secondary" style="padding: 6px 14px; font-size: 12px;">Next →</button>
      </div>
    </section>

    <!-- Detail Modal -->
    <div id="audit-detail-modal" style="display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.6); z-index: 1000; align-items: center; justify-content: center; padding: 20px;">
      <div style="background: var(--surface); border: 1px solid var(--line); border-radius: 12px; width: 100%; max-width: 650px; max-height: 90vh; overflow-y: auto; box-shadow: 0 16px 40px rgba(0,0,0,0.4); padding: 24px;">
        <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--line); padding-bottom: 14px; margin-bottom: 18px;">
          <h3 style="margin: 0; font-size: 18px;">Audit Record Details</h3>
          <button id="btn-close-modal" style="background: none; border: none; font-size: 20px; color: var(--muted); cursor: pointer;">✕</button>
        </div>

        <div id="modal-content-body" style="font-size: 13px; line-height: 1.6;">
          <!-- Injected via JavaScript -->
        </div>
      </div>
    </div>
  `;

  // Bind Listeners
  document.querySelector("#btn-audit-search").addEventListener("click", () => {
    currentPage = 0;
    fetchAuditLogs(toast);
  });

  document.querySelector("#btn-audit-reset").addEventListener("click", () => {
    document.querySelector("#filter-start-date").value = "";
    document.querySelector("#filter-end-date").value = "";
    document.querySelector("#filter-module").value = "";
    document.querySelector("#filter-outcome").value = "";
    document.querySelector("#filter-action").value = "";
    currentPage = 0;
    fetchAuditLogs(toast);
  });

  document.querySelector("#btn-prev-page").addEventListener("click", () => {
    if (currentPage > 0) {
      currentPage--;
      fetchAuditLogs(toast);
    }
  });

  document.querySelector("#btn-next-page").addEventListener("click", () => {
    if (currentPage < totalPages - 1) {
      currentPage++;
      fetchAuditLogs(toast);
    }
  });

  document.querySelector("#btn-close-modal").addEventListener("click", () => {
    document.querySelector("#audit-detail-modal").style.display = "none";
  });

  document.querySelector("#audit-detail-modal").addEventListener("click", (e) => {
    if (e.target.id === "audit-detail-modal") {
      document.querySelector("#audit-detail-modal").style.display = "none";
    }
  });

  // Initial Data Fetching
  fetchAuditSummary();
  fetchAuditLogs(toast);
}

async function fetchAuditSummary() {
  try {
    const summary = await api.get("audit-logs/summary");
    document.querySelector("#stat-total-events").textContent = summary.totalEvents?.toLocaleString() || "0";
    document.querySelector("#stat-success-events").textContent = summary.successCount?.toLocaleString() || "0";
    document.querySelector("#stat-failure-events").textContent = summary.failureCount?.toLocaleString() || "0";
    document.querySelector("#stat-active-actors").textContent = summary.activeActorsCount?.toLocaleString() || "0";
  } catch (err) {
    console.warn("Failed to load audit summary metrics:", err);
  }
}

async function fetchAuditLogs(toast) {
  const tbody = document.querySelector("#audit-table-body");
  if (!tbody) return;

  tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding:24px; color:var(--muted);"><span class="loading-spinner"></span> Loading audit logs...</td></tr>`;

  const startDate = document.querySelector("#filter-start-date")?.value;
  const endDate = document.querySelector("#filter-end-date")?.value;
  const moduleVal = document.querySelector("#filter-module")?.value;
  const outcomeVal = document.querySelector("#filter-outcome")?.value;
  const actionVal = document.querySelector("#filter-action")?.value;

  const params = new URLSearchParams();
  params.set("page", currentPage);
  params.set("size", pageSize);
  params.set("sortBy", "createdAt");
  params.set("sortDir", "desc");

  if (startDate) params.set("startDate", startDate);
  if (endDate) params.set("endDate", endDate);
  if (moduleVal) params.set("entityName", moduleVal);
  if (outcomeVal) params.set("outcome", outcomeVal);
  if (actionVal) params.set("action", actionVal);

  try {
    const data = await api.get(`audit-logs?${params.toString()}`);
    const items = data.content || [];
    totalPages = data.totalPages || 1;

    document.querySelector("#page-indicator").textContent = `Page ${currentPage + 1} of ${totalPages}`;
    document.querySelector("#pagination-info").textContent = `Showing ${items.length} of ${data.totalElements || 0} events`;

    document.querySelector("#btn-prev-page").disabled = currentPage === 0;
    document.querySelector("#btn-next-page").disabled = currentPage >= totalPages - 1;

    if (items.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding:32px; color:var(--muted);">No matching audit events found.</td></tr>`;
      return;
    }

    tbody.innerHTML = items.map(item => {
      const outcomeBadge = item.outcome === "SUCCESS"
        ? `<span style="background: rgba(46,204,113,0.15); color:#2ecc71; padding:3px 8px; border-radius:12px; font-size:11px; font-weight:600;">SUCCESS</span>`
        : `<span style="background: rgba(231,76,60,0.15); color:#e74c3c; padding:3px 8px; border-radius:12px; font-size:11px; font-weight:600;">FAILURE</span>`;

      const formattedDate = new Date(item.createdAt).toLocaleString([], {
        year: 'numeric', month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit'
      });

      return `
        <tr style="border-bottom: 1px solid var(--line); font-size: 13px;">
          <td style="padding: 12px; white-space: nowrap; color: var(--muted);">${formattedDate}</td>
          <td style="padding: 12px;">
            <strong>${escapeHtml(item.actorName || 'System')}</strong>
            <br><small style="color:var(--muted); font-size:11px;">${escapeHtml(item.actorRole || 'SYSTEM')}</small>
          </td>
          <td style="padding: 12px; font-weight: 600;">${escapeHtml(item.action)}</td>
          <td style="padding: 12px;"><span style="background:var(--surface-light); padding:3px 8px; border-radius:4px; font-size:11px;">${escapeHtml(item.entityName)}</span></td>
          <td style="padding: 12px; font-family: monospace;">${item.entityId != null ? '#' + item.entityId : '—'}</td>
          <td style="padding: 12px;">${outcomeBadge}</td>
          <td style="padding: 12px; max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${escapeHtml(item.description)}">${escapeHtml(item.description || '—')}</td>
          <td style="padding: 12px; text-align: right;">
            <button class="btn-view-audit-detail btn-secondary" data-id="${item.id}" style="padding: 4px 10px; font-size: 11px;">View</button>
          </td>
        </tr>
      `;
    }).join("");

    tbody.querySelectorAll(".btn-view-audit-detail").forEach(btn => {
      btn.addEventListener("click", async () => {
        const auditId = btn.dataset.id;
        const record = items.find(i => String(i.id) === String(auditId));
        if (record) {
          showDetailModal(record);
        }
      });
    });
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding:24px; color:#e74c3c;">Failed to load audit logs: ${escapeHtml(err.message)}</td></tr>`;
    if (toast) toast("Failed to load audit logs");
  }
}

function showDetailModal(record) {
  const modal = document.querySelector("#audit-detail-modal");
  const body = document.querySelector("#modal-content-body");

  let changesMarkup = `<p style="color:var(--muted);">No structured field changes recorded for this event.</p>`;

  if (record.details) {
    try {
      const parsed = JSON.parse(record.details);
      if (typeof parsed === "object" && parsed !== null && Object.keys(parsed).length > 0) {
        // Check if it's field diffs structure { field: { old: ..., new: ... } }
        const keys = Object.keys(parsed);
        const isDiff = keys.every(k => typeof parsed[k] === "object" && parsed[k] !== null && ("old" in parsed[k] || "new" in parsed[k]));

        if (isDiff) {
          changesMarkup = `
            <table style="width:100%; border-collapse:collapse; margin-top:8px; font-size:12px;">
              <thead>
                <tr style="border-bottom:2px solid var(--line); background:var(--surface-light); text-align:left;">
                  <th style="padding:8px;">Field</th>
                  <th style="padding:8px;">Previous Value</th>
                  <th style="padding:8px;">New Value</th>
                </tr>
              </thead>
              <tbody>
                ${keys.map(key => `
                  <tr style="border-bottom:1px solid var(--line);">
                    <td style="padding:8px; font-weight:600;">${escapeHtml(key)}</td>
                    <td style="padding:8px; color: #e74c3c; font-family:monospace;">${escapeHtml(String(parsed[key].old ?? 'null'))}</td>
                    <td style="padding:8px; color: #2ecc71; font-family:monospace;">${escapeHtml(String(parsed[key].new ?? 'null'))}</td>
                  </tr>
                `).join("")}
              </tbody>
            </table>
          `;
        } else {
          changesMarkup = `<pre style="background:var(--surface-light); padding:12px; border-radius:6px; overflow-x:auto; font-size:12px; font-family:monospace; border:1px solid var(--line);">${escapeHtml(JSON.stringify(parsed, null, 2))}</pre>`;
        }
      }
    } catch (e) {
      changesMarkup = `<p style="font-family:monospace; background:var(--surface-light); padding:8px; border-radius:4px;">${escapeHtml(record.details)}</p>`;
    }
  }

  const formattedDate = new Date(record.createdAt).toLocaleString([], {
    year: 'numeric', month: 'long', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit'
  });

  body.innerHTML = `
    <div style="display:grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px; background: var(--surface-light); padding: 14px; border-radius: 8px;">
      <div><strong>Event ID:</strong> #${record.id}</div>
      <div><strong>Timestamp:</strong> ${formattedDate}</div>
      <div><strong>Actor:</strong> ${escapeHtml(record.actorName || 'System')} (${escapeHtml(record.actorRole || 'SYSTEM')})</div>
      <div><strong>IP Address:</strong> ${escapeHtml(record.ipAddress || 'Internal')}</div>
      <div><strong>Action:</strong> <span style="font-weight:600;">${escapeHtml(record.action)}</span></div>
      <div><strong>Module / Entity:</strong> ${escapeHtml(record.entityName)} ${record.entityId != null ? '(#' + record.entityId + ')' : ''}</div>
      <div><strong>Outcome:</strong> ${record.outcome === 'SUCCESS' ? '<span style="color:#2ecc71; font-weight:bold;">SUCCESS</span>' : '<span style="color:#e74c3c; font-weight:bold;">FAILURE</span>'}</div>
    </div>

    <div style="margin-bottom: 16px;">
      <strong>Description:</strong>
      <p style="margin:4px 0; color:var(--text);">${escapeHtml(record.description || 'N/A')}</p>
    </div>

    <div>
      <strong>Field Changes / Payload Details:</strong>
      <div style="margin-top:6px;">${changesMarkup}</div>
    </div>
  `;

  modal.style.display = "flex";
}

function escapeHtml(str) {
  if (!str) return "";
  return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
