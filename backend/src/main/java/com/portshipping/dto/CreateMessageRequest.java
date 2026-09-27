package com.portshipping.dto;

public class CreateMessageRequest {
    private String messageBody;
    private boolean internalNote = false;

    public CreateMessageRequest() {}

    public String getMessageBody() { return messageBody; }
    public void setMessageBody(String messageBody) { this.messageBody = messageBody; }

    public boolean isInternalNote() { return internalNote; }
    public void setInternalNote(boolean internalNote) { this.internalNote = internalNote; }
}
