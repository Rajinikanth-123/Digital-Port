package com.portshipping.repository;

import com.portshipping.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByShipmentId(Long shipmentId);
    long countByShipmentId(Long shipmentId);

    List<Document> findByCustomsClearanceId(Long customsClearanceId);
    long countByCustomsClearanceId(Long customsClearanceId);

    List<Document> findByDocumentTypeIgnoreCase(String docType);
    Page<Document> findByDocumentTypeIgnoreCase(String docType, Pageable pageable);

    Page<Document> findByFileNameContainingIgnoreCaseOrDocumentTypeContainingIgnoreCase(String fileName, String docType, Pageable pageable);
}
