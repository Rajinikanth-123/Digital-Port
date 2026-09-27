import { ALL_NAV_ITEMS, getFilteredNavItems, currentPage } from "./navigation.js";
import { api } from "./api/api.js";
import { dashboardContent, initDashboard } from "./dashboard.js";
import { initPortsModule } from "./modules/ports.js";
import { initVesselsModule } from "./modules/vessels.js";
import { initTerminalsModule } from "./modules/terminals.js";
import { initBerthsModule } from "./modules/berths.js";
import { initContainersModule } from "./modules/containers.js";
import { initCargoModule } from "./modules/cargo.js";
import { initShipmentsModule } from "./modules/shipments.js";
import { initSchedulesModule } from "./modules/schedules.js";
import { initCustomsModule } from "./modules/customs.js";
import { initDocumentsModule } from "./modules/documents.js";
import { initInvoicesModule } from "./modules/invoices.js";
import { initPaymentsModule } from "./modules/payments.js";
import { initReportsModule } from "./modules/reports.js";
import { initUsersModule } from "./modules/users.js";
import { initNotificationsModule } from "./modules/notifications.js";
import { initAuditLogsModule } from "./modules/audit-logs.js";
import { initSearchModule } from "./modules/search.js";
import { initProfileModule } from "./modules/profile.js";
import { initHelpModule } from "./modules/help.js";

const page = currentPage();
const moduleNames = {
  "ports.html": ["Port Management", "Manage ports and operating locations."],
  "terminals.html": ["Terminal Management", "Coordinate terminal capacity and activity."],
  "vessels.html": ["Vessel Management", "Maintain vessel registry and operator details."],
  "berths.html": ["Berth Management", "Schedule berth allocation without conflicts."],
  "containers.html": ["Container Management", "Track container locations, types, and status."],
  "cargo.html": ["Cargo Management", "Manage cargo linked to shipments."],
  "shipments.html": ["Shipment Tracking", "Search and monitor shipment progress."],
  "schedules.html": ["Shipping Schedules", "Manage vessel arrival and departure windows."],
  "customs.html": ["Customs Clearance", "Review declarations and clearance status."],
  "documents.html": ["Documents", "Securely manage shipping documentation."],
  "invoices.html": ["Invoices", "Review charges, taxes, and payment status."],
  "payments.html": ["Payments", "Track invoice payment activity."],
  "notifications.html": ["Notifications & Alerts", "System notifications and event alerts."],
  "users.html": ["User Management", "Manage team access and roles."],
  "audit-logs.html": ["Audit Logs", "System security activity logging and change history."],
  "search.html": ["Global Search", "Search and filter records across workspace modules."],
  "reports.html": ["Reports & Analytics", "Explore operational and revenue reports."],
  "profile.html": ["Profile", "Manage your account preferences."]
};

let currentUser = null;

async function checkAuthAndInit() {
  let savedUser = null;
  try {
    const sessionStr = sessionStorage.getItem("harbor_user");
    if (sessionStr) savedUser = JSON.parse(sessionStr);
  } catch (e) {}

  try {
    const me = await api.get("auth/me");
    if (me && me.name) {
      currentUser = me;
    } else if (savedUser) {
      currentUser = savedUser;
    } else {
      currentUser = { name: "System User", email: "user@demo.invalid", role: "ADMIN" };
    }
  } catch (e) {
    if (savedUser) {
      currentUser = savedUser;
    } else {
      if (page !== "login.html" && page !== "index.html") {
        location.href = "login.html";
        return;
      }
      currentUser = { name: "Guest User", email: "guest@demo.invalid", role: "VIEWER" };
    }
  }

  renderShell();
}

