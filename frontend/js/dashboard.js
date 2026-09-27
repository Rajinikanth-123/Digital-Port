import { api } from "./api/api.js";

export function dashboardContent() {
  return `
    <section class="page-heading">
      <div>
        <p class="eyebrow">${new Date().toLocaleDateString('en-US', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).toUpperCase()}</p>
        <h1>Operations & Admin Overview</h1>
        <p>Monitor real-time port network operational metrics, system database totals, and security audit activity.</p>
      </div>
      <div class="heading-actions">
        <button class="button" id="btn-refresh-dashboard">↻ Refresh Data</button>
        <button class="button primary" id="btn-export-report">Export report</button>
      </div>
    </section>

    <div class="demo-notice" id="db-notice">Connecting to Spring Boot REST API & MySQL...</div>

    <section class="stats-grid" style="grid-template-columns: repeat(4, 1fr);">
      <article class="stat-card">
        <div class="stat-icon purple">◉</div>
        <div><span>Total Vessels</span><strong id="dash-vessels-count">-</strong><small>Fleet vessels</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon teal">⇄</div>
        <div><span>Active Shipments</span><strong id="dash-shipments-count">-</strong><small>Cargo tracking</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon orange">▣</div>
        <div><span>Containers</span><strong id="dash-containers-count">-</strong><small>Inventory total</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon blue">◫</div>
        <div><span>Berth Allocations</span><strong id="dash-berths-count">-</strong><small>Scheduled calls</small></div>
      </article>
    </section>

    <section class="stats-grid" style="grid-template-columns: repeat(4, 1fr); margin-top: -6px;">
      <article class="stat-card">
        <div class="stat-icon teal">⌖</div>
        <div><span>Ports & Terminals</span><strong id="dash-ports-count">-</strong><small>Operational locations</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon orange">✓</div>
        <div><span>Pending Customs</span><strong id="dash-customs-count">-</strong><small>Clearance queue</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon purple">♙</div>
        <div><span>Total Accounts</span><strong id="dash-users-count">-</strong><small>Registered users</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon blue">◎</div>
        <div><span>Active System Users</span><strong id="dash-active-users-count">-</strong><small>Enabled accounts</small></div>
      </article>
    </section>

    <section class="content-grid">
      <article class="panel wide">
        <div class="panel-title">
          <div>
            <h2>Port Traffic Activity</h2>
            <p>Vessel arrivals and departures across network terminals</p>
          </div>
          <div class="legend">
            <span class="arrivals">Arrivals</span>
            <span class="departures">Departures</span>
          </div>
        </div>
        <div class="bar-chart" aria-label="Shipment status chart">
          ${["Mon","Tue","Wed","Thu","Fri","Sat","Sun"].map(day => `<div class="bar-group"><div class="bars"><i></i><b></b></div><small>${day}</small></div>`).join("")}
        </div>
      </article>

      <article class="panel">
        <div class="panel-title">
          <div>
            <h2>Shipment Distribution</h2>
            <p>Current movement status</p>
          </div>
        </div>
        <div class="donut"><span id="dash-donut-text">-<small>shipments</small></span></div>
        <ul class="metric-list" id="dash-metric-list">
          <li><i class="dot teal"></i>In transit <b id="dash-pct-transit">-</b></li>
          <li><i class="dot blue"></i>Registered / Delivered <b id="dash-pct-delivered">-</b></li>
          <li><i class="dot orange"></i>Customs hold <b id="dash-pct-hold">-</b></li>
        </ul>
      </article>
    </section>

    <section class="content-grid">
      <article class="panel wide">
        <div class="panel-title">
          <div>
            <h2>Vessel Registry & Status</h2>
            <p>Next scheduled port calls and fleet status</p>
          </div>
          <a class="text-link" href="vessels.html">View vessel registry →</a>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Vessel</th>
                <th>IMO Number</th>
                <th>Type</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody id="dash-vessels-tbody">
              <tr><td colspan="4" style="text-align:center; padding: 20px;"><span class="loading-spinner"></span> Loading vessels...</td></tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="panel">
        <div class="panel-title">
          <div>
            <h2>Upcoming Schedules</h2>
            <p>Vessel arrivals & departures</p>
          </div>
        </div>
        <ul class="schedule-list" id="dash-schedule-list">
          <li><span style="color:var(--muted)">Loading live schedules...</span></li>
        </ul>
      </article>
    </section>

    <!-- Recent Security Audit Log section -->
    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>System Audit & Activity Trail</h2>
          <p>Real-time security events and administrative action log from database</p>
        </div>
        <a class="text-link" href="users.html">Manage Users & Roles →</a>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Timestamp</th>
              <th>Action</th>
              <th>Target Entity</th>
              <th>User / Actor</th>
              <th>Details</th>
            </tr>
          </thead>
          <tbody id="dash-audit-tbody">
            <tr><td colspan="5" style="text-align:center; padding: 20px;"><span class="loading-spinner"></span> Loading audit logs...</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>Recent Shipments</h2>
          <p>Live cargo movements from Spring Boot API</p>
        </div>
        <div class="toolbar">
          <input class="table-search" id="dash-shipment-search" placeholder="Search shipments" aria-label="Search shipments">
        </div>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Tracking ID</th>
              <th>Route</th>
              <th>Containers</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody id="dash-shipments-tbody">
            <tr><td colspan="4" style="text-align:center; padding: 20px;"><span class="loading-spinner"></span> Loading shipments...</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  `;
}

