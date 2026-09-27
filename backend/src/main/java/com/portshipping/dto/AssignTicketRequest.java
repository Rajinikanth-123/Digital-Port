package com.portshipping.dto;

public class AssignTicketRequest {
    private Long assignedToId;
    private String note;

    public AssignTicketRequest() {}

    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
