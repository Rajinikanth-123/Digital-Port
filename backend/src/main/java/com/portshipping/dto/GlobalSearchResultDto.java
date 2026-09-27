package com.portshipping.dto;

import java.time.Instant;
import java.util.Map;

public class GlobalSearchResultDto {
    private Long recordId;
    private String module;
    private String displayTitle;
    private String referenceNumber;
    private String status;
    private Instant eventDate;
    private String summary;
    private String detailUrl;
    private Map<String, Object> additionalInfo;

    public GlobalSearchResultDto() {}

    public GlobalSearchResultDto(Long recordId, String module, String displayTitle, String referenceNumber,
                                 String status, Instant eventDate, String summary, String detailUrl,
                                 Map<String, Object> additionalInfo) {
        this.recordId = recordId;
        this.module = module;
        this.displayTitle = displayTitle;
        this.referenceNumber = referenceNumber;
        this.status = status;
        this.eventDate = eventDate;
        this.summary = summary;
        this.detailUrl = detailUrl;
        this.additionalInfo = additionalInfo;
    }

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getDisplayTitle() { return displayTitle; }
    public void setDisplayTitle(String displayTitle) { this.displayTitle = displayTitle; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getEventDate() { return eventDate; }
    public void setEventDate(Instant eventDate) { this.eventDate = eventDate; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDetailUrl() { return detailUrl; }
    public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }

    public Map<String, Object> getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(Map<String, Object> additionalInfo) { this.additionalInfo = additionalInfo; }
}
