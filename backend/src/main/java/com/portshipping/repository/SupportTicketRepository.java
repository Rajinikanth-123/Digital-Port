package com.portshipping.repository;

import com.portshipping.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long>, JpaSpecificationExecutor<SupportTicket> {

    Optional<SupportTicket> findByTicketReference(String ticketReference);

    Page<SupportTicket> findByCreatedById(Long createdById, Pageable pageable);

    @Query("SELECT t FROM SupportTicket t WHERE " +
           "(:search IS NULL OR LOWER(t.ticketReference) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:status IS NULL OR LOWER(t.status) = LOWER(:status)) AND " +
           "(:priority IS NULL OR LOWER(t.priority) = LOWER(:priority)) AND " +
           "(:category IS NULL OR LOWER(t.category) = LOWER(:category)) AND " +
           "(:assignedToId IS NULL OR t.assignedTo.id = :assignedToId) AND " +
           "(:createdById IS NULL OR t.createdBy.id = :createdById)")
    Page<SupportTicket> filterTickets(@Param("search") String search,
                                      @Param("status") String status,
                                      @Param("priority") String priority,
                                      @Param("category") String category,
                                      @Param("assignedToId") Long assignedToId,
                                      @Param("createdById") Long createdById,
                                      Pageable pageable);
}
