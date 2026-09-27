package com.portshipping.dto;

public class UpdateTicketStatusRequest {
    private String status;
    private String note;

    public UpdateTicketStatusRequest() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