function renderShell() {
  const app = document.querySelector("#app");
  if (!app) return;

  const role = currentUser ? currentUser.role : "VIEWER";
  const navItems = getFilteredNavItems(role);

  const links = navItems.map(([href, icon, label]) => {
    if (href === "login.html") {
      return `<a href="#" id="btn-sidebar-logout"><span>${icon}</span>Logout</a>`;
    }
    return `<a class="${href === page ? "active" : ""}" href="${href}"><span>${icon}</span>${label}</a>`;
  }).join("");

  const pageTitle = page === "dashboard.html" ? "Dashboard" : (moduleNames[page]?.[0] || "Welcome");
  const userName = currentUser ? currentUser.name : "Alex Rivera";
  const userRoleStr = currentUser ? currentUser.role : "Port Manager";
  const initials = userName.split(" ").map(n => n[0]).join("").toUpperCase().slice(0, 2) || "US";

  app.innerHTML = `
    <div class="app-shell">
      <aside class="sidebar" id="sidebar">
        <a class="brand" href="dashboard.html"><span>⚓</span> Harbor<span>OS</span></a>
        <p class="sidebar-label">Operations</p>
        <nav aria-label="Main navigation">${links}</nav>
        <div class="sidebar-foot">
          <div class="help-box">
            <b>Need a hand?</b>
            <small>Visit our help center</small>
            <a href="reports.html">Get support →</a>
          </div>
          <button class="profile-button" id="profileButton">
            <span class="avatar">${initials}</span>
            <span><b>${escapeHtml(userName)}</b><small>${escapeHtml(userRoleStr)}</small></span>⌄
          </button>
        </div>
      </aside>

      <div class="page">
        <header class="topbar">
          <button class="icon-button" id="menuToggle" aria-label="Toggle navigation">☰</button>
          <div class="breadcrumbs">Workspace <i>/</i> <b>${pageTitle}</b></div>
          <div class="top-actions">
            <div style="position:relative;">
              <label class="global-search">
                <span>⌕</span>
                <input id="header-search-input" placeholder="Search workspace..." aria-label="Search workspace" autocomplete="off">
              </label>
              <div id="header-search-dropdown" style="display:none; position:absolute; left:0; top:40px; width:340px; background:var(--surface); border:1px solid var(--line); border-radius:10px; box-shadow:0 8px 24px rgba(0,0,0,0.25); z-index:200; overflow:hidden;"></div>
            </div>
            <button class="icon-button" id="themeToggle" aria-label="Toggle theme">☾</button>
            <div style="position:relative;">
              <button class="icon-button" id="notifToggle" aria-label="Notifications" style="position:relative;">
                🔔 <span id="notif-badge" style="display:none; position:absolute; top:-2px; right:-2px; background:#e07b65; color:#fff; border-radius:50%; font-size:10px; width:16px; height:16px; line-height:16px; text-align:center; font-weight:bold;">0</span>
              </button>
              <div id="notif-dropdown" style="display:none; position:absolute; right:0; top:40px; width:340px; background:var(--surface); border:1px solid var(--line); border-radius:10px; box-shadow:0 8px 24px rgba(0,0,0,0.25); z-index:100; overflow:hidden;">
                <div style="display:flex; justify-space-between; align-items:center; padding:12px 16px; border-bottom:1px solid var(--line); background:var(--surface-light, #1e2530);">
                  <strong style="font-size:14px;">In-App Notifications</strong>
                  <button id="btn-read-all-notifs" style="background:none; border:none; color:var(--accent); font-size:12px; cursor:pointer;">Mark all read</button>
                </div>
                <div id="notif-list-body" style="max-height:320px; overflow-y:auto; padding:8px 0;">
                  <p style="text-align:center; padding:16px; color:var(--muted); font-size:12px;">Loading notifications...</p>
                </div>
                <div style="padding: 8px 16px; border-top: 1px solid var(--line); text-align: center; background: var(--surface-light, #1e2530);">
                  <a href="notifications.html" style="font-size: 12px; color: var(--accent); text-decoration: none; font-weight: 600;">View all notifications →</a>
                </div>
              </div>
            </div>
            <button class="user-chip" id="userChip"><span class="avatar">${initials}</span> ${escapeHtml(userName)} (${escapeHtml(userRoleStr)})</button>
          </div>
        </header>
        <main class="main-content" id="main-content">
          <div style="padding:24px;"><span class="loading-spinner"></span> Loading workspace...</div>
        </main>
        <footer>© 2026 HarborOS <span>Spring Boot RBAC Security & MySQL Connected</span></footer>
      </div>
    </div>
    <div class="toast" id="toast" role="status" aria-live="polite"></div>
  `;

  bindGlobalEvents();
  loadCurrentPageModule();
  initNotificationsSystem();
}

