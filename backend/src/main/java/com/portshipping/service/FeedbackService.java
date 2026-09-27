package com.portshipping.service;

import com.portshipping.dto.FeedbackDto;
import com.portshipping.dto.SubmitFeedbackRequest;
import com.portshipping.entity.AuditLog;
import com.portshipping.entity.Feedback;
import com.portshipping.entity.SupportTicket;
import com.portshipping.entity.User;
import com.portshipping.repository.AuditLogRepository;
import com.portshipping.repository.FeedbackRepository;
import com.portshipping.repository.SupportTicketRepository;
import com.portshipping.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final SupportTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           SupportTicketRepository ticketRepository,
                           UserRepository userRepository,
                           AuditLogRepository auditLogRepository) {
        this.feedbackRepository = feedbackRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public FeedbackDto submitFeedback(SubmitFeedbackRequest req, String username) {
        if (req.getFeedbackText() == null || req.getFeedbackText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback text is required.");
        }

        if (req.getRating() != null && (req.getRating() < 1 || req.getRating() > 5)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5.");
        }

        User user = (username != null && !username.isBlank()) ? userRepository.findByEmailIgnoreCase(username).orElse(null) : null;

        SupportTicket ticket = null;
        if (req.getTicketId() != null) {
            ticket = ticketRepository.findById(req.getTicketId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked ticket #" + req.getTicketId() + " not found."));

            // Check if feedback already submitted for this ticket
            if (feedbackRepository.findByTicketId(ticket.getId()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback has already been submitted for this support ticket.");
            }
        }

        String category = (req.getCategory() != null && !req.getCategory().isBlank()) ? req.getCategory().toUpperCase() : "GENERAL";

        Feedback feedback = new Feedback(user, ticket, req.getRating(), category, req.getFeedbackText().trim());
        Feedback saved = feedbackRepository.save(feedback);

        if (user != null) {
            auditLogRepository.save(new AuditLog(user, "SUBMIT_FEEDBACK", "Feedback", saved.getId(),
                    "{\"rating\":" + saved.getRating() + ",\"category\":\"" + saved.getCategory() + "\"}"));
        }

        return FeedbackDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<FeedbackDto> filterFeedbackAdmin(String category, String reviewState, Integer rating, Pageable pageable) {
        Page<Feedback> page = feedbackRepository.filterFeedback(
                (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) ? category.trim() : null,
                (reviewState != null && !reviewState.isBlank() && !"ALL".equalsIgnoreCase(reviewState)) ? reviewState.trim() : null,
                rating,
                pageable
        );

        List<FeedbackDto> dtos = page.getContent().stream().map(FeedbackDto::fromEntity).toList();
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public FeedbackDto updateReviewState(Long feedbackId, String newReviewState, String adminUsername) {
        if (newReviewState == null || newReviewState.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review state is required.");
        }

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback #" + feedbackId + " not found."));

        feedback.setReviewState(newReviewState.trim().toUpperCase());
        Feedback updated = feedbackRepository.save(feedback);

        User admin = userRepository.findByEmailIgnoreCase(adminUsername).orElse(null);
        if (admin != null) {
            auditLogRepository.save(new AuditLog(admin, "REVIEW_FEEDBACK", "Feedback", updated.getId(),
                    "{\"newReviewState\":\"" + updated.getReviewState() + "\"}"));
        }

        return FeedbackDto.fromEntity(updated);
    }
}
