/**
 * Help Center, Support Tickets & Feedback Module
 */

let activeTicketId = null;
let activeUserRole = null;

export function initHelpModule() {
    console.log("Initializing Help Center & Support Module...");
    detectUserRole();
    setupEventListeners();

    loadFaqs();
    loadMyTickets();

    if (isStaffRole()) {
        const queueTab = document.getElementById("nav-admin-queue-item");
        if (queueTab) queueTab.style.display = "block";
        loadAdminQueue();
    }
}

function detectUserRole() {
    try {
        const userJson = localStorage.getItem("currentUser") || sessionStorage.getItem("currentUser");
        if (userJson) {
            const user = JSON.parse(userJson);
            activeUserRole = user.role ? user.role.toUpperCase() : "VIEWER";
        }
    } catch (e) {
        activeUserRole = "VIEWER";
    }
}

function isStaffRole() {
    return activeUserRole && (activeUserRole.includes("ADMIN") || activeUserRole.includes("PORT_MANAGER") || activeUserRole.includes("SHIPPING_AGENT"));
}

function setupEventListeners() {
    // Search FAQs
    const searchBtn = document.getElementById("faq-search-btn");
    const searchInput = document.getElementById("faq-search-input");
    if (searchBtn) {
        searchBtn.addEventListener("click", () => {
            loadFaqs(searchInput.value, null);
        });
    }
    if (searchInput) {
        searchInput.addEventListener("keypress", (e) => {
            if (e.key === "Enter") loadFaqs(searchInput.value, null);
        });
    }

    // Create ticket form
    const createForm = document.getElementById("create-ticket-form");
    if (createForm) {
        createForm.addEventListener("submit", handleCreateTicket);
    }

    // Star rating selector
    const starContainer = document.getElementById("star-rating-selector");
    if (starContainer) {
        const stars = starContainer.querySelectorAll(".rating-star");
        stars.forEach(star => {
            star.addEventListener("click", function() {
                const rating = parseInt(this.getAttribute("data-rating"));
                document.getElementById("feedback-rating-val").value = rating;
                updateStarRatingUI(rating);
            });
        });
        updateStarRatingUI(5);
    }

    // Submit feedback form
    const feedbackForm = document.getElementById("submit-feedback-form");
    if (feedbackForm) {
        feedbackForm.addEventListener("submit", handleSubmitFeedback);
    }

    // Ticket modal reply form
    const replyForm = document.getElementById("modal-reply-form");
    if (replyForm) {
        replyForm.addEventListener("submit", handleSendReply);
    }

    // Admin controls in modal
    const btnUpdateStatus = document.getElementById("btn-update-status-action");
    if (btnUpdateStatus) {
        btnUpdateStatus.addEventListener("click", handleUpdateStatus);
    }

    const btnAssignAgent = document.getElementById("btn-assign-agent-action");
    if (btnAssignAgent) {
        btnAssignAgent.addEventListener("click", handleAssignAgent);
    }
}

function updateStarRatingUI(rating) {
    const stars = document.querySelectorAll(".rating-star");
    const ratingText = document.getElementById("rating-text");
    const labels = ["Poor", "Fair", "Good", "Very Good", "Excellent"];

    stars.forEach(star => {
        const r = parseInt(star.getAttribute("data-rating"));
        if (r <= rating) {
            star.classList.remove("far");
            star.classList.add("fas");
        } else {
            star.classList.remove("fas");
            star.classList.add("far");
        }
    });

    if (ratingText && labels[rating - 1]) {
        ratingText.textContent = `${rating} / 5 — ${labels[rating - 1]}`;
    }
}

// --- FAQs & HELP CENTER ---