export async function initDashboard(container, toastFn) {
  const notice = container.querySelector("#db-notice");
  const refreshBtn = container.querySelector("#btn-refresh-dashboard");
  const exportBtn = container.querySelector("#btn-export-report");

  refreshBtn.addEventListener("click", () => loadAll());
  exportBtn.addEventListener("click", () => toastFn("Report export: API summary report generated."));

  async function loadAll() {
    notice.className = "demo-notice";
    notice.textContent = "Connecting to Spring Boot REST API & MySQL...";

    try {
      const [summary, vesselsRes, shipmentsRes, allocRes, auditLogsRes] = await Promise.all([
        api.get("dashboard").catch(() => null),
        api.get("vessels", { size: 10 }).catch(() => null),
        api.get("shipments", { size: 10 }).catch(() => null),
        api.get("berth-allocations").catch(() => null),
        api.get("audit-logs/recent").catch(() => null)
      ]);

      const vessels = api.unwrap(vesselsRes);
      const shipments = api.unwrap(shipmentsRes);
      const allocs = api.unwrap(allocRes);
      const auditLogs = api.unwrap(auditLogsRes);

      if (summary) {
        container.querySelector("#dash-vessels-count").textContent = summary.vessels;
        container.querySelector("#dash-shipments-count").textContent = summary.shipments;
        container.querySelector("#dash-containers-count").textContent = summary.containers;
        container.querySelector("#dash-berths-count").textContent = summary.berthAllocations;
        if (container.querySelector("#dash-ports-count")) container.querySelector("#dash-ports-count").textContent = summary.ports !== undefined ? summary.ports : "-";
        if (container.querySelector("#dash-customs-count")) container.querySelector("#dash-customs-count").textContent = summary.pendingCustoms !== undefined ? summary.pendingCustoms : "-";
        if (container.querySelector("#dash-users-count")) container.querySelector("#dash-users-count").textContent = summary.users !== undefined ? summary.users : "-";
        if (container.querySelector("#dash-active-users-count")) container.querySelector("#dash-active-users-count").textContent = summary.activeUsers !== undefined ? summary.activeUsers : "-";

        notice.style.background = "#e5f7f3";
        notice.style.color = "#078e81";
        notice.textContent = `Connected to Spring Boot API · Live data loaded from MySQL (${summary.vessels} vessels, ${summary.shipments} shipments, ${summary.containers} containers, ${summary.users || 0} users).`;
      } else {
        container.querySelector("#dash-vessels-count").textContent = vessels.length;
        container.querySelector("#dash-shipments-count").textContent = shipments.length;
        container.querySelector("#dash-containers-count").textContent = "-";
        container.querySelector("#dash-berths-count").textContent = allocs.length;
        notice.textContent = "Connected to backend resource APIs.";
      }

      // Render vessels table
      const vesselsTbody = container.querySelector("#dash-vessels-tbody");
      if (vessels.length === 0) {
        vesselsTbody.innerHTML = `<tr><td colspan="4" style="text-align:center; color:var(--muted);">No vessel records found.</td></tr>`;
      } else {
        vesselsTbody.innerHTML = vessels.slice(0, 5).map(v => `
          <tr>
            <td><strong>${escapeHtml(v.name)}</strong></td>
            <td><span class="status at-berth">${escapeHtml(v.imoNumber)}</span></td>
            <td>${escapeHtml(v.vesselType || "CONTAINER")}</td>
            <td><span class="status delivered">${escapeHtml(v.status || "ACTIVE")}</span></td>
          </tr>
        `).join("");
      }

      // Render shipments table
      const shipmentsTbody = container.querySelector("#dash-shipments-tbody");
      if (shipments.length === 0) {
        shipmentsTbody.innerHTML = `<tr><td colspan="4" style="text-align:center; color:var(--muted);">No shipment records found.</td></tr>`;
      } else {
        renderDashShipments(shipments);
      }

      // Render audit logs
      const auditTbody = container.querySelector("#dash-audit-tbody");
      if (auditTbody) {
        if (!auditLogs || auditLogs.length === 0) {
          auditTbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:var(--muted);">No audit log records recorded yet or Admin permissions required.</td></tr>`;
        } else {
          auditTbody.innerHTML = auditLogs.slice(0, 8).map(a => {
            const time = a.createdAt ? new Date(a.createdAt).toLocaleString() : "-";
            return `
              <tr>
                <td><small>${escapeHtml(time)}</small></td>
                <td><span class="status at-berth">${escapeHtml(a.action)}</span></td>
                <td><strong>${escapeHtml(a.entityName)}${a.entityId ? ` #${a.entityId}` : ""}</strong></td>
                <td>${escapeHtml(a.userName || "System")} <small>(${escapeHtml(a.userEmail || "system")})</small></td>
                <td>${escapeHtml(a.details || "-")}</td>
              </tr>
            `;
          }).join("");
        }
      }

      // Render upcoming schedule list from berth allocations
      const schedList = container.querySelector("#dash-schedule-list");
      if (allocs.length === 0) {
        schedList.innerHTML = `<li><span style="color:var(--muted)">No scheduled port calls in database.</span></li>`;
      } else {
        schedList.innerHTML = allocs.slice(0, 5).map(a => {
          const timeStr = a.startTime ? new Date(a.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : "00:00";
          const vesselName = a.vessel ? a.vessel.name : "Vessel Call";
          const berthName = a.berth || (a.berthEntity ? a.berthEntity.name : "Berth");
          return `
            <li>
              <time>${timeStr}</time>
              <span><b>${escapeHtml(vesselName)}</b><small>Arrival · ${escapeHtml(berthName)}</small></span>
              <em>${escapeHtml(a.status || "On time")}</em>
            </li>
          `;
        }).join("");
      }

      // Update distribution percentages
      const totalS = shipments.length || 1;
      const inTransit = shipments.filter(s => s.status === "IN_TRANSIT").length;
      const hold = shipments.filter(s => s.status === "CUSTOMS_HOLD").length;
      const other = totalS - inTransit - hold;

      container.querySelector("#dash-donut-text").innerHTML = `${shipments.length}<small>total</small>`;
      container.querySelector("#dash-pct-transit").textContent = `${Math.round((inTransit / totalS) * 100)}%`;
      container.querySelector("#dash-pct-delivered").textContent = `${Math.round((other / totalS) * 100)}%`;
      container.querySelector("#dash-pct-hold").textContent = `${Math.round((hold / totalS) * 100)}%`;

    } catch (e) {
      notice.className = "demo-notice error-banner";
      notice.textContent = `Backend connection warning: ${e.message}`;
    }
  }

  function renderDashShipments(items) {
    const tbody = container.querySelector("#dash-shipments-tbody");
    tbody.innerHTML = items.map(s => `
      <tr>
        <td><strong>${escapeHtml(s.trackingNumber || s.referenceNumber || `#${s.id}`)}</strong></td>
        <td>${escapeHtml(s.origin || "-")} → ${escapeHtml(s.destination || "-")}</td>
        <td>${s.containerCount || 0}</td>
        <td><span class="status ${s.status === "IN_TRANSIT" ? "" : (s.status === "CUSTOMS_HOLD" ? "customs-hold" : "delivered")}">${escapeHtml(s.status || "REGISTERED")}</span></td>
      </tr>
    `).join("");
  }

  const dashSearch = container.querySelector("#dash-shipment-search");
  if (dashSearch) {
    dashSearch.addEventListener("input", async () => {
      const q = dashSearch.value.toLowerCase().trim();
      const res = await api.get("shipments", { size: 20 });
      const items = api.unwrap(res);
      const filtered = items.filter(s =>
        (s.trackingNumber && s.trackingNumber.toLowerCase().includes(q)) ||
        (s.referenceNumber && s.referenceNumber.toLowerCase().includes(q)) ||
        (s.origin && s.origin.toLowerCase().includes(q)) ||
        (s.destination && s.destination.toLowerCase().includes(q))
      );
      renderDashShipments(filtered);
    });
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  loadAll();
}
