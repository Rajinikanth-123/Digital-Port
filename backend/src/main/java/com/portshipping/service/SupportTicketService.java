package com.portshipping.service;

import com.portshipping.dto.*;
import com.portshipping.entity.*;
import com.portshipping.repository.*;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional
public class SupportTicketService {

    private static final AtomicLong REF_COUNTER = new AtomicLong(System.currentTimeMillis() % 10000);

    public static final Set<String> VALID_STATUSES = Set.of("OPEN", "IN_PROGRESS", "WAITING_FOR_USER", "RESOLVED", "CLOSED");
    public static final Set<String> VALID_PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");
    public static final Set<String> VALID_CATEGORIES = Set.of("GENERAL", "SHIPMENT", "VESSEL", "CUSTOMS", "BILLING", "TECHNICAL");

    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final TicketStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;
    private final DocumentStorageService storageService;
    private final NotificationService notificationService;
    private final AuditLogRepository auditLogRepository;

    public SupportTicketService(SupportTicketRepository ticketRepository,
                                TicketMessageRepository messageRepository,
                                TicketStatusHistoryRepository statusHistoryRepository,
                                UserRepository userRepository,
                                DocumentStorageService storageService,
                                NotificationService notificationService,
                                AuditLogRepository auditLogRepository) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.notificationService = notificationService;
        this.auditLogRepository = auditLogRepository;
    }

    public SupportTicketDto createTicket(CreateTicketRequest req, MultipartFile attachment, String username) {
        if (req.getSubject() == null || req.getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject is required.");
        }
        if (req.getDescription() == null || req.getDescription().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description is required.");
        }

        User creator = findUserByEmail(username);
        String ref = generateTicketReference();

        String category = (req.getCategory() != null && VALID_CATEGORIES.contains(req.getCategory().toUpperCase()))
                ? req.getCategory().toUpperCase() : "GENERAL";
        String priority = (req.getPriority() != null && VALID_PRIORITIES.contains(req.getPriority().toUpperCase()))
                ? req.getPriority().toUpperCase() : "MEDIUM";

        SupportTicket ticket = new SupportTicket(ref, creator, req.getSubject().trim(), req.getDescription().trim(), category, priority);
        SupportTicket savedTicket = ticketRepository.save(ticket);

        // Initial ticket message
        TicketMessage initialMsg = new TicketMessage(savedTicket, creator, req.getDescription().trim(), false);

        if (attachment != null && !attachment.isEmpty()) {
            DocumentStorageService.StoredFileResult result = storageService.storeFile(attachment);
            initialMsg.setAttachmentFilename(result.originalFileName);
            initialMsg.setAttachmentStorageKey(result.storageKey);
            initialMsg.setAttachmentMimeType(result.mimeType);
            initialMsg.setAttachmentSizeBytes(result.fileSizeBytes);
        }

        messageRepository.save(initialMsg);

        // Record initial status history
        statusHistoryRepository.save(new TicketStatusHistory(savedTicket, creator, null, "OPEN", "Ticket submitted"));

        // Audit Log
        auditLogRepository.save(new AuditLog(creator, "CREATE_SUPPORT_TICKET", "SupportTicket", savedTicket.getId(),
                "{\"ticketReference\":\"" + savedTicket.getTicketReference() + "\",\"subject\":\"" + sanitizeJson(savedTicket.getSubject()) + "\"}"));

        return SupportTicketDto.fromEntity(savedTicket, 1);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicketDto> getMyTickets(String username, Pageable pageable) {
        User user = findUserByEmail(username);
        Page<SupportTicket> page = ticketRepository.findByCreatedById(user.getId(), pageable);
        List<SupportTicketDto> dtos = page.getContent().stream()
                .map(t -> SupportTicketDto.fromEntity(t, messageRepository.findByTicketIdAndIsInternalNoteFalseOrderByCreatedAtAsc(t.getId()).size()))
                .toList();
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public SupportTicketDto getTicketById(Long id, String username) {
        SupportTicket ticket = findTicketOrThrow(id);
        User user = findUserByEmail(username);

        // Security / Ownership Check
        if (!isStaff(user) && !ticket.getCreatedBy().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only view your own support tickets.");
        }

        int count = isStaff(user)
                ? messageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()).size()
                : messageRepository.findByTicketIdAndIsInternalNoteFalseOrderByCreatedAtAsc(ticket.getId()).size();

        return SupportTicketDto.fromEntity(ticket, count);
    }

    public TicketMessageDto addMessageToTicket(Long ticketId, CreateMessageRequest req, MultipartFile attachment, String username) {
        if (req.getMessageBody() == null || req.getMessageBody().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message body cannot be empty.");
        }

        SupportTicket ticket = findTicketOrThrow(ticketId);
        User sender = findUserByEmail(username);

        // Security / Ownership Check
        if (!isStaff(sender) && !ticket.getCreatedBy().getId().equals(sender.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only reply to your own support tickets.");
        }

        boolean isInternal = req.isInternalNote();
        if (isInternal && !isStaff(sender)) {
            isInternal = false; // Non-staff cannot post internal notes
        }

        TicketMessage msg = new TicketMessage(ticket, sender, req.getMessageBody().trim(), isInternal);

        if (attachment != null && !attachment.isEmpty()) {
            DocumentStorageService.StoredFileResult result = storageService.storeFile(attachment);
            msg.setAttachmentFilename(result.originalFileName);
            msg.setAttachmentStorageKey(result.storageKey);
            msg.setAttachmentMimeType(result.mimeType);
            msg.setAttachmentSizeBytes(result.fileSizeBytes);
        }

        TicketMessage savedMsg = messageRepository.save(msg);

        // Update ticket timestamp
        ticket.setUpdatedAt(Instant.now());

        // If ticket was WAITING_FOR_USER and user replies, shift status to IN_PROGRESS
        if (!isStaff(sender) && "WAITING_FOR_USER".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus("IN_PROGRESS");
            statusHistoryRepository.save(new TicketStatusHistory(ticket, sender, "WAITING_FOR_USER", "IN_PROGRESS", "User replied to ticket"));
        } else if (isStaff(sender) && !isInternal && "OPEN".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus("IN_PROGRESS");
            statusHistoryRepository.save(new TicketStatusHistory(ticket, sender, "OPEN", "IN_PROGRESS", "Support staff replied"));
        }

        ticketRepository.save(ticket);

        // Dispatch notifications
        if (isStaff(sender) && !isInternal) {
            notificationService.sendNotification(ticket.getCreatedBy(),
                    "New Support Ticket Reply",
                    "Support agent replied to ticket #" + ticket.getTicketReference() + ": " + ticket.getSubject(),
                    "SYSTEM", "SupportTicket", ticket.getId());
        } else if (!isStaff(sender) && ticket.getAssignedTo() != null) {
            notificationService.sendNotification(ticket.getAssignedTo(),
                    "Ticket Reply Received",
                    "User replied to assigned ticket #" + ticket.getTicketReference(),
                    "SYSTEM", "SupportTicket", ticket.getId());
        }

        // Audit Log
        auditLogRepository.save(new AuditLog(sender, "ADD_TICKET_MESSAGE", "SupportTicket", ticket.getId(),
                "{\"ticketReference\":\"" + ticket.getTicketReference() + "\",\"isInternalNote\":" + isInternal + "}"));

        return TicketMessageDto.fromEntity(savedMsg);
    }

    @Transactional(readOnly = true)
    public List<TicketMessageDto> getTicketMessages(Long ticketId, String username) {
        SupportTicket ticket = findTicketOrThrow(ticketId);
        User user = findUserByEmail(username);

        if (!isStaff(user) && !ticket.getCreatedBy().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only view your own ticket messages.");
        }

        List<TicketMessage> msgs = isStaff(user)
                ? messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                : messageRepository.findByTicketIdAndIsInternalNoteFalseOrderByCreatedAtAsc(ticketId);

        return msgs.stream().map(TicketMessageDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Resource loadAttachmentResource(Long ticketId, Long messageId, String username) {
        SupportTicket ticket = findTicketOrThrow(ticketId);
        User user = findUserByEmail(username);

        if (!isStaff(user) && !ticket.getCreatedBy().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }

        TicketMessage msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found."));

        if (!msg.getTicket().getId().equals(ticketId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message does not belong to specified ticket.");
        }

        if (msg.isInternalNote() && !isStaff(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }

        if (msg.getAttachmentStorageKey() == null || msg.getAttachmentStorageKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No attachment linked to this message.");
        }

        return storageService.loadFileAsResource(msg.getAttachmentStorageKey());
    }

    // --- ADMIN / SUPPORT STAFF AGENT METHODS ---

    @Transactional(readOnly = true)
    public Page<SupportTicketDto> filterTicketsAdmin(String search, String status, String priority, String category, Long assignedToId, Pageable pageable) {
        Page<SupportTicket> page = ticketRepository.filterTickets(
                (search != null && !search.isBlank()) ? search.trim() : null,
                (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) ? status.trim() : null,
                (priority != null && !priority.isBlank() && !"ALL".equalsIgnoreCase(priority)) ? priority.trim() : null,
                (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) ? category.trim() : null,
                assignedToId,
                null,
                pageable
        );

        List<SupportTicketDto> dtos = page.getContent().stream()
                .map(t -> SupportTicketDto.fromEntity(t, messageRepository.findByTicketIdOrderByCreatedAtAsc(t.getId()).size()))
                .toList();

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public SupportTicketDto assignTicket(Long ticketId, AssignTicketRequest req, String adminUsername) {
        SupportTicket ticket = findTicketOrThrow(ticketId);
        User admin = findUserByEmail(adminUsername);

        User assignee = null;
        if (req.getAssignedToId() != null) {
            assignee = userRepository.findById(req.getAssignedToId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assigned user #" + req.getAssignedToId() + " not found."));
        }

        ticket.setAssignedTo(assignee);
        ticket.setUpdatedAt(Instant.now());

        if (assignee != null && "OPEN".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus("IN_PROGRESS");
        }

        SupportTicket updated = ticketRepository.save(ticket);

        String note = (req.getNote() != null && !req.getNote().isBlank())
                ? req.getNote().trim() : (assignee != null ? "Assigned to " + assignee.getName() : "Unassigned ticket");

        statusHistoryRepository.save(new TicketStatusHistory(updated, admin, ticket.getStatus(), updated.getStatus(), note));

        if (assignee != null) {
            notificationService.sendNotification(assignee,
                    "Ticket Assigned to You",
                    "Support ticket #" + updated.getTicketReference() + " has been assigned to you.",
                    "SYSTEM", "SupportTicket", updated.getId());
        }

        auditLogRepository.save(new AuditLog(admin, "ASSIGN_SUPPORT_TICKET", "SupportTicket", updated.getId(),
                "{\"ticketReference\":\"" + updated.getTicketReference() + "\",\"assignedToId\":" + (assignee != null ? assignee.getId() : "null") + "}"));

        return SupportTicketDto.fromEntity(updated, messageRepository.findByTicketIdOrderByCreatedAtAsc(updated.getId()).size());
    }

    public SupportTicketDto updateTicketStatus(Long ticketId, UpdateTicketStatusRequest req, String adminUsername) {
        if (req.getStatus() == null || req.getStatus().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required.");
        }

        String newStatus = req.getStatus().trim().toUpperCase();
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ticket status: " + newStatus + ". Valid statuses: " + VALID_STATUSES);
        }

        SupportTicket ticket = findTicketOrThrow(ticketId);
        User actor = findUserByEmail(adminUsername);

        String oldStatus = ticket.getStatus();
        if (oldStatus.equals(newStatus)) {
            return SupportTicketDto.fromEntity(ticket, messageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()).size());
        }

        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(Instant.now());

        if ("RESOLVED".equals(newStatus)) {
            ticket.setResolvedAt(Instant.now());
        } else if ("CLOSED".equals(newStatus)) {
            ticket.setClosedAt(Instant.now());
        }

        SupportTicket updated = ticketRepository.save(ticket);

        String note = (req.getNote() != null && !req.getNote().isBlank()) ? req.getNote().trim() : "Status updated to " + newStatus;
        statusHistoryRepository.save(new TicketStatusHistory(updated, actor, oldStatus, newStatus, note));

        // Notify creator
        notificationService.sendNotification(updated.getCreatedBy(),
                "Support Ticket Status Update",
                "Your ticket #" + updated.getTicketReference() + " status changed to " + newStatus,
                "SYSTEM", "SupportTicket", updated.getId());

        auditLogRepository.save(new AuditLog(actor, "UPDATE_TICKET_STATUS", "SupportTicket", updated.getId(),
                "{\"ticketReference\":\"" + updated.getTicketReference() + "\",\"oldStatus\":\"" + oldStatus + "\",\"newStatus\":\"" + newStatus + "\"}"));

        return SupportTicketDto.fromEntity(updated, messageRepository.findByTicketIdOrderByCreatedAtAsc(updated.getId()).size());
    }

    private SupportTicket findTicketOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support Ticket #" + id + " not found."));
    }

    private User findUserByEmail(String username) {
        if (username == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        return userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private boolean isStaff(User user) {
        if (user == null || user.getRole() == null) return false;
        String role = user.getRole().toUpperCase().replace("ROLE_", "");
        return "ADMIN".equals(role) || "PORT_MANAGER".equals(role) || "SHIPPING_AGENT".equals(role);
    }

    private synchronized String generateTicketReference() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = REF_COUNTER.incrementAndGet() % 10000;
        return String.format("TICK-%s-%04d", dateStr, seq);
    }

    private String sanitizeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
