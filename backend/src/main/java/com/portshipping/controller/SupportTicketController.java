package com.portshipping.controller;

import com.portshipping.dto.*;
import com.portshipping.service.SupportTicketService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class SupportTicketController {

    private final SupportTicketService ticketService;

    public SupportTicketController(SupportTicketService ticketService) {
        this.ticketService = ticketService;
    }

    // --- USER ENDPOINTS ---

    @PostMapping(value = "/api/support/tickets", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<SupportTicketDto> createTicket(
            @RequestPart(value = "data", required = false) CreateTicketRequest jsonReq,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Authentication auth) {

        CreateTicketRequest req = jsonReq;
        if (req == null) {
            req = new CreateTicketRequest();
            req.setSubject(subject);
            req.setDescription(description);
            req.setCategory(category);
            req.setPriority(priority);
        }

        SupportTicketDto dto = ticketService.createTicket(req, file, auth.getName());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/api/support/tickets/my")
    public ResponseEntity<Page<SupportTicketDto>> getMyTickets(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication auth) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ticketService.getMyTickets(auth.getName(), pageable));
    }

    @GetMapping("/api/support/tickets/{id}")
    public ResponseEntity<SupportTicketDto> getTicketById(@PathVariable("id") Long id, Authentication auth) {
        return ResponseEntity.ok(ticketService.getTicketById(id, auth.getName()));
    }

    @PostMapping(value = "/api/support/tickets/{id}/messages", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<TicketMessageDto> addMessageToTicket(
            @PathVariable("id") Long ticketId,
            @RequestPart(value = "data", required = false) CreateMessageRequest jsonReq,
            @RequestParam(value = "messageBody", required = false) String body,
            @RequestParam(value = "internalNote", defaultValue = "false") boolean internalNote,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Authentication auth) {

        CreateMessageRequest req = jsonReq;
        if (req == null) {
            req = new CreateMessageRequest();
            req.setMessageBody(body);
            req.setInternalNote(internalNote);
        }

        TicketMessageDto dto = ticketService.addMessageToTicket(ticketId, req, file, auth.getName());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/api/support/tickets/{id}/messages")
    public ResponseEntity<List<TicketMessageDto>> getTicketMessages(@PathVariable("id") Long ticketId, Authentication auth) {
        return ResponseEntity.ok(ticketService.getTicketMessages(ticketId, auth.getName()));
    }

    @GetMapping("/api/support/tickets/{id}/attachments/{messageId}")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable("id") Long ticketId,
            @PathVariable("messageId") Long messageId,
            Authentication auth) {

        Resource resource = ticketService.loadAttachmentResource(ticketId, messageId, auth.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    // --- ADMIN / SUPPORT STAFF ENDPOINTS ---

    @GetMapping("/api/admin/support/tickets")
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'SHIPPING_AGENT')")
    public ResponseEntity<Page<SupportTicketDto>> filterTicketsAdmin(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "assignedToId", required = false) Long assignedToId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        return ResponseEntity.ok(ticketService.filterTicketsAdmin(search, status, priority, category, assignedToId, pageable));
    }

    @PatchMapping("/api/admin/support/tickets/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'SHIPPING_AGENT')")
    public ResponseEntity<SupportTicketDto> assignTicket(
            @PathVariable("id") Long ticketId,
            @RequestBody AssignTicketRequest req,
            Authentication auth) {
        return ResponseEntity.ok(ticketService.assignTicket(ticketId, req, auth.getName()));
    }

    @PatchMapping("/api/admin/support/tickets/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'SHIPPING_AGENT')")
    public ResponseEntity<SupportTicketDto> updateTicketStatus(
            @PathVariable("id") Long ticketId,
            @RequestBody UpdateTicketStatusRequest req,
            Authentication auth) {
        return ResponseEntity.ok(ticketService.updateTicketStatus(ticketId, req, auth.getName()));
    }
}