function loadCurrentPageModule() {
  const mainContent = document.querySelector("#main-content");
  if (!mainContent) return;

  if (page === "dashboard.html") {
    mainContent.innerHTML = dashboardContent();
    initDashboard(mainContent, toast);
  } else if (page === "ports.html") {
    initPortsModule(mainContent, toast);
  } else if (page === "vessels.html") {
    initVesselsModule(mainContent, toast);
  } else if (page === "terminals.html") {
    initTerminalsModule(mainContent, toast);
  } else if (page === "berths.html") {
    initBerthsModule(mainContent, toast);
  } else if (page === "containers.html") {
    initContainersModule(mainContent, toast);
  } else if (page === "cargo.html") {
    initCargoModule(mainContent, toast);
  } else if (page === "shipments.html") {
    initShipmentsModule(mainContent, toast);
  } else if (page === "schedules.html") {
    initSchedulesModule(mainContent, toast);
  } else if (page === "customs.html") {
    initCustomsModule(mainContent, toast);
  } else if (page === "documents.html") {
    initDocumentsModule(mainContent, toast);
  } else if (page === "invoices.html") {
    initInvoicesModule(mainContent, toast);
  } else if (page === "payments.html") {
    initPaymentsModule(mainContent, toast);
  } else if (page === "notifications.html") {
    initNotificationsModule(mainContent, toast);
  } else if (page === "audit-logs.html") {
    initAuditLogsModule(mainContent, toast);
  } else if (page === "search.html") {
    initSearchModule(mainContent, toast);
  } else if (page === "reports.html") {
    initReportsModule(mainContent, toast);
  } else if (page === "users.html") {
    initUsersModule(mainContent, toast);
  } else if (page === "profile.html") {
    initProfileModule(mainContent, toast);
  } else if (page === "help.html") {
    initHelpModule();
  } else {
    mainContent.innerHTML = genericModuleContent();
  }
}

function genericModuleContent() {
  const [title, description] = moduleNames[page] || ["Module", "Digital Port Operational Workspace"];
  return `
    <section class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / ${title.toUpperCase()}</p>
        <h1>${title}</h1>
        <p>${description}</p>
      </div>
    </section>
    <div class="demo-notice">Connected to Spring Boot REST API Service.</div>
    <section class="panel">
      <div class="panel-title">
        <div><h2>${title}</h2><p>Operational controls and system configuration.</p></div>
      </div>
      <p style="color:var(--muted); font-size:13px; margin: 12px 0;">Logged in as: <b>${escapeHtml(currentUser ? currentUser.name : "User")}</b> (Role: <b>${escapeHtml(currentUser ? currentUser.role : "VIEWER")}</b>).</p>
    </section>
  `;
}

async function initNotificationsSystem() {
  const notifBtn = document.querySelector("#notifToggle");
  const badge = document.querySelector("#notif-badge");
  const dropdown = document.querySelector("#notif-dropdown");
  const listBody = document.querySelector("#notif-list-body");
  const readAllBtn = document.querySelector("#btn-read-all-notifs");

  if (!notifBtn || !badge || !dropdown) return;

  let isPolling = false;

  async function updateUnreadBadge() {
    if (isPolling) return;
    isPolling = true;
    try {
      const res = await api.get("notifications/unread-count");
      const count = res.unreadCount || 0;
      if (count > 0) {
        badge.textContent = count > 99 ? "99+" : count;
        badge.style.display = "inline-block";
      } else {
        badge.style.display = "none";
      }
    } catch (e) {
    } finally {
      isPolling = false;
    }
  }

  async function fetchNotificationsList() {
    listBody.innerHTML = `<p style="text-align:center; padding:16px; color:var(--muted); font-size:12px;"><span class="loading-spinner"></span> Loading notifications...</p>`;
    try {
      const res = await api.get("notifications?size=8&archived=false");
      const items = res.content || res || [];
      if (!items || items.length === 0) {
        listBody.innerHTML = `<p style="text-align:center; padding:20px; color:var(--muted); font-size:12px;">No notifications yet.</p>`;
        return;
      }

      listBody.innerHTML = items.map(n => `
        <div style="padding:10px 14px; border-bottom:1px solid var(--line); ${n.readStatus ? 'opacity:0.6;' : 'background:rgba(224,123,101,0.06);'} font-size:12px;">
          <div style="display:flex; justify-content:space-between; font-weight:600; color:var(--text);">
            <span>${escapeHtml(n.title)}</span>
            <small style="color:var(--muted); font-weight:normal;">${new Date(n.createdAt).toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'})}</small>
          </div>
          <p style="margin:4px 0 6px 0; color:var(--muted);">${escapeHtml(n.message)}</p>
          ${!n.readStatus ? `<button class="btn-mark-notif-read" data-id="${n.id}" style="background:none; border:none; color:var(--accent); font-size:11px; cursor:pointer; padding:0;">✓ Mark read</button>` : ''}
        </div>
      `).join("");

      listBody.querySelectorAll(".btn-mark-notif-read").forEach(btn => {
        btn.addEventListener("click", async (e) => {
          e.stopPropagation();
          try {
            await api.put(`notifications/${btn.dataset.id}/read`);
            fetchNotificationsList();
            updateUnreadBadge();
          } catch (err) {}
        });
      });
    } catch (err) {
      listBody.innerHTML = `<p style="text-align:center; padding:16px; color:#e07b65; font-size:12px;">Failed to load: ${escapeHtml(err.message)}</p>`;
    }
  }

  notifBtn.addEventListener("click", (e) => {
    e.stopPropagation();
    const isShown = dropdown.style.display === "block";
    dropdown.style.display = isShown ? "none" : "block";
    if (!isShown) fetchNotificationsList();
  });

  document.addEventListener("click", (e) => {
    if (dropdown.style.display === "block" && !dropdown.contains(e.target) && !notifBtn.contains(e.target)) {
      dropdown.style.display = "none";
    }
  });

  readAllBtn?.addEventListener("click", async () => {
    try {
      await api.put("notifications/read-all");
      fetchNotificationsList();
      updateUnreadBadge();
    } catch (e) {}
  });

  // Initial load
  updateUnreadBadge();

  // Smart polling: Poll every 30 seconds if page is visible
  setInterval(() => {
    if (document.visibilityState === "visible") {
      updateUnreadBadge();
      if (dropdown.style.display === "block") {
        fetchNotificationsList();
      }
    }
  }, 30000);

  // Listen to custom update event
  window.addEventListener("notifications-updated", () => {
    updateUnreadBadge();
    if (dropdown.style.display === "block") {
      fetchNotificationsList();
    }
  });
}

