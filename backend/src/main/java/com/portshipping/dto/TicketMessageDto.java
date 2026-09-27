package com.portshipping.dto;

import com.portshipping.entity.TicketMessage;
import java.time.Instant;

public class TicketMessageDto {
    private Long id;
    private Long ticketId;
    private Long senderId;
    private String senderName;
    private String senderEmail;
    private String senderRole;
    private String messageBody;
    private boolean isInternalNote;
    private String attachmentFilename;
    private String attachmentMimeType;
    private Long attachmentSizeBytes;
    private boolean hasAttachment;
    private Instant createdAt;

    public TicketMessageDto() {}

    public static TicketMessageDto fromEntity(TicketMessage msg) {
        TicketMessageDto dto = new TicketMessageDto();
        dto.setId(msg.getId());
        if (msg.getTicket() != null) {
            dto.setTicketId(msg.getTicket().getId());
        }
        if (msg.getSender() != null) {
            dto.setSenderId(msg.getSender().getId());
            dto.setSenderName(msg.getSender().getName());
            dto.setSenderEmail(msg.getSender().getEmail());
            dto.setSenderRole(msg.getSender().getRole());
        }
        dto.setMessageBody(msg.getMessageBody());
        dto.setInternalNote(msg.isInternalNote());
        dto.setAttachmentFilename(msg.getAttachmentFilename());
        dto.setAttachmentMimeType(msg.getAttachmentMimeType());
        dto.setAttachmentSizeBytes(msg.getAttachmentSizeBytes());
        dto.setHasAttachment(msg.getAttachmentStorageKey() != null && !msg.getAttachmentStorageKey().isBlank());
        dto.setCreatedAt(msg.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getMessageBody() { return messageBody; }
    public void setMessageBody(String messageBody) { this.messageBody = messageBody; }

    public boolean isInternalNote() { return isInternalNote; }
    public void setInternalNote(boolean internalNote) { isInternalNote = internalNote; }

    public String getAttachmentFilename() { return attachmentFilename; }
    public void setAttachmentFilename(String attachmentFilename) { this.attachmentFilename = attachmentFilename; }

    public String getAttachmentMimeType() { return attachmentMimeType; }
    public void setAttachmentMimeType(String attachmentMimeType) { this.attachmentMimeType = attachmentMimeType; }

    public Long getAttachmentSizeBytes() { return attachmentSizeBytes; }
    public void setAttachmentSizeBytes(Long attachmentSizeBytes) { this.attachmentSizeBytes = attachmentSizeBytes; }

    public boolean isHasAttachment() { return hasAttachment; }
    public void setHasAttachment(boolean hasAttachment) { this.hasAttachment = hasAttachment; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
