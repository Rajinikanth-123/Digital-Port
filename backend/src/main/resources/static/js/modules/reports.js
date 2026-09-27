import { api } from "../api/api.js";

let chartInstances = {};

export function initReportsModule(container, toastFn) {
  let currentUserRole = "VIEWER";
  try {
    const s = sessionStorage.getItem("harbor_user");
    if (s) {
      const u = JSON.parse(s);
      if (u && u.role) currentUserRole = u.role;
    }
  } catch (e) {}

  const canViewFinancials = ["ADMIN", "PORT_MANAGER", "FINANCE_OFFICER"].includes(currentUserRole);

  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">OPERATIONS / ANALYTICS & REPORTS</p>
        <h1>Reports, Analytics & Data Visualization</h1>
        <p>Real-time database metrics, operational status distribution, revenue performance, and filtered data exports.</p>
      </div>
      <div class="heading-actions">
        <button class="button" id="btn-export-csv">📥 Export CSV</button>
        <button class="button primary" id="btn-export-pdf">📄 Export PDF</button>
      </div>
    </section>

    <!-- Filter Bar -->
    <section class="panel" style="margin-bottom:16px; padding:16px;">
      <form id="filter-form" style="display:flex; flex-wrap:wrap; gap:12px; align-items:flex-end;">
        <div>
          <label style="display:block; font-size:11px; font-weight:600; color:var(--muted); margin-bottom:4px;">Start Date</label>
          <input type="date" id="filter-start-date" class="input" style="padding:6px 10px; font-size:13px;">
        </div>
        <div>
          <label style="display:block; font-size:11px; font-weight:600; color:var(--muted); margin-bottom:4px;">End Date</label>
          <input type="date" id="filter-end-date" class="input" style="padding:6px 10px; font-size:13px;">
        </div>
        <div>
          <label style="display:block; font-size:11px; font-weight:600; color:var(--muted); margin-bottom:4px;">Report Focus</label>
          <select id="filter-report-type" class="input" style="padding:6px 10px; font-size:13px;">
            <option value="shipments">Shipments Status</option>
            <option value="vessels">Vessel Activity</option>
            <option value="containers">Containers & Cargo</option>
            <option value="customs">Customs Clearance</option>
            ${canViewFinancials ? '<option value="financial">Financial Revenue</option>' : ''}
          </select>
        </div>
        <div>
          <button type="submit" class="button primary" style="padding:7px 16px;">Apply Filters</button>
          <button type="button" id="btn-reset-filters" class="button" style="padding:7px 14px;">Reset</button>
        </div>
      </form>
    </section>

    <!-- Operational KPI Grid -->
    <section class="stats-grid compact" style="grid-template-columns: repeat(6, 1fr);">
      <article class="stat-card">
        <div class="stat-icon purple">◉</div>
        <div><span>Total Vessels</span><strong id="kpi-vessels">-</strong><small id="kpi-vessels-sub">In Port: -</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon orange">▣</div>
        <div><span>Containers</span><strong id="kpi-containers">-</strong><small id="kpi-containers-sub">On Hold: -</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon teal">⇄</div>
        <div><span>Total Shipments</span><strong id="kpi-shipments">-</strong><small id="kpi-shipments-sub">In Transit: -</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon blue">✓</div>
        <div><span>Pending Customs</span><strong id="kpi-customs">-</strong><small id="kpi-customs-sub">Declarations</small></div>
      </article>
      ${canViewFinancials ? `
      <article class="stat-card">
        <div class="stat-icon purple">$</div>
        <div><span>Gross Invoiced</span><strong id="kpi-invoiced">$0.00</strong><small>Billed Total</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon teal">✓</div>
        <div><span>Total Revenue</span><strong id="kpi-revenue">$0.00</strong><small>Collected</small></div>
      </article>
      ` : `
      <article class="stat-card" style="grid-column: span 2; opacity: 0.6;">
        <div class="stat-icon orange">🔒</div>
        <div><span>Financial Metrics</span><strong>Restricted</strong><small>Role Authorized</small></div>
      </article>
      `}
    </section>

    <!-- Charts & Visualizations -->
    <div style="display:grid; grid-template-columns: 1fr 1fr; gap:16px; margin-top:16px;">
      <!-- Shipment Status Chart -->
      <section class="panel">
        <div class="panel-title">
          <div><h2>Shipment Status Distribution</h2><p>Real-time breakdown of current shipment states.</p></div>
        </div>
        <div style="padding:16px; position:relative; height:260px; display:flex; justify-content:center; align-items:center;">
          <canvas id="chart-shipments-canvas" style="max-height:240px;"></canvas>
          <div id="chart-shipments-loading" style="position:absolute;"><span class="loading-spinner"></span> Loading status...</div>
        </div>
      </section>

      <!-- Vessel Activity Chart -->
      <section class="panel">
        <div class="panel-title">
          <div><h2>Vessel Status & Activity</h2><p>Operational fleet status distribution.</p></div>
        </div>
        <div style="padding:16px; position:relative; height:260px; display:flex; justify-content:center; align-items:center;">
          <canvas id="chart-vessels-canvas" style="max-height:240px;"></canvas>
          <div id="chart-vessels-loading" style="position:absolute;"><span class="loading-spinner"></span> Loading vessels...</div>
        </div>
      </section>
    </div>

    <div style="display:grid; grid-template-columns: 1fr 1fr; gap:16px; margin-top:16px;">
      <!-- Container & Cargo Summary -->
      <section class="panel">
        <div class="panel-title">
          <div><h2>Containers & Customs Breakdown</h2><p>Customs clearance status queue.</p></div>
        </div>
        <div style="padding:16px; position:relative; height:260px; display:flex; justify-content:center; align-items:center;">
          <canvas id="chart-customs-canvas" style="max-height:240px;"></canvas>
          <div id="chart-customs-loading" style="position:absolute;"><span class="loading-spinner"></span> Loading customs...</div>
        </div>
      </section>

      <!-- Revenue / Payment Time-Series Trend -->
      <section class="panel">
        <div class="panel-title">
          <div>
            <h2>${canViewFinancials ? 'Revenue & Payments Trend' : 'Operational Summary Note'}</h2>
            <p>${canViewFinancials ? 'Recorded collections over time.' : 'Access to financial trends requires elevated role authorization.'}</p>
          </div>
        </div>
        <div style="padding:16px; position:relative; height:260px; display:flex; justify-content:center; align-items:center;">
          ${canViewFinancials ? `
            <canvas id="chart-revenue-canvas" style="max-height:240px; width:100%;"></canvas>
            <div id="chart-revenue-loading" style="position:absolute;"><span class="loading-spinner"></span> Loading revenue trend...</div>
          ` : `
            <div style="text-align:center; color:var(--muted); padding:20px;">
              <p style="font-size:32px; margin:0 0 8px 0;">🔒</p>
              <p>Financial revenue trends are restricted to <b>ADMIN</b>, <b>PORT_MANAGER</b>, and <b>FINANCE_OFFICER</b> accounts.</p>
            </div>
          `}
        </div>
      </section>
    </div>

    <!-- Detailed Status Table -->
    <section class="panel" style="margin-top:16px;">
      <div class="panel-title">
        <div>
          <h2>Operational & Status Summary Table</h2>
          <p>Database metric aggregates matching your selected date window.</p>
        </div>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Category</th>
              <th>Status Identifier</th>
              <th>Total Count</th>
              <th>Percentage Share</th>
            </tr>
          </thead>
          <tbody id="reports-tbody">
            <tr><td colspan="4" style="text-align:center; padding:16px;"><span class="loading-spinner"></span> Aggregating metrics...</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  `;

  // Bind controls
  const form = container.querySelector("#filter-form");
  const startDateInput = container.querySelector("#filter-start-date");
  const endDateInput = container.querySelector("#filter-end-date");
  const reportTypeSelect = container.querySelector("#filter-report-type");
  const btnReset = container.querySelector("#btn-reset-filters");
  const btnExportCsv = container.querySelector("#btn-export-csv");
  const btnExportPdf = container.querySelector("#btn-export-pdf");

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    loadAllReportData();
  });

  btnReset.addEventListener("click", () => {
    startDateInput.value = "";
    endDateInput.value = "";
    reportTypeSelect.value = "shipments";
    loadAllReportData();
    toastFn("Filters reset.");
  });

  btnExportCsv.addEventListener("click", () => {
    const type = reportTypeSelect.value;
    const start = startDateInput.value;
    const end = endDateInput.value;
    let url = `${api.baseUrl || "/api"}/reports/export/csv?reportType=${encodeURIComponent(type)}`;
    if (start) url += `&startDate=${encodeURIComponent(start)}`;
    if (end) url += `&endDate=${encodeURIComponent(end)}`;
    window.open(url, "_blank");
    toastFn("Exporting CSV report...");
  });

  btnExportPdf.addEventListener("click", () => {
    const type = reportTypeSelect.value;
    const start = startDateInput.value;
    const end = endDateInput.value;
    let url = `${api.baseUrl || "/api"}/reports/export/pdf?reportType=${encodeURIComponent(type)}`;
    if (start) url += `&startDate=${encodeURIComponent(start)}`;
    if (end) url += `&endDate=${encodeURIComponent(end)}`;
    window.open(url, "_blank");
    toastFn("Generating PDF report...");
  });

  async function loadAllReportData() {
    const start = startDateInput.value;
    const end = endDateInput.value;

    if (start && end && start > end) {
      toastFn("Start date cannot be after end date.");
      return;
    }

    const params = {};
    if (start) params.startDate = start;
    if (end) params.endDate = end;

    // Load Operational Summary
    try {
      const op = await api.get("reports/operational-summary", params);
      container.querySelector("#kpi-vessels").textContent = op.totalVessels || 0;
      container.querySelector("#kpi-vessels-sub").textContent = `In Port: ${op.vesselsInPort || 0}`;

      container.querySelector("#kpi-containers").textContent = op.totalContainers || 0;
      container.querySelector("#kpi-containers-sub").textContent = `On Hold: ${op.containersInHold || 0}`;

      container.querySelector("#kpi-shipments").textContent = op.totalShipments || 0;
      container.querySelector("#kpi-shipments-sub").textContent = `In Transit: ${op.activeShipments || 0}`;

      container.querySelector("#kpi-customs").textContent = op.pendingCustoms || 0;
      container.querySelector("#kpi-customs-sub").textContent = `Approved: ${op.approvedCustoms || 0}`;
    } catch (err) {
      toastFn(`Failed operational summary: ${err.message}`);
    }

    // Load Financial Summary if permitted
    if (canViewFinancials) {
      try {
        const fin = await api.get("reports/financial-summary", params);
        container.querySelector("#kpi-invoiced").textContent = `$${(fin.totalInvoiced || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
        container.querySelector("#kpi-revenue").textContent = `$${(fin.totalCollected || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
      } catch (err) {}
    }

    // Load Shipment Status Chart
    try {
      const shipmentsData = await api.get("reports/shipment-distribution", params);
      renderChart("chart-shipments-canvas", "chart-shipments-loading", "doughnut", "Shipments", shipmentsData);
    } catch (e) {}

    // Load Vessel Activity Chart
    try {
      const vesselData = await api.get("reports/vessel-activity", params);
      renderChart("chart-vessels-canvas", "chart-vessels-loading", "bar", "Vessels", vesselData);
    } catch (e) {}

    // Load Customs Summary Chart
    try {
      const customsData = await api.get("reports/customs-summary", params);
      renderChart("chart-customs-canvas", "chart-customs-loading", "pie", "Customs Declarations", customsData);
    } catch (e) {}

    // Load Revenue Trends Chart if permitted
    if (canViewFinancials) {
      try {
        const trendData = await api.get("reports/revenue-trends", params);
        renderTimeSeriesChart("chart-revenue-canvas", "chart-revenue-loading", trendData);
      } catch (e) {}
    }

    // Load Detailed Table
    loadTableData(params);
  }

  async function loadTableData(params) {
    const tbody = container.querySelector("#reports-tbody");
    try {
      const list = await api.get("reports/shipment-distribution", params);
      if (!list || list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; padding:16px;">No shipment records match date filters.</td></tr>`;
        return;
      }
      const total = list.reduce((sum, item) => sum + (item.count || 0), 0);
      tbody.innerHTML = list.map(item => {
        const pct = total > 0 ? ((item.count / total) * 100).toFixed(1) : "0.0";
        return `
          <tr>
            <td>Shipment Operations</td>
            <td><strong>${escapeHtml(item.status)}</strong></td>
            <td><strong>${item.count}</strong></td>
            <td>${pct}%</td>
          </tr>
        `;
      }).join("");
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; padding:16px; color:#e07b65;">Failed loading details: ${escapeHtml(e.message)}</td></tr>`;
    }
  }

  function renderChart(canvasId, loaderId, type, label, dataList) {
    const canvas = container.querySelector(`#${canvasId}`);
    const loader = container.querySelector(`#${loaderId}`);
    if (loader) loader.style.display = "none";
    if (!canvas || !window.Chart) return;

    if (chartInstances[canvasId]) {
      chartInstances[canvasId].destroy();
    }

    const labels = (dataList || []).map(d => d.status);
    const counts = (dataList || []).map(d => d.count);

    const colors = [
      '#00b4d8', '#38a169', '#d69e2e', '#e53e3e', '#805ad5',
      '#3182ce', '#dd6b20', '#319795', '#d53f8c', '#718096'
    ];

    chartInstances[canvasId] = new window.Chart(canvas, {
      type: type,
      data: {
        labels: labels,
        datasets: [{
          label: label,
          data: counts,
          backgroundColor: colors.slice(0, labels.length),
          borderColor: 'transparent',
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 11 } } }
        },
        scales: type === 'bar' ? { y: { beginAtZero: true, ticks: { precision: 0 } } } : {}
      }
    });
  }

  function renderTimeSeriesChart(canvasId, loaderId, trendPoints) {
    const canvas = container.querySelector(`#${canvasId}`);
    const loader = container.querySelector(`#${loaderId}`);
    if (loader) loader.style.display = "none";
    if (!canvas || !window.Chart) return;

    if (chartInstances[canvasId]) {
      chartInstances[canvasId].destroy();
    }

    const labels = (trendPoints || []).map(t => t.label);
    const amounts = (trendPoints || []).map(t => t.amount || 0);

    chartInstances[canvasId] = new window.Chart(canvas, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Revenue Received ($)',
          data: amounts,
          fill: true,
          backgroundColor: 'rgba(0, 180, 216, 0.12)',
          borderColor: '#00b4d8',
          tension: 0.3,
          pointRadius: 4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: true, position: 'bottom' }
        },
        scales: {
          y: { beginAtZero: true, ticks: { callback: v => '$' + v } }
        }
      }
    });
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  loadAllReportData();
}