async function loadFaqs(search = null, category = null) {
    const accordion = document.getElementById("faq-accordion");
    const badge = document.getElementById("faq-category-badge");
    if (!accordion) return;

    accordion.innerHTML = `<div class="p-4 text-center text-muted"><i class="fas fa-spinner fa-spin me-2"></i>Loading articles...</div>`;

    if (badge) {
        badge.textContent = category ? category.toUpperCase() : (search ? `SEARCH: "${search}"` : "ALL TOPICS");
    }

    try {
        let url = `/api/help/articles?size=20`;
        if (search) url += `&search=${encodeURIComponent(search)}`;
        if (category) url += `&category=${encodeURIComponent(category)}`;

        const response = await fetch(url);
        if (!response.ok) throw new Error("Failed to fetch articles");
        const page = await response.json();
        const articles = page.content || [];

        if (articles.length === 0) {
            accordion.innerHTML = `<div class="p-4 text-center text-muted"><i class="fas fa-info-circle me-2"></i>No knowledge base articles found matching your query.</div>`;
            return;
        }

        accordion.innerHTML = articles.map((art, idx) => `
            <div class="accordion-item">
                <h2 class="accordion-header" id="heading-${art.id}">
                    <button class="accordion-button ${idx !== 0 ? 'collapsed' : ''}" type="button" data-bs-toggle="collapse" data-bs-target="#collapse-${art.id}" aria-expanded="${idx === 0}">
                        <span class="badge bg-outline-primary me-2 text-primary border border-primary">${art.category}</span>
                        <strong class="text-dark">${escapeHtml(art.title)}</strong>
                    </button>
                </h2>
                <div id="collapse-${art.id}" class="accordion-collapse collapse ${idx === 0 ? 'show' : ''}" data-bs-parent="#faq-accordion">
                    <div class="accordion-body text-secondary lh-lg">
                        ${escapeHtml(art.content)}
                    </div>
                </div>
            </div>
        `).join("");

    } catch (e) {
        console.error("Error loading FAQs:", e);
        accordion.innerHTML = `<div class="p-4 text-center text-danger"><i class="fas fa-exclamation-triangle me-2"></i>Error loading help articles.</div>`;
    }
}

window.filterFaqCategory = function(cat) {
    const searchInput = document.getElementById("faq-search-input");
    if (searchInput) searchInput.value = "";
    loadFaqs(null, cat);
};

// --- MY TICKETS ---

async function loadMyTickets() {
    const tbody = document.getElementById("my-tickets-tbody");
    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted p-4"><i class="fas fa-spinner fa-spin me-2"></i>Loading your tickets...</td></tr>`;

    try {
        const response = await fetch('/api/support/tickets/my?size=20');
        if (!response.ok) throw new Error("Failed to fetch tickets");
        const page = await response.json();
        const tickets = page.content || [];

        if (tickets.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted p-4">You have not submitted any support tickets yet.</td></tr>`;
            return;
        }

        tbody.innerHTML = tickets.map(t => `
            <tr>
                <td><strong class="font-monospace text-primary">${escapeHtml(t.ticketReference)}</strong></td>
                <td><a href="#" onclick="window.openTicketDetailModal(${t.id}); return false;" class="fw-bold text-decoration-none text-dark">${escapeHtml(t.subject)}</a></td>
                <td><span class="badge bg-light text-dark border">${escapeHtml(t.category)}</span></td>
                <td>${getPriorityBadge(t.priority)}</td>
                <td>${getStatusBadge(t.status)}</td>
                <td><small class="text-muted">${formatDate(t.updatedAt)}</small></td>
                <td>
                    <button class="btn btn-sm btn-outline-primary" onclick="window.openTicketDetailModal(${t.id})"><i class="fas fa-comments me-1"></i> View</button>
                </td>
            </tr>
        `).join("");

    } catch (e) {
        console.error("Error loading tickets:", e);
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger p-4">Failed to load support tickets.</td></tr>`;
    }
}

window.refreshMyTickets = function() {
    loadMyTickets();
};

// --- CREATE TICKET ---

async function handleCreateTicket(e) {
    e.preventDefault();
    const btn = document.getElementById("submit-ticket-btn");
    btn.disabled = true;
    btn.innerHTML = `<i class="fas fa-spinner fa-spin me-1"></i> Submitting...`;

    try {
        const formData = new FormData();
        const jsonReq = {
            subject: document.getElementById("ticket-subject").value,
            category: document.getElementById("ticket-category").value,
            priority: document.getElementById("ticket-priority").value,
            description: document.getElementById("ticket-description").value
        };

        formData.append("data", new Blob([JSON.stringify(jsonReq)], { type: "application/json" }));

        const fileInput = document.getElementById("ticket-attachment");
        if (fileInput && fileInput.files.length > 0) {
            formData.append("file", fileInput.files[0]);
        }

        const response = await fetch('/api/support/tickets', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.message || "Failed to create ticket");
        }

        const result = await response.json();
        showToast("Success", `Support ticket ${result.ticketReference} submitted successfully!`, "success");

        document.getElementById("create-ticket-form").reset();
        loadMyTickets();

        // Switch to My Tickets tab
        const myTicketsBtn = document.getElementById("tab-my-tickets-btn");
        if (myTicketsBtn) myTicketsBtn.click();

    } catch (err) {
        showToast("Error", err.message, "danger");
    } finally {
        btn.disabled = false;
        btn.innerHTML = `<i class="fas fa-paper-plane me-1"></i> Submit Ticket`;
    }
}

