package com.portshipping.repository;

import com.portshipping.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByEntityNameIgnoreCase(String entityName);

    List<AuditLog> findByEntityNameIgnoreCaseAndEntityIdOrderByCreatedAtDesc(String entityName, Long entityId);

    List<AuditLog> findByUserId(Long userId);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AuditLog> findTop20ByOrderByCreatedAtDesc();

    long countByOutcomeIgnoreCase(String outcome);

    @Query("SELECT COUNT(DISTINCT a.user.id) FROM AuditLog a WHERE a.user IS NOT NULL")
    long countDistinctActiveUsers();
}
