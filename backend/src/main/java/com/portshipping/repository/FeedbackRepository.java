package com.portshipping.repository;

import com.portshipping.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByTicketId(Long ticketId);

    Page<Feedback> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT f FROM Feedback f WHERE " +
           "(:category IS NULL OR LOWER(f.category) = LOWER(:category)) AND " +
           "(:reviewState IS NULL OR LOWER(f.reviewState) = LOWER(:reviewState)) AND " +
           "(:rating IS NULL OR f.rating = :rating)")
    Page<Feedback> filterFeedback(@Param("category") String category,
                                  @Param("reviewState") String reviewState,
                                  @Param("rating") Integer rating,
                                  Pageable pageable);
}