// --- TICKET DETAIL & CONVERSATION MODAL ---

window.openTicketDetailModal = async function(ticketId) {
    activeTicketId = ticketId;
    const modalEl = document.getElementById("ticketDetailModal");
    const modal = new bootstrap.Modal(modalEl);

    document.getElementById("modal-ticket-ref").textContent = "Loading...";
    document.getElementById("modal-message-thread").innerHTML = `<div class="text-center text-muted p-3"><i class="fas fa-spinner fa-spin me-2"></i>Loading messages...</div>`;

    const adminControls = document.getElementById("modal-admin-controls");
    const internalNoteWrap = document.getElementById("modal-internal-note-check-wrap");

    if (isStaffRole()) {
        if (adminControls) adminControls.style.display = "block";
        if (internalNoteWrap) internalNoteWrap.style.display = "block";
    } else {
        if (adminControls) adminControls.style.display = "none";
        if (internalNoteWrap) internalNoteWrap.style.display = "none";
    }

    modal.show();

    try {
        const [ticketRes, msgsRes] = await Promise.all([
            fetch(`/api/support/tickets/${ticketId}`),
            fetch(`/api/support/tickets/${ticketId}/messages`)
        ]);

        if (!ticketRes.ok || !msgsRes.ok) throw new Error("Failed to load ticket details");

        const ticket = await ticketRes.json();
        const messages = await msgsRes.json();

        document.getElementById("modal-ticket-ref").textContent = `Ticket ${ticket.ticketReference}`;
        document.getElementById("modal-ticket-status").className = `badge ${getStatusBadgeClass(ticket.status)}`;
        document.getElementById("modal-ticket-status").textContent = ticket.status;
        document.getElementById("modal-ticket-priority").className = `badge ${getPriorityBadgeClass(ticket.priority)}`;
        document.getElementById("modal-ticket-priority").textContent = ticket.priority;
        document.getElementById("modal-ticket-subject").textContent = ticket.subject;

        document.getElementById("modal-ticket-meta").textContent =
            `Submitted by ${ticket.createdByName} (${ticket.createdByEmail}) on ${formatDate(ticket.createdAt)} | Category: ${ticket.category} | Assigned: ${ticket.assignedToName || 'Unassigned'}`;

        const statusSelect = document.getElementById("modal-change-status-select");
        if (statusSelect) statusSelect.value = ticket.status;

        renderMessages(messages);

    } catch (err) {
        showToast("Error", err.message, "danger");
    }
};

function renderMessages(messages) {
    const container = document.getElementById("modal-message-thread");
    if (!container) return;

    if (messages.length === 0) {
        container.innerHTML = `<div class="text-center text-muted p-3">No messages in this ticket thread.</div>`;
        return;
    }

    container.innerHTML = messages.map(m => {
        const isInternal = m.internalNote;
        const cardBg = isInternal ? 'bg-warning-subtle border-warning' : 'bg-white border';
        return `
            <div class="card ${cardBg} shadow-sm p-3">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <div>
                        <strong class="text-dark">${escapeHtml(m.senderName)}</strong>
                        <span class="badge bg-secondary ms-1">${escapeHtml(m.senderRole || 'User')}</span>
                        ${isInternal ? '<span class="badge bg-warning text-dark ms-1"><i class="fas fa-lock me-1"></i>Internal Staff Note</span>' : ''}
                    </div>
                    <small class="text-muted">${formatDate(m.createdAt)}</small>
                </div>
                <div class="text-secondary lh-relaxed mb-2" style="white-space: pre-wrap;">${escapeHtml(m.messageBody)}</div>
                ${m.hasAttachment ? `
                    <div class="pt-2 border-top">
                        <a href="/api/support/tickets/${m.ticketId}/attachments/${m.id}" target="_blank" class="btn btn-sm btn-light border text-primary">
                            <i class="fas fa-paperclip me-1"></i> Download Attachment: ${escapeHtml(m.attachmentFilename || 'File')}
                        </a>
                    </div>
                ` : ''}
            </div>
        `;
    }).join("");
}

