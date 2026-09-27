import { api } from "../api/api.js";

export function initUsersModule(container, toastFn) {
  let usersData = [];
  let pageNum = 0;
  let totalPages = 1;
  let searchQuery = "";

  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / USER MANAGEMENT</p>
        <h1>User Management & Security Roles</h1>
        <p>Admin workspace to manage team accounts, security roles, and active permissions.</p>
      </div>
      <div class="heading-actions">
        <button class="button primary" id="btn-create-user-modal">+ Add New User</button>
      </div>
    </section>

    <div class="error-banner" id="users-error-banner" hidden style="margin-bottom: 16px;"></div>

    <section class="stats-grid compact">
      <article class="stat-card">
        <div class="stat-icon purple">♙</div>
        <div><span>Total Accounts</span><strong id="stat-total-users">-</strong><small>Registered users</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon teal">✓</div>
        <div><span>Active Users</span><strong id="stat-active-users">-</strong><small>Enabled accounts</small></div>
      </article>
      <article class="stat-card">
        <div class="stat-icon blue">◷</div>
        <div><span>Administrators</span><strong id="stat-admin-count">-</strong><small>Full access</small></div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>User Accounts</h2>
          <p>Modify user access roles, account status, and team credentials.</p>
        </div>
        <div class="toolbar">
          <input class="table-search" id="user-search-input" placeholder="Search accounts..." aria-label="Search accounts">
          <button class="button" id="btn-refresh-users">↻ Refresh</button>
        </div>
      </div>
      <div class="table-wrap">
        <table id="users-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>User Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="users-table-body">
            <tr><td colspan="6" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading user accounts...</td></tr>
          </tbody>
        </table>
        <div class="empty-state" id="users-empty-state" hidden>No user accounts found.</div>
      </div>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-top:16px; font-size:12px; color:var(--muted);">
        <span id="users-page-info">Showing page 1</span>
        <div>
          <button class="button" id="btn-prev-page" disabled>Previous</button>
          <button class="button" id="btn-next-page" disabled style="margin-left:6px;">Next</button>
        </div>
      </div>
    </section>

    <!-- Create User Modal -->
    <div class="modal-overlay" id="create-user-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3>Create New User Account</h3>
          <button class="modal-close" id="btn-close-create-modal">×</button>
        </div>
        <div class="modal-body">
          <div class="error-banner" id="create-modal-error" hidden></div>
          <div class="field">
            <label for="create-name">Name *</label>
            <input type="text" id="create-name" placeholder="John Doe" required>
          </div>
          <div class="field">
            <label for="create-full-name">Full Name</label>
            <input type="text" id="create-full-name" placeholder="Johnathan Doe">
          </div>
          <div class="field">
            <label for="create-email">Email Address *</label>
            <input type="email" id="create-email" placeholder="john.doe@port.example" required>
          </div>
          <div class="field">
            <label for="create-password">Initial Password * (min 8 characters)</label>
            <input type="password" id="create-password" placeholder="••••••••" required>
          </div>
          <div class="field">
            <label for="create-role">Security Role *</label>
            <select id="create-role" style="padding:10px; border:1px solid var(--line); border-radius:8px; background:var(--surface); color:var(--text);">
              <option value="VIEWER">VIEWER - Read only access</option>
              <option value="SHIPPING_AGENT">SHIPPING_AGENT - Manage shipments & cargo</option>
              <option value="PORT_MANAGER">PORT_MANAGER - Manage berths & operations</option>
              <option value="ADMIN">ADMIN - Full administrative access</option>
            </select>
          </div>
          <div class="field" style="flex-direction:row; align-items:center; gap:8px;">
            <input type="checkbox" id="create-enabled" checked style="width:16px; height:16px;">
            <label for="create-enabled" style="font-weight:normal;">Account Enabled (Active)</label>
          </div>
        </div>
        <div class="modal-footer">
          <button class="button" id="btn-cancel-create-modal">Cancel</button>
          <button class="button primary" id="btn-save-create-user">Create Account</button>
        </div>
      </div>
    </div>

    <!-- Edit User Modal -->
    <div class="modal-overlay" id="edit-user-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3>Edit User Profile</h3>
          <button class="modal-close" id="btn-close-edit-modal">×</button>
        </div>
        <div class="modal-body">
          <div class="error-banner" id="edit-modal-error" hidden></div>
          <input type="hidden" id="edit-user-id">
          <div class="field">
            <label for="edit-name">Name *</label>
            <input type="text" id="edit-name" required>
          </div>
          <div class="field">
            <label for="edit-full-name">Full Name</label>
            <input type="text" id="edit-full-name">
          </div>
          <div class="field">
            <label for="edit-email">Email Address *</label>
            <input type="email" id="edit-email" required>
          </div>
          <div class="field" style="flex-direction:row; align-items:center; gap:8px;">
            <input type="checkbox" id="edit-enabled" style="width:16px; height:16px;">
            <label for="edit-enabled" style="font-weight:normal;">Account Enabled (Active)</label>
          </div>
        </div>
        <div class="modal-footer">
          <button class="button" id="btn-cancel-edit-modal">Cancel</button>
          <button class="button primary" id="btn-save-edit-user">Save Changes</button>
        </div>
      </div>
    </div>
  `;

  const tbody = container.querySelector("#users-table-body");
  const emptyState = container.querySelector("#users-empty-state");
  const statTotal = container.querySelector("#stat-total-users");
  const statActive = container.querySelector("#stat-active-users");
  const statAdmins = container.querySelector("#stat-admin-count");
  const pageInfo = container.querySelector("#users-page-info");
  const prevBtn = container.querySelector("#btn-prev-page");
  const nextBtn = container.querySelector("#btn-next-page");
  const errorBanner = container.querySelector("#users-error-banner");

  const createModal = container.querySelector("#create-user-modal");
  const editModal = container.querySelector("#edit-user-modal");

  async function loadUsers(page = 0, search = "") {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 24px;"><span class="loading-spinner"></span> Loading user accounts...</td></tr>`;
    emptyState.hidden = true;
    errorBanner.hidden = true;

    try {
      const response = await api.get("users", { page, size: 20, search });
      usersData = api.unwrap(response);
      pageNum = response.number !== undefined ? response.number : page;
      totalPages = response.totalPages || 1;

      renderTable(usersData);
      updateStats(response.totalElements || usersData.length, usersData);

      pageInfo.textContent = `Page ${pageNum + 1} of ${totalPages} (${response.totalElements || usersData.length} records)`;
      prevBtn.disabled = pageNum <= 0;
      nextBtn.disabled = pageNum >= totalPages - 1;
    } catch (error) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:#e07b65; padding: 24px;">Failed to load users: ${error.message} (Admin permission required)</td></tr>`;
      showError(errorBanner, `Access Denied or Server Error: ${error.message}`);
    }
  }

  function updateStats(total, items) {
    statTotal.textContent = total;
    statActive.textContent = items.filter(u => u.enabled).length;
    statAdmins.textContent = items.filter(u => u.role === "ADMIN" && u.enabled).length;
  }

  function renderTable(items) {
    if (!items || items.length === 0) {
      tbody.innerHTML = "";
      emptyState.hidden = false;
      return;
    }

    emptyState.hidden = true;
    tbody.innerHTML = items.map(u => `
      <tr>
        <td><strong>#${u.id}</strong></td>
        <td>
          <strong>${escapeHtml(u.name)}</strong>
          ${u.fullName && u.fullName !== u.name ? `<small>${escapeHtml(u.fullName)}</small>` : ""}
        </td>
        <td>${escapeHtml(u.email)}</td>
        <td>
          <select class="role-select" data-id="${u.id}" data-current="${u.role}" style="padding:5px 8px; border:1px solid var(--line); border-radius:6px; background:var(--surface); color:var(--text); font-size:11px;">
            <option value="VIEWER" ${u.role === "VIEWER" ? "selected" : ""}>VIEWER</option>
            <option value="SHIPPING_AGENT" ${u.role === "SHIPPING_AGENT" ? "selected" : ""}>SHIPPING_AGENT</option>
            <option value="PORT_MANAGER" ${u.role === "PORT_MANAGER" ? "selected" : ""}>PORT_MANAGER</option>
            <option value="ADMIN" ${u.role === "ADMIN" ? "selected" : ""}>ADMIN</option>
          </select>
        </td>
        <td>
          <span class="status ${u.enabled ? "" : "delayed"}">${u.enabled ? "ACTIVE" : "INACTIVE"}</span>
        </td>
        <td>
          <button class="table-action btn-toggle-status" data-id="${u.id}" data-enabled="${u.enabled}">${u.enabled ? "Deactivate" : "Activate"}</button>
          <button class="table-action btn-edit-user" data-id="${u.id}">Edit</button>
          <button class="table-action danger btn-delete-user" data-id="${u.id}">Delete</button>
        </td>
      </tr>
    `).join("");

    tbody.querySelectorAll(".role-select").forEach(select => {
      select.addEventListener("change", () => handleRoleChange(select.dataset.id, select.dataset.current, select.value, select));
    });

    tbody.querySelectorAll(".btn-toggle-status").forEach(btn => {
      btn.addEventListener("click", () => handleStatusToggle(btn.dataset.id, btn.dataset.enabled === "true"));
    });

    tbody.querySelectorAll(".btn-edit-user").forEach(btn => {
      btn.addEventListener("click", () => openEditModal(btn.dataset.id));
    });

    tbody.querySelectorAll(".btn-delete-user").forEach(btn => {
      btn.addEventListener("click", () => handleDelete(btn.dataset.id));
    });
  }

  async function handleRoleChange(id, currentRole, newRole, selectEl) {
    if (currentRole === newRole) return;
    if (!confirm(`Are you sure you want to change role of User #${id} from ${currentRole} to ${newRole}?`)) {
      selectEl.value = currentRole;
      return;
    }

    try {
      await api.put(`users/${id}/role`, { role: newRole });
      toastFn(`User #${id} role updated to ${newRole}.`);
      loadUsers(pageNum, searchQuery);
    } catch (error) {
      selectEl.value = currentRole;
      alert(`Role change failed: ${error.message}`);
    }
  }

  async function handleStatusToggle(id, currentEnabled) {
    const action = currentEnabled ? "deactivate" : "activate";
    if (!confirm(`Are you sure you want to ${action} User #${id}?`)) return;

    try {
      await api.put(`users/${id}`, { enabled: !currentEnabled });
      toastFn(`User #${id} ${action}d successfully.`);
      loadUsers(pageNum, searchQuery);
    } catch (error) {
      alert(`Action failed: ${error.message}`);
    }
  }

  function openEditModal(id) {
    const user = usersData.find(u => String(u.id) === String(id));
    if (!user) return;

    container.querySelector("#edit-user-id").value = user.id;
    container.querySelector("#edit-name").value = user.name || "";
    container.querySelector("#edit-full-name").value = user.fullName || "";
    container.querySelector("#edit-email").value = user.email || "";
    container.querySelector("#edit-enabled").checked = user.enabled;
    container.querySelector("#edit-modal-error").hidden = true;

    editModal.classList.add("show");
  }

  async function handleSaveEdit() {
    const id = container.querySelector("#edit-user-id").value;
    const name = container.querySelector("#edit-name").value.trim();
    const fullName = container.querySelector("#edit-full-name").value.trim();
    const email = container.querySelector("#edit-email").value.trim();
    const enabled = container.querySelector("#edit-enabled").checked;
    const errBox = container.querySelector("#edit-modal-error");

    if (!name || !email) {
      showError(errBox, "Name and Email are required.");
      return;
    }

    try {
      await api.put(`users/${id}`, { name, fullName, email, enabled });
      toastFn(`User #${id} details updated.`);
      editModal.classList.remove("show");
      loadUsers(pageNum, searchQuery);
    } catch (error) {
      showError(errBox, error.message);
    }
  }

  async function handleSaveCreate() {
    const name = container.querySelector("#create-name").value.trim();
    const fullName = container.querySelector("#create-full-name").value.trim();
    const email = container.querySelector("#create-email").value.trim();
    const password = container.querySelector("#create-password").value;
    const role = container.querySelector("#create-role").value;
    const enabled = container.querySelector("#create-enabled").checked;
    const errBox = container.querySelector("#create-modal-error");

    if (!name || !email || !password) {
      showError(errBox, "Name, Email, and Password are required.");
      return;
    }
    if (password.length < 8) {
      showError(errBox, "Password must be at least 8 characters long.");
      return;
    }

    try {
      await api.post("users", { name, fullName, email, password, role, enabled });
      toastFn(`Created new user account for ${email}.`);
      createModal.classList.remove("show");
      // Reset form
      container.querySelector("#create-name").value = "";
      container.querySelector("#create-full-name").value = "";
      container.querySelector("#create-email").value = "";
      container.querySelector("#create-password").value = "";
      loadUsers(0, searchQuery);
    } catch (error) {
      showError(errBox, error.message);
    }
  }

  async function handleDelete(id) {
    const user = usersData.find(u => String(u.id) === String(id));
    const name = user ? user.name : `#${id}`;

    if (!confirm(`Are you sure you want to permanently delete user account "${name}"?`)) return;

    try {
      await api.delete(`users/${id}`);
      toastFn(`User account "${name}" deleted.`);
      loadUsers(pageNum, searchQuery);
    } catch (error) {
      alert(`Delete failed: ${error.message}`);
    }
  }

  function showError(el, message) {
    if (!el) return;
    el.textContent = message;
    el.hidden = false;
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  // Bind Event Listeners
  container.querySelector("#btn-refresh-users").addEventListener("click", () => loadUsers(pageNum, searchQuery));
  prevBtn.addEventListener("click", () => { if (pageNum > 0) loadUsers(pageNum - 1, searchQuery); });
  nextBtn.addEventListener("click", () => { if (pageNum < totalPages - 1) loadUsers(pageNum + 1, searchQuery); });

  const searchInput = container.querySelector("#user-search-input");
  let searchDebounce = null;
  searchInput.addEventListener("input", () => {
    clearTimeout(searchDebounce);
    searchDebounce = setTimeout(() => {
      searchQuery = searchInput.value.trim();
      loadUsers(0, searchQuery);
    }, 300);
  });

  // Modal event listeners
  container.querySelector("#btn-create-user-modal").addEventListener("click", () => {
    container.querySelector("#create-modal-error").hidden = true;
    createModal.classList.add("show");
  });

  container.querySelector("#btn-close-create-modal").addEventListener("click", () => createModal.classList.remove("show"));
  container.querySelector("#btn-cancel-create-modal").addEventListener("click", () => createModal.classList.remove("show"));
  container.querySelector("#btn-save-create-user").addEventListener("click", handleSaveCreate);

  container.querySelector("#btn-close-edit-modal").addEventListener("click", () => editModal.classList.remove("show"));
  container.querySelector("#btn-cancel-edit-modal").addEventListener("click", () => editModal.classList.remove("show"));
  container.querySelector("#btn-save-edit-user").addEventListener("click", handleSaveEdit);

  loadUsers(0);
}