function toast(message) {
  const el = document.querySelector("#toast");
  if (!el) return;
  el.textContent = message;
  el.classList.add("show");
  setTimeout(() => el.classList.remove("show"), 2800);
}

function bindGlobalEvents() {
  document.querySelector("#menuToggle")?.addEventListener("click", () =>
    document.querySelector("#sidebar").classList.toggle("open")
  );

  document.querySelector("#themeToggle")?.addEventListener("click", () => {
    document.body.classList.toggle("dark");
    localStorage.setItem("harbor-theme", document.body.classList.contains("dark") ? "dark" : "light");
  });

  if (localStorage.getItem("harbor-theme") === "dark") {
    document.body.classList.add("dark");
  }

  const searchInput = document.querySelector("#header-search-input");
  const searchDropdown = document.querySelector("#header-search-dropdown");
  let searchDebounce = null;

  if (searchInput && searchDropdown) {
    searchInput.addEventListener("input", (e) => {
      clearTimeout(searchDebounce);
      const val = e.target.value.trim();
      if (val.length < 2) {
        searchDropdown.style.display = "none";
        return;
      }
      searchDebounce = setTimeout(async () => {
        try {
          const res = await api.get("search/suggestions?q=" + encodeURIComponent(val) + "&limit=6");
          const items = res || [];
          if (items.length === 0) {
            searchDropdown.innerHTML = `<div style="padding:12px; font-size:12px; color:var(--muted); text-align:center;">No matching records found</div>`;
          } else {
            searchDropdown.innerHTML = items.map(item => `
              <a href="${item.detailUrl}" style="display:flex; justify-space-between; align-items:center; padding:10px 14px; text-decoration:none; color:var(--text); border-bottom:1px solid var(--line); background:transparent;">
                <div>
                  <div style="font-size:13px; font-weight:600; color:var(--accent);">${escapeHtml(item.displayTitle)}</div>
                  <div style="font-size:11px; color:var(--muted);">${escapeHtml(item.summary || '')}</div>
                </div>
                <span class="badge" style="font-size:10px; background:var(--surface-light);">${item.module}</span>
              </a>
            `).join("") + `
              <div style="padding:8px 14px; background:var(--surface-light); text-align:center;">
                <a href="search.html?q=${encodeURIComponent(val)}" style="font-size:12px; color:var(--accent); font-weight:600; text-decoration:none;">See all results →</a>
              </div>
            `;
          }
          searchDropdown.style.display = "block";
        } catch (err) {}
      }, 250);
    });

    searchInput.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        const val = searchInput.value.trim();
        searchDropdown.style.display = "none";
        location.href = "search.html?q=" + encodeURIComponent(val);
      } else if (e.key === "Escape") {
        searchDropdown.style.display = "none";
      }
    });

    document.addEventListener("click", (e) => {
      if (!searchInput.contains(e.target) && !searchDropdown.contains(e.target)) {
        searchDropdown.style.display = "none";
      }
    });
  }

  document.querySelector("#profileButton")?.addEventListener("click", () =>
    location.href = "profile.html"
  );
  document.querySelector("#userChip")?.addEventListener("click", () =>
    location.href = "profile.html"
  );

  document.querySelector("#btn-sidebar-logout")?.addEventListener("click", async (e) => {
    e.preventDefault();
    if (!confirm("Sign out of HarborOS?")) return;
    try {
      await api.post("auth/logout");
    } catch (err) {}
    sessionStorage.removeItem("harbor_user");
    location.href = "login.html";
  });
}

function escapeHtml(str) {
  if (!str) return "";
  return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

checkAuthAndInit();
