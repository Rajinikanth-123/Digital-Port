import { api } from "../api/api.js";

export function initProfileModule(container, toast) {
  let profileData = null;
  let preferencesData = null;
  let securityData = null;

  container.innerHTML = `
    <section class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / MY PROFILE</p>
        <h1>My Profile & Settings</h1>
        <p>Manage your personal information, security credentials, and interface preferences.</p>
      </div>
    </section>

    <div id="profile-alert-container"></div>

    <div style="display: grid; grid-template-columns: 1fr; gap: 24px; max-width: 1000px;">

      <!-- SECTION 1: PROFILE INFORMATION -->
      <section class="panel">
        <div class="panel-title">
          <div>
            <h2>Profile Information</h2>
            <p>Update your display name, full name, email address, and contact details.</p>
          </div>
          <span class="status-pill status-active" id="profile-role-badge">Loading...</span>
        </div>
        <form id="form-update-profile" style="margin-top: 16px; display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
          <div>
            <label style="font-size:12px; font-weight:600; display:block; margin-bottom:6px; color:var(--muted);">Display Name</label>
            <input type="text" id="prof-name" class="input-field" placeholder="John Doe" required style="width:100%; padding:10px 12px; border:1px solid var(--line); border-radius:6px; background:var(--background); color:var(--text);">
          </div>
          <div>
            <label style="font-size:12px; font-weight:600; display:block; margin-bottom:6px; color:var(--muted);">Full Name</label>
            <input type="text" id="prof-fullname" class="input-field" placeholder="Johnathan Doe" style="width:100%; padding:10px 12px; border:1px solid var(--line); border-radius:6px; background:var(--background); color:var(--text);">
          </div>
          <div>
            <label style="font-size:12px; font-weight:600; display:block; margin-bottom:6px; color:var(--muted);">Email Address</label>
            <input type="email" id="prof-email" class="input-field" placeholder="john.doe@portshipping.com" required style="width:100%; padding:10px 12px; border:1px solid var(--line); border-radius:6px; background:var(--background); color:var(--text);">
          </div>
          <div>
            <label style="font-size:12px; font-weight:600; display:block; margin-bottom:6px; color:var(--muted);">Phone Number</label>
            <input type="tel" id="prof-phone" class="input-field" placeholder="+1-555-0199" style="width:100%; padding:10px 12px; border:1px solid var(--line); border-radius:6px; background:var(--background); color:var(--text);">
          </div>
          <div style="grid-column: 1 / -1; display:flex; justify-content:space-between; align-items:center; margin-top:8px;">
            <span style="font-size:12px; color:var(--muted);" id="prof-created-at">Account created: --</span>
            <button type="submit" id="btn-save-profile" class="button primary">Save Profile Changes</button>
          </div>
        </form>
      </section>

      <!-- SECTION 2: CHANGE PASSWORD -->
      <section class="panel">
        <div class="panel-title">
          <div>
            <h2>Change Password</h2>
            <p>Ensure your account is using a long, random password to stay secure.</p>
          </div>
        </div>
        <form id="form-change-password" style="margin-top: 16px; display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px;">
          <div>
            <label style="font-size:12px; font-weight:600; display:block; margin-bottom:6px; color:var(--muted);">Current Password</label>
            <input type="password" id="pwd-current" class="input-field" placeholder="••••••••" required style="width:100%; padding:10px 12px; border:1px solid var(--line); border-radius:6px; background:var(--background); color:var(--text);">
          </div>
          <div>
            <label style="font-size:12px; font-weight:600; display:block; margin-bottom:6px; color:var(--muted);">New Password</label>
            <input type="password" id="pwd-new" class="input-field" placeholder="••••••••" required minlength="8" style="width:100%; padding:10px 12px; border:1px solid var(--line); border-radius:6px; background:var(--background); color:var(--text);">
          </div>
          <div>
            <label style="font-size:12px; font-weight:600; display:block; margin-bottom:6px; color:var(--muted);">Confirm New Password</label>
            <input type="password" id="pwd-confirm" class="input-field" placeholder="••••••••" required minlength="8" style="width:100%; padding:10px 12px; border:1px solid var(--line); border-radius:6px; background:var(--background); color:var(--text);">
          </div>
          <div style="grid-column: 1 / -1; display:flex; justify-content:space-between; align-items:center; margin-top:8px;">
            <small style="color:var(--muted);">Password policy: Minimum 8 characters long.</small>
            <button type="submit" id="btn-change-password" class="button primary">Update Password</button>
          </div>
        </form>
      </section>

      <!-- SECTION 3: DISPLAY & LOCALIZATION PREFERENCES -->
      <section class="panel">
        <div class="panel-title">
          <div>
            <h2>Display & Preferences</h2>
            <p>Customize system theme, language, and timezone settings.</p>
          </div>
        </div>
        <form id="form-update-preferences" style="margin-top: 16px; display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px;">
          <div>
            <label style="font-size:12px; font-weight:600; display:block; margin-bottom:6px; color:var(--muted);">Application Theme</label>
            <select id="pref-theme" style="width:100%; padding:10px 12px; border:1px solid var(--line); border-radius:6px; background:var(--background); color:var(--text);">
              <option value="light">Light Mode</option>
              <option value="dark">Dark Mode</option>
              <option value="system">System Default</option>
            </select>
          </div>
          <div>
            <label style="font-size:12px; font-weight:600; display:block; margin-bottom:6px; color:var(--muted);">Preferred Language</label>
            <select id="pref-lang" style="width:100%; padding:10px 12px; border:1px solid var(--line); border-radius:6px; background:var(--background); color:var(--text);">
              <option value="en">English (US)</option>
              <option value="es">Spanish (Español)</option>
              <option value="fr">French (Français)</option>
            </select>
          </div>
          <div>
            <label style="font-size:12px; font-weight:600; display:block; margin-bottom:6px; color:var(--muted);">Timezone</label>
            <select id="pref-tz" style="width:100%; padding:10px 12px; border:1px solid var(--line); border-radius:6px; background:var(--background); color:var(--text);">
              <option value="UTC">UTC (Universal Coordinated Time)</option>
              <option value="America/New_York">Eastern Time (America/New_York)</option>
              <option value="Europe/London">London (Europe/London)</option>
              <option value="Asia/Tokyo">Tokyo (Asia/Tokyo)</option>
            </select>
          </div>
          <div style="grid-column: 1 / -1; text-align:right; margin-top:8px;">
            <button type="submit" id="btn-save-prefs" class="button primary">Save Display Settings</button>
          </div>
        </form>
      </section>

      <!-- SECTION 4: NOTIFICATION PREFERENCES -->
      <section class="panel">
        <div class="panel-title">
          <div>
            <h2>Notification Preferences</h2>
            <p>Configure which categories of in-app alerts you want to receive.</p>
          </div>
        </div>
        <form id="form-notification-preferences" style="margin-top: 16px;">
          <div style="display:grid; grid-template-columns: 1fr 1fr; gap:16px;">
            <label style="display:flex; align-items:center; gap:12px; padding:12px; border:1px solid var(--line); border-radius:8px; background:var(--surface); cursor:pointer;">
              <input type="checkbox" id="notif-shipments" checked style="width:18px; height:18px; accent-color:var(--accent);">
              <div>
                <strong style="display:block; font-size:14px;">Shipment Notifications</strong>
                <span style="font-size:12px; color:var(--muted);">Receive status updates when shipments move or are delivered.</span>
              </div>
            </label>

            <label style="display:flex; align-items:center; gap:12px; padding:12px; border:1px solid var(--line); border-radius:8px; background:var(--surface); cursor:pointer;">
              <input type="checkbox" id="notif-vessels" checked style="width:18px; height:18px; accent-color:var(--accent);">
              <div>
                <strong style="display:block; font-size:14px;">Vessel & Berth Alerts</strong>
                <span style="font-size:12px; color:var(--muted);">Arrivals, departures, and berth schedule adjustments.</span>
              </div>
            </label>

            <label style="display:flex; align-items:center; gap:12px; padding:12px; border:1px solid var(--line); border-radius:8px; background:var(--surface); cursor:pointer;">
              <input type="checkbox" id="notif-customs" checked style="width:18px; height:18px; accent-color:var(--accent);">
              <div>
                <strong style="display:block; font-size:14px;">Customs Clearance Notifications</strong>
                <span style="font-size:12px; color:var(--muted);">Filing updates, approvals, or required document holds.</span>
              </div>
            </label>

            <label style="display:flex; align-items:center; gap:12px; padding:12px; border:1px solid var(--line); border-radius:8px; background:var(--surface); cursor:pointer;">
              <input type="checkbox" id="notif-invoices" checked style="width:18px; height:18px; accent-color:var(--accent);">
              <div>
                <strong style="display:block; font-size:14px;">Invoice & Payment Alerts</strong>
                <span style="font-size:12px; color:var(--muted);">New billing statements, overdue alerts, and receipts.</span>
              </div>
            </label>

            <label style="display:flex; align-items:center; gap:12px; padding:12px; border:1px solid var(--line); border-radius:8px; background:var(--surface); cursor:pointer; grid-column:1 / -1;">
              <input type="checkbox" id="notif-system" checked style="width:18px; height:18px; accent-color:var(--accent);">
              <div>
                <strong style="display:block; font-size:14px;">General System Announcements</strong>
                <span style="font-size:12px; color:var(--muted);">Platform security alerts, maintenance notices, and broadcast updates.</span>
              </div>
            </label>
          </div>
          <div style="text-align:right; margin-top:16px;">
            <button type="submit" id="btn-save-notif-prefs" class="button primary">Save Notification Settings</button>
          </div>
        </form>
      </section>

      <!-- SECTION 5: ACCOUNT SECURITY & DEACTIVATION -->
      <section class="panel" style="border-color: rgba(224, 123, 101, 0.3);">
        <div class="panel-title">
          <div>
            <h2>Account Security & Management</h2>
            <p>Review active account status or initiate self-service account deactivation.</p>
          </div>
        </div>
        <div style="margin-top:16px; display:flex; justify-content:space-between; align-items:center; padding:16px; background:var(--surface); border:1px solid var(--line); border-radius:8px;">
          <div>
            <strong style="display:block; font-size:14px;">Account Status: <span id="security-status" style="color:#65c466;">Active</span></strong>
            <span style="font-size:12px; color:var(--muted);" id="security-details">Assigned Role: --</span>
          </div>
          <button id="btn-open-deactivate-modal" style="background:#e07b65; color:#fff; border:none; padding:10px 16px; border-radius:6px; font-weight:600; cursor:pointer;">Deactivate Account</button>
        </div>
      </section>

    </div>

    <!-- DEACTIVATION MODAL -->
    <div id="deactivate-modal" style="display:none; position:fixed; inset:0; background:rgba(0,0,0,0.6); z-index:999; justify-content:center; align-items:center;">
      <div style="background:var(--surface); border:1px solid var(--line); border-radius:12px; padding:24px; width:440px; max-width:90%; box-shadow:0 12px 32px rgba(0,0,0,0.4);">
        <h3 style="margin-top:0; color:#e07b65;">Confirm Account Deactivation</h3>
        <p style="font-size:13px; color:var(--muted); margin:12px 0;">Deactivating your account will prevent future sign-ins. You will be logged out immediately.</p>
        <form id="form-deactivate-account">
          <div style="margin-bottom:16px;">
            <label style="font-size:12px; font-weight:600; display:block; margin-bottom:6px; color:var(--muted);">Enter Current Password</label>
            <input type="password" id="deactivate-password" class="input-field" required placeholder="••••••••" style="width:100%; padding:10px 12px; border:1px solid var(--line); border-radius:6px; background:var(--background); color:var(--text);">
          </div>
          <div style="margin-bottom:16px;">
            <label style="font-size:12px; font-weight:600; display:block; margin-bottom:6px; color:var(--muted);">Reason for Leaving (Optional)</label>
            <input type="text" id="deactivate-reason" class="input-field" placeholder="Optional feedback..." style="width:100%; padding:10px 12px; border:1px solid var(--line); border-radius:6px; background:var(--background); color:var(--text);">
          </div>
          <div style="display:flex; justify-content:flex-end; gap:12px;">
            <button type="button" id="btn-cancel-deactivate" class="button">Cancel</button>
            <button type="submit" style="background:#e07b65; color:#fff; border:none; padding:10px 16px; border-radius:6px; font-weight:600; cursor:pointer;">Deactivate & Sign Out</button>
          </div>
        </form>
      </div>
    </div>
  `;

  loadAllProfileData();
  bindFormEvents();

  async function loadAllProfileData() {
    try {
      profileData = await api.get("users/me");
      if (profileData) {
        document.querySelector("#prof-name").value = profileData.name || "";
        document.querySelector("#prof-fullname").value = profileData.fullName || "";
        document.querySelector("#prof-email").value = profileData.email || "";
        document.querySelector("#prof-phone").value = profileData.phoneNumber || "";
        document.querySelector("#profile-role-badge").textContent = profileData.role || "USER";
        document.querySelector("#prof-created-at").textContent = "Account created: " + (profileData.createdAt ? new Date(profileData.createdAt).toLocaleDateString() : "--");
        document.querySelector("#security-details").textContent = `Assigned Role: ${profileData.role || 'USER'} | Email: ${profileData.email}`;
      }
    } catch (err) {
      showAlert("danger", "Failed to load profile data: " + (err.message || err));
    }

    try {
      preferencesData = await api.get("users/me/preferences");
      if (preferencesData) {
        document.querySelector("#pref-theme").value = preferencesData.theme || "light";
        document.querySelector("#pref-lang").value = preferencesData.language || "en";
        document.querySelector("#pref-tz").value = preferencesData.timezone || "UTC";

        document.querySelector("#notif-shipments").checked = preferencesData.notifyShipments !== false;
        document.querySelector("#notif-vessels").checked = preferencesData.notifyVessels !== false;
        document.querySelector("#notif-customs").checked = preferencesData.notifyCustoms !== false;
        document.querySelector("#notif-invoices").checked = preferencesData.notifyInvoices !== false;
        document.querySelector("#notif-system").checked = preferencesData.notifySystem !== false;
      }
    } catch (err) {
      console.warn("Could not load preferences:", err);
    }
  }

  function bindFormEvents() {
    // 1. Profile Update Form
    const profileForm = document.querySelector("#form-update-profile");
    profileForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const btn = document.querySelector("#btn-save-profile");
      btn.disabled = true;
      btn.textContent = "Saving...";

      try {
        const body = {
          name: document.querySelector("#prof-name").value,
          fullName: document.querySelector("#prof-fullname").value,
          email: document.querySelector("#prof-email").value,
          phoneNumber: document.querySelector("#prof-phone").value
        };
        const updated = await api.put("users/me", body);
        showAlert("success", "Profile information updated successfully.");
        if (toast) toast("Profile updated", "success");

        // Refresh stored user session if email or name changed
        sessionStorage.setItem("harbor_user", JSON.stringify(updated));
      } catch (err) {
        showAlert("danger", "Failed to update profile: " + (err.message || err));
      } finally {
        btn.disabled = false;
        btn.textContent = "Save Profile Changes";
      }
    });

    // 2. Change Password Form
    const passwordForm = document.querySelector("#form-change-password");
    passwordForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const cur = document.querySelector("#pwd-current").value;
      const n1 = document.querySelector("#pwd-new").value;
      const n2 = document.querySelector("#pwd-confirm").value;

      if (n1 !== n2) {
        showAlert("danger", "New password and confirmation password do not match.");
        return;
      }

      const btn = document.querySelector("#btn-change-password");
      btn.disabled = true;
      btn.textContent = "Updating...";

      try {
        await api.post("users/me/change-password", { currentPassword: cur, newPassword: n1, confirmPassword: n2 });
        showAlert("success", "Password updated successfully.");
        if (toast) toast("Password changed", "success");
        passwordForm.reset();
      } catch (err) {
        showAlert("danger", "Failed to change password: " + (err.message || err));
      } finally {
        btn.disabled = false;
        btn.textContent = "Update Password";
      }
    });

    // 3. Display Preferences Form
    const prefForm = document.querySelector("#form-update-preferences");
    prefForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const btn = document.querySelector("#btn-save-prefs");
      btn.disabled = true;

      try {
        const body = {
          theme: document.querySelector("#pref-theme").value,
          language: document.querySelector("#pref-lang").value,
          timezone: document.querySelector("#pref-tz").value
        };
        const updated = await api.put("users/me/preferences", body);
        showAlert("success", "Display & localization preferences saved.");
        if (toast) toast("Preferences saved", "success");

        // Apply theme immediately
        if (updated.theme === "dark") {
          document.documentElement.classList.add("dark");
        } else {
          document.documentElement.classList.remove("dark");
        }
      } catch (err) {
        showAlert("danger", "Failed to save preferences: " + (err.message || err));
      } finally {
        btn.disabled = false;
      }
    });

    // 4. Notification Preferences Form
    const notifForm = document.querySelector("#form-notification-preferences");
    notifForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const btn = document.querySelector("#btn-save-notif-prefs");
      btn.disabled = true;

      try {
        const body = {
          notifyShipments: document.querySelector("#notif-shipments").checked,
          notifyVessels: document.querySelector("#notif-vessels").checked,
          notifyCustoms: document.querySelector("#notif-customs").checked,
          notifyInvoices: document.querySelector("#notif-invoices").checked,
          notifySystem: document.querySelector("#notif-system").checked
        };
        await api.put("users/me/preferences", body);
        showAlert("success", "Notification preferences saved.");
        if (toast) toast("Notification settings updated", "success");
      } catch (err) {
        showAlert("danger", "Failed to save notification preferences: " + (err.message || err));
      } finally {
        btn.disabled = false;
      }
    });

    // 5. Account Deactivation Modal & Form
    const deactivateModal = document.querySelector("#deactivate-modal");
    document.querySelector("#btn-open-deactivate-modal").addEventListener("click", () => {
      deactivateModal.style.display = "flex";
    });
    document.querySelector("#btn-cancel-deactivate").addEventListener("click", () => {
      deactivateModal.style.display = "none";
    });

    const deactivateForm = document.querySelector("#form-deactivate-account");
    deactivateForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const pwd = document.querySelector("#deactivate-password").value;
      const reason = document.querySelector("#deactivate-reason").value;

      try {
        await api.post("users/me/deactivate", { currentPassword: pwd, reason });
        sessionStorage.clear();
        alert("Account deactivated successfully. You will now be redirected to the login page.");
        location.href = "login.html";
      } catch (err) {
        alert("Failed to deactivate account: " + (err.message || err));
      }
    });
  }

  function showAlert(type, msg) {
    const alertBox = document.querySelector("#profile-alert-container");
    if (!alertBox) return;
    const bg = type === "success" ? "rgba(101,196,102,0.15)" : "rgba(224,123,101,0.15)";
    const border = type === "success" ? "#65c466" : "#e07b65";

    alertBox.innerHTML = `
      <div style="padding:12px 16px; background:${bg}; border:1px solid ${border}; border-radius:8px; margin-bottom:20px; color:var(--text); font-size:13px; display:flex; justify-content:space-between; align-items:center;">
        <span>${msg}</span>
        <button onclick="this.parentElement.remove()" style="background:none; border:none; cursor:pointer; font-size:16px; color:var(--muted);">✕</button>
      </div>
    `;
  }
}
