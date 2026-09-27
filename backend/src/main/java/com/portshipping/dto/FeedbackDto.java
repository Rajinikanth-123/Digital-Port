package com.portshipping.dto;

import com.portshipping.entity.Feedback;
import java.time.Instant;

public class FeedbackDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Long ticketId;
    private String ticketReference;
    private Integer rating;
    private String category;
    private String feedbackText;
    private String reviewState;
    private Instant createdAt;

    public FeedbackDto() {}

    public static FeedbackDto fromEntity(Feedback feedback) {
        FeedbackDto dto = new FeedbackDto();
        dto.setId(feedback.getId());
        if (feedback.getUser() != null) {
            dto.setUserId(feedback.getUser().getId());
            dto.setUserName(feedback.getUser().getName());
            dto.setUserEmail(feedback.getUser().getEmail());
        }
        if (feedback.getTicket() != null) {
            dto.setTicketId(feedback.getTicket().getId());
            dto.setTicketReference(feedback.getTicket().getTicketReference());
        }
        dto.setRating(feedback.getRating());
        dto.setCategory(feedback.getCategory());
        dto.setFeedbackText(feedback.getFeedbackText());
        dto.setReviewState(feedback.getReviewState());
        dto.setCreatedAt(feedback.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public String getTicketReference() { return ticketReference; }
    public void setTicketReference(String ticketReference) { this.ticketReference = ticketReference; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getFeedbackText() { return feedbackText; }
    public void setFeedbackText(String feedbackText) { this.feedbackText = feedbackText; }

    public String getReviewState() { return reviewState; }
    public void setReviewState(String reviewState) { this.reviewState = reviewState; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