async function handleSendReply(e) {
    e.preventDefault();
    if (!activeTicketId) return;

    const btn = document.getElementById("btn-send-reply");
    btn.disabled = true;

    try {
        const formData = new FormData();
        const jsonReq = {
            messageBody: document.getElementById("modal-reply-body").value,
            internalNote: document.getElementById("modal-is-internal-note") ? document.getElementById("modal-is-internal-note").checked : false
        };

        formData.append("data", new Blob([JSON.stringify(jsonReq)], { type: "application/json" }));

        const fileInput = document.getElementById("modal-reply-file");
        if (fileInput && fileInput.files.length > 0) {
            formData.append("file", fileInput.files[0]);
        }

        const response = await fetch(`/api/support/tickets/${activeTicketId}/messages`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.message || "Failed to send reply");
        }

        document.getElementById("modal-reply-form").reset();
        showToast("Success", "Reply added to ticket conversation.", "success");

        // Refresh conversation
        const msgsRes = await fetch(`/api/support/tickets/${activeTicketId}/messages`);
        if (msgsRes.ok) {
            const msgs = await msgsRes.json();
            renderMessages(msgs);
        }

        loadMyTickets();
        if (isStaffRole()) loadAdminQueue();

    } catch (err) {
        showToast("Error", err.message, "danger");
    } finally {
        btn.disabled = false;
    }
}

async function handleUpdateStatus() {
    if (!activeTicketId) return;
    const newStatus = document.getElementById("modal-change-status-select").value;

    try {
        const response = await fetch(`/api/admin/support/tickets/${activeTicketId}/status`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: newStatus, note: "Status updated by support staff" })
        });

        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.message || "Failed to update status");
        }

        const updated = await response.json();
        document.getElementById("modal-ticket-status").className = `badge ${getStatusBadgeClass(updated.status)}`;
        document.getElementById("modal-ticket-status").textContent = updated.status;

        showToast("Success", `Ticket status updated to ${updated.status}`, "success");
        loadMyTickets();
        if (isStaffRole()) loadAdminQueue();

    } catch (err) {
        showToast("Error", err.message, "danger");
    }
}

async function handleAssignAgent() {
    if (!activeTicketId) return;
    const agentIdVal = document.getElementById("modal-assign-agent-id").value;
    const agentId = agentIdVal ? parseInt(agentIdVal) : null;

    try {
        const response = await fetch(`/api/admin/support/tickets/${activeTicketId}/assign`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ assignedToId: agentId, note: "Reassigned staff agent" })
        });

        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.message || "Failed to assign agent");
        }

        const updated = await response.json();
        document.getElementById("modal-ticket-meta").textContent =
            `Submitted by ${updated.createdByName} (${updated.createdByEmail}) on ${formatDate(updated.createdAt)} | Category: ${updated.category} | Assigned: ${updated.assignedToName || 'Unassigned'}`;

        showToast("Success", "Ticket assignment updated.", "success");
        if (isStaffRole()) loadAdminQueue();

    } catch (err) {
        showToast("Error", err.message, "danger");
    }
}

// --- SUBMIT FEEDBACK ---

async function handleSubmitFeedback(e) {
    e.preventDefault();
    const btn = document.getElementById("submit-feedback-btn");
    btn.disabled = true;

    try {
        const ratingVal = parseInt(document.getElementById("feedback-rating-val").value);
        const ticketIdVal = document.getElementById("feedback-ticket-id").value;

        const payload = {
            rating: ratingVal,
            category: document.getElementById("feedback-category").value,
            ticketId: ticketIdVal ? parseInt(ticketIdVal) : null,
            feedbackText: document.getElementById("feedback-text").value
        };

        const response = await fetch('/api/support/feedback', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.message || "Failed to submit feedback");
        }

        showToast("Thank You!", "Your feedback has been submitted successfully.", "success");
        document.getElementById("submit-feedback-form").reset();
        updateStarRatingUI(5);

    } catch (err) {
        showToast("Error", err.message, "danger");
    } finally {
        btn.disabled = false;
    }
}

