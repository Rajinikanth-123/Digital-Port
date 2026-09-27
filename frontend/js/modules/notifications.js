import { api } from "../api/api.js";

export function initNotificationsModule(container, toast) {
  let currentPage = 0;
  const pageSize = 15;
  let activeTab = "ALL"; // ALL, UNREAD, ARCHIVED
  let selectedCategory = "ALL";

  renderSkeleton();

  function renderSkeleton() {
    container.innerHTML = `
      <div style="padding: 24px; max-width: 1100px; margin: 0 auto;">
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; margin-bottom: 24px;">
          <div>
            <h1 style="font-size: 24px; font-weight: 700; margin: 0; color: var(--text);">Notifications & Alerts</h1>
            <p style="margin: 4px 0 0 0; color: var(--muted); font-size: 14px;">Monitor system alerts, shipment updates, customs status, and billing notifications.</p>
          </div>
          <div style="display: flex; gap: 12px; align-items: center;">
            <button id="btn-refresh-notifs" class="button" style="padding: 8px 14px; font-size: 13px;">🔄 Refresh</button>
            <button id="btn-page-mark-all-read" class="button primary" style="padding: 8px 14px; font-size: 13px;">✓ Mark all read</button>
          </div>
        </div>

        <div class="card" style="padding: 16px 20px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; background: var(--surface);">
          <div style="display: flex; gap: 8px;" id="tab-group">
            <button class="button notif-tab active" data-tab="ALL">All Alerts</button>
            <button class="button notif-tab" data-tab="UNREAD">Unread <span id="tab-unread-count" style="background:var(--accent); color:#fff; padding:2px 6px; border-radius:10px; font-size:11px; margin-left:4px;">0</span></button>
            <button class="button notif-tab" data-tab="ARCHIVED">Archived</button>
          </div>

          <div style="display: flex; align-items: center; gap: 10px;">
            <label style="font-size: 13px; color: var(--muted);">Category:</label>
            <select id="select-category-filter" style="padding: 6px 12px; border-radius: 6px; border: 1px solid var(--line); background: var(--bg); color: var(--text); font-size: 13px;">
              <option value="ALL">All Categories</option>
              <option value="SHIPMENT">Shipments</option>
              <option value="VESSEL">Vessels & Schedules</option>
              <option value="BERTH">Berths</option>
              <option value="CUSTOMS">Customs Clearance</option>
              <option value="INVOICE">Invoices</option>
              <option value="PAYMENT">Payments</option>
              <option value="SYSTEM">System</option>
            </select>
          </div>
        </div>

        <div id="notifications-list-container" class="card" style="padding: 0; overflow: hidden; background: var(--surface);">
          <div style="padding: 32px; text-align: center; color: var(--muted);"><span class="loading-spinner"></span> Loading notifications...</div>
        </div>

        <div id="pagination-controls" style="display: flex; justify-content: space-between; align-items: center; margin-top: 20px; font-size: 13px; color: var(--muted);">
          <span id="page-info">Showing page 1</span>
          <div style="display: flex; gap: 8px;">
            <button id="btn-prev-page" class="button" disabled style="padding: 6px 12px;">← Previous</button>
            <button id="btn-next-page" class="button" disabled style="padding: 6px 12px;">Next →</button>
          </div>
        </div>
      </div>
    `;

    bindEvents();
    loadNotifications();
  }

  function bindEvents() {
    container.querySelectorAll(".notif-tab").forEach(tabBtn => {
      tabBtn.addEventListener("click", () => {
        container.querySelectorAll(".notif-tab").forEach(b => b.classList.remove("active"));
        tabBtn.classList.add("active");
        activeTab = tabBtn.dataset.tab;
        currentPage = 0;
        loadNotifications();
      });
    });

    container.querySelector("#select-category-filter")?.addEventListener("change", (e) => {
      selectedCategory = e.target.value;
      currentPage = 0;
      loadNotifications();
    });

    container.querySelector("#btn-refresh-notifs")?.addEventListener("click", () => {
      loadNotifications();
    });

    container.querySelector("#btn-page-mark-all-read")?.addEventListener("click", async () => {
      try {
        await api.put("notifications/read-all");
        toast?.("All notifications marked as read");
        loadNotifications();
        window.dispatchEvent(new CustomEvent("notifications-updated"));
      } catch (err) {
        toast?.("Failed to mark all as read: " + err.message);
      }
    });

    container.querySelector("#btn-prev-page")?.addEventListener("click", () => {
      if (currentPage > 0) {
        currentPage--;
        loadNotifications();
      }
    });

    container.querySelector("#btn-next-page")?.addEventListener("click", () => {
      currentPage++;
      loadNotifications();
    });
  }

  async function loadNotifications() {
    const listContainer = container.querySelector("#notifications-list-container");
    if (!listContainer) return;

    listContainer.innerHTML = `<div style="padding: 32px; text-align: center; color: var(--muted);"><span class="loading-spinner"></span> Loading notifications...</div>`;

    const params = new URLSearchParams();
    params.set("page", currentPage);
    params.set("size", pageSize);

    if (activeTab === "UNREAD") {
      params.set("unreadOnly", "true");
      params.set("archived", "false");
    } else if (activeTab === "ARCHIVED") {
      params.set("archived", "true");
    } else {
      params.set("archived", "false");
    }

    if (selectedCategory !== "ALL") {
      params.set("category", selectedCategory);
    }

    try {
      const res = await api.get(`notifications?${params.toString()}`);
      const data = res.content || res || [];
      const totalPages = res.totalPages || 1;
      const totalElements = res.totalElements || data.length;

      // Update unread count pill
      try {
        const unreadRes = await api.get("notifications/unread-count");
        const count = unreadRes.unreadCount || 0;
        const pill = container.querySelector("#tab-unread-count");
        if (pill) pill.textContent = count;
      } catch (e) {}

      if (data.length === 0) {
        listContainer.innerHTML = `
          <div style="padding: 48px 24px; text-align: center; color: var(--muted);">
            <div style="font-size: 36px; margin-bottom: 8px;">🔕</div>
            <h3 style="font-size: 16px; margin: 0; font-weight: 600;">No notifications found</h3>
            <p style="margin: 4px 0 0 0; font-size: 13px;">There are no alerts matching your current filters.</p>
          </div>
        `;
      } else {
        const rowsHtml = data.map(item => renderNotificationItem(item)).join("");
        listContainer.innerHTML = `<div class="notif-feed-list">${rowsHtml}</div>`;
        bindItemActions(listContainer);
      }

      // Update pagination
      const pageInfo = container.querySelector("#page-info");
      if (pageInfo) pageInfo.textContent = `Showing page ${currentPage + 1} of ${totalPages} (${totalElements} items)`;

      const prevBtn = container.querySelector("#btn-prev-page");
      const nextBtn = container.querySelector("#btn-next-page");
      if (prevBtn) prevBtn.disabled = currentPage === 0;
      if (nextBtn) nextBtn.disabled = currentPage >= totalPages - 1;

    } catch (err) {
      listContainer.innerHTML = `
        <div style="padding: 24px; text-align: center; color: #e07b65;">
          Failed to load notifications: ${escapeHtml(err.message)}
          <br><button class="button" id="btn-retry-notifs" style="margin-top: 12px; padding: 6px 12px;">Retry</button>
        </div>
      `;
      container.querySelector("#btn-retry-notifs")?.addEventListener("click", loadNotifications);
    }
  }

  function renderNotificationItem(item) {
    const isUnread = !item.readStatus;
    const isArchived = Boolean(item.archived);
    const category = item.type || "SYSTEM";
    const categoryIcon = getCategoryIcon(category);
    const priority = item.priority || "NORMAL";
    const timeAgoStr = formatTimestamp(item.createdAt);
    const targetLink = getEntityLink(item.entityName, item.entityId);

    return `
      <div class="notif-feed-item ${isUnread ? "unread" : ""}" style="display: flex; gap: 16px; padding: 16px 20px; border-bottom: 1px solid var(--line); background: ${isUnread ? "rgba(224,123,101,0.06)" : "transparent"}; transition: background 0.2s;">
        <div class="notif-icon-bubble" style="width: 40px; height: 40px; border-radius: 50%; background: var(--surface-light, #252e3e); display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0;">
          ${categoryIcon}
        </div>
        <div style="flex: 1; min-width: 0;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 4px;">
            <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
              <strong style="font-size: 14px; color: var(--text);">${escapeHtml(item.title)}</strong>
              ${isUnread ? `<span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: var(--accent);"></span>` : ""}
              ${priority === "URGENT" || priority === "HIGH" ? `<span style="font-size: 10px; font-weight: bold; background: #e07b65; color: #fff; padding: 2px 6px; border-radius: 4px;">${priority}</span>` : ""}
              <span style="font-size: 11px; padding: 2px 8px; border-radius: 12px; background: var(--surface-light, #252e3e); color: var(--muted);">${escapeHtml(category)}</span>
            </div>
            <span style="font-size: 12px; color: var(--muted); white-space: nowrap;">${timeAgoStr}</span>
          </div>
          <p style="margin: 0 0 10px 0; font-size: 13px; color: var(--text-light, #a0aec0); line-height: 1.4;">${escapeHtml(item.message)}</p>
          <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
            ${targetLink ? `<a href="${targetLink}" class="button" style="padding: 4px 10px; font-size: 12px; text-decoration: none;">View Details →</a>` : ""}
            ${isUnread ? `<button class="btn-item-read button" data-id="${item.id}" style="padding: 4px 10px; font-size: 12px;">Mark as read</button>` : ""}
            ${!isArchived ? `<button class="btn-item-archive button" data-id="${item.id}" style="padding: 4px 10px; font-size: 12px;">Archive</button>` : ""}
            <button class="btn-item-delete button" data-id="${item.id}" style="padding: 4px 10px; font-size: 12px; color: #e07b65;">Delete</button>
          </div>
        </div>
      </div>
    `;
  }

  function bindItemActions(listContainer) {
    listContainer.querySelectorAll(".btn-item-read").forEach(btn => {
      btn.addEventListener("click", async () => {
        try {
          await api.put(`notifications/${btn.dataset.id}/read`);
          loadNotifications();
          window.dispatchEvent(new CustomEvent("notifications-updated"));
        } catch (err) {
          toast?.("Error: " + err.message);
        }
      });
    });

    listContainer.querySelectorAll(".btn-item-archive").forEach(btn => {
      btn.addEventListener("click", async () => {
        try {
          await api.put(`notifications/${btn.dataset.id}/archive`);
          toast?.("Notification archived");
          loadNotifications();
          window.dispatchEvent(new CustomEvent("notifications-updated"));
        } catch (err) {
          toast?.("Error: " + err.message);
        }
      });
    });

    listContainer.querySelectorAll(".btn-item-delete").forEach(btn => {
      btn.addEventListener("click", async () => {
        if (!confirm("Delete this notification?")) return;
        try {
          await api.delete(`notifications/${btn.dataset.id}`);
          toast?.("Notification deleted");
          loadNotifications();
          window.dispatchEvent(new CustomEvent("notifications-updated"));
        } catch (err) {
          toast?.("Error: " + err.message);
        }
      });
    });
  }

  function getCategoryIcon(cat) {
    switch ((cat || "").toUpperCase()) {
      case "SHIPMENT": return "⇄";
      case "VESSEL": return "◉";
      case "BERTH": return "◫";
      case "CUSTOMS": return "✓";
      case "INVOICE": return "$";
      case "PAYMENT": return "◒";
      default: return "🔔";
    }
  }

  function getEntityLink(entityName, entityId) {
    if (!entityName) return null;
    const name = entityName.toLowerCase();
    if (name.includes("shipment")) return "shipments.html";
    if (name.includes("vessel") || name.includes("schedule")) return "schedules.html";
    if (name.includes("customs")) return "customs.html";
    if (name.includes("invoice")) return "invoices.html";
    if (name.includes("payment")) return "payments.html";
    return null;
  }

  function formatTimestamp(isoStr) {
    if (!isoStr) return "";
    const date = new Date(isoStr);
    const now = new Date();
    const diffSecs = Math.floor((now - date) / 1000);

    if (diffSecs < 60) return "Just now";
    if (diffSecs < 3600) return `${Math.floor(diffSecs / 60)} min ago`;
    if (diffSecs < 86400) return `${Math.floor(diffSecs / 3600)} hour(s) ago`;
    return date.toLocaleDateString() + " " + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }
}
