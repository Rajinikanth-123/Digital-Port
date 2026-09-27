package com.portshipping.repository;

import com.portshipping.entity.CustomsClearance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CustomsClearanceRepository extends JpaRepository<CustomsClearance, Long> {
    Optional<CustomsClearance> findByDeclarationNumber(String declarationNumber);
    Optional<CustomsClearance> findByDeclarationNumberIgnoreCase(String declarationNumber);
    boolean existsByDeclarationNumberIgnoreCase(String declarationNumber);
    boolean existsByDeclarationNumberIgnoreCaseAndIdNot(String declarationNumber, Long id);

    List<CustomsClearance> findByShipmentId(Long shipmentId);
    long countByShipmentId(Long shipmentId);

    List<CustomsClearance> findByStatusIgnoreCase(String status);
    Page<CustomsClearance> findByStatusIgnoreCase(String status, Pageable pageable);
    long countByStatusIgnoreCase(String status);

    Page<CustomsClearance> findByDeclarationNumberContainingIgnoreCaseOrStatusContainingIgnoreCase(String decl, String status, Pageable pageable);
}