// --- ADMIN / SUPPORT STAFF QUEUE ---

async function loadAdminQueue() {
    const tbody = document.getElementById("admin-queue-tbody");
    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted p-4"><i class="fas fa-spinner fa-spin me-2"></i>Loading ticket queue...</td></tr>`;

    const search = document.getElementById("admin-filter-search") ? document.getElementById("admin-filter-search").value : null;
    const status = document.getElementById("admin-filter-status") ? document.getElementById("admin-filter-status").value : null;
    const priority = document.getElementById("admin-filter-priority") ? document.getElementById("admin-filter-priority").value : null;
    const category = document.getElementById("admin-filter-category") ? document.getElementById("admin-filter-category").value : null;

    try {
        let url = `/api/admin/support/tickets?size=20`;
        if (search) url += `&search=${encodeURIComponent(search)}`;
        if (status) url += `&status=${encodeURIComponent(status)}`;
        if (priority) url += `&priority=${encodeURIComponent(priority)}`;
        if (category) url += `&category=${encodeURIComponent(category)}`;

        const response = await fetch(url);
        if (!response.ok) throw new Error("Failed to fetch admin ticket queue");

        const page = await response.json();
        const tickets = page.content || [];

        if (tickets.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted p-4">No tickets matching the filter criteria.</td></tr>`;
            return;
        }

        tbody.innerHTML = tickets.map(t => `
            <tr>
                <td><strong class="font-monospace text-primary">${escapeHtml(t.ticketReference)}</strong></td>
                <td><small class="fw-bold">${escapeHtml(t.createdByName || '')}</small><br><small class="text-muted">${escapeHtml(t.createdByEmail || '')}</small></td>
                <td><a href="#" onclick="window.openTicketDetailModal(${t.id}); return false;" class="fw-bold text-decoration-none text-dark">${escapeHtml(t.subject)}</a></td>
                <td><span class="badge bg-light text-dark border">${escapeHtml(t.category)}</span></td>
                <td>${getPriorityBadge(t.priority)}</td>
                <td>${getStatusBadge(t.status)}</td>
                <td><small class="text-muted">${escapeHtml(t.assignedToName || 'Unassigned')}</small></td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="window.openTicketDetailModal(${t.id})"><i class="fas fa-tasks me-1"></i> Manage</button>
                </td>
            </tr>
        `).join("");

    } catch (e) {
        console.error("Error loading admin queue:", e);
        tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger p-4">Failed to load support ticket queue.</td></tr>`;
    }
}

window.applyAdminQueueFilters = function() {
    loadAdminQueue();
};

// --- UTILITY HELPERS ---

function getStatusBadge(status) {
    return `<span class="badge ${getStatusBadgeClass(status)}">${escapeHtml(status)}</span>`;
}

function getStatusBadgeClass(status) {
    switch (status) {
        case 'OPEN': return 'bg-primary';
        case 'IN_PROGRESS': return 'bg-info text-dark';
        case 'WAITING_FOR_USER': return 'bg-warning text-dark';
        case 'RESOLVED': return 'bg-success';
        case 'CLOSED': return 'bg-secondary';
        default: return 'bg-light text-dark border';
    }
}

function getPriorityBadge(priority) {
    return `<span class="badge ${getPriorityBadgeClass(priority)}">${escapeHtml(priority)}</span>`;
}

function getPriorityBadgeClass(priority) {
    switch (priority) {
        case 'URGENT': return 'bg-danger';
        case 'HIGH': return 'bg-warning text-dark';
        case 'MEDIUM': return 'bg-info text-dark';
        case 'LOW': return 'bg-light text-dark border';
        default: return 'bg-secondary';
    }
}

function formatDate(isoStr) {
    if (!isoStr) return "-";
    try {
        const dt = new Date(isoStr);
        return dt.toLocaleString();
    } catch (e) {
        return isoStr;
    }
}

function escapeHtml(str) {
    if (!str) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function showToast(title, message, type = "info") {
    if (window.showAppNotification) {
        window.showAppNotification(title, message, type);
    } else {
        alert(`${title}: ${message}`);
    }
}
