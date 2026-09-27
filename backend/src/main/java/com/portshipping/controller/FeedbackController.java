package com.portshipping.controller;

import com.portshipping.dto.FeedbackDto;
import com.portshipping.dto.SubmitFeedbackRequest;
import com.portshipping.service.FeedbackService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/api/support/feedback")
    public ResponseEntity<FeedbackDto> submitFeedback(@RequestBody SubmitFeedbackRequest req, Authentication auth) {
        String username = (auth != null) ? auth.getName() : null;
        return ResponseEntity.ok(feedbackService.submitFeedback(req, username));
    }

    @GetMapping("/api/admin/support/feedback")
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'SHIPPING_AGENT')")
    public ResponseEntity<Page<FeedbackDto>> filterFeedbackAdmin(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "reviewState", required = false) String reviewState,
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(feedbackService.filterFeedbackAdmin(category, reviewState, rating, pageable));
    }

    @PatchMapping("/api/admin/support/feedback/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'SHIPPING_AGENT')")
    public ResponseEntity<FeedbackDto> updateReviewState(
            @PathVariable("id") Long id,
            @RequestParam("reviewState") String reviewState,
            Authentication auth) {
        return ResponseEntity.ok(feedbackService.updateReviewState(id, reviewState, auth.getName()));
    }
}
