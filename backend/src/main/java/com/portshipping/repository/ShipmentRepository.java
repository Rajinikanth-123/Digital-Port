package com.portshipping.repository;

import com.portshipping.entity.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    long countByStatusIgnoreCase(String status);

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    Optional<Shipment> findByReferenceNumber(String referenceNumber);

    boolean existsByTrackingNumberIgnoreCase(String trackingNumber);

    boolean existsByReferenceNumberIgnoreCase(String referenceNumber);

    boolean existsByReferenceNumberIgnoreCaseAndIdNot(String referenceNumber, Long id);

    List<Shipment> findByVesselId(Long vesselId);

    long countByVesselId(Long vesselId);

    @Query("SELECT s FROM Shipment s WHERE " +
           "(:status IS NULL OR UPPER(s.status) = UPPER(:status)) AND (" +
           ":query IS NULL OR :query = '' OR " +
           "LOWER(s.trackingNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.referenceNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.origin) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.destination) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Shipment> searchShipments(@Param("query") String query, @Param("status") String status, Pageable pageable);
}
