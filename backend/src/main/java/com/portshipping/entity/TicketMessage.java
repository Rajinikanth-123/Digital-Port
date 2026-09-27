package com.portshipping.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ticket_messages")
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT")
    private String messageBody;

    @Column(name = "is_internal_note", nullable = false)
    private boolean isInternalNote = false;

    @Column(name = "attachment_filename", length = 255)
    private String attachmentFilename;

    @Column(name = "attachment_storage_key", length = 500)
    private String attachmentStorageKey;

    @Column(name = "attachment_mime_type", length = 120)
    private String attachmentMimeType;

    @Column(name = "attachment_size_bytes")
    private Long attachmentSizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TicketMessage() {}

    public TicketMessage(SupportTicket ticket, User sender, String messageBody, boolean isInternalNote) {
        this.ticket = ticket;
        this.sender = sender;
        this.messageBody = messageBody;
        this.isInternalNote = isInternalNote;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SupportTicket getTicket() { return ticket; }
    public void setTicket(SupportTicket ticket) { this.ticket = ticket; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public String getMessageBody() { return messageBody; }
    public void setMessageBody(String messageBody) { this.messageBody = messageBody; }

    public boolean isInternalNote() { return isInternalNote; }
    public void setInternalNote(boolean internalNote) { isInternalNote = internalNote; }

    public String getAttachmentFilename() { return attachmentFilename; }
    public void setAttachmentFilename(String attachmentFilename) { this.attachmentFilename = attachmentFilename; }

    public String getAttachmentStorageKey() { return attachmentStorageKey; }
    public void setAttachmentStorageKey(String attachmentStorageKey) { this.attachmentStorageKey = attachmentStorageKey; }

    public String getAttachmentMimeType() { return attachmentMimeType; }
    public void setAttachmentMimeType(String attachmentMimeType) { this.attachmentMimeType = attachmentMimeType; }

    public Long getAttachmentSizeBytes() { return attachmentSizeBytes; }
    public void setAttachmentSizeBytes(Long attachmentSizeBytes) { this.attachmentSizeBytes = attachmentSizeBytes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
