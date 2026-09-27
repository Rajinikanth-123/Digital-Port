package com.portshipping.repository;

import com.portshipping.entity.Vessel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VesselRepository extends JpaRepository<Vessel, Long> {
    Optional<Vessel> findByImoNumberIgnoreCase(String imoNumber);
    boolean existsByImoNumberIgnoreCase(String imoNumber);
    boolean existsByImoNumberIgnoreCaseAndIdNot(String imoNumber, Long id);

    Page<Vessel> findByNameContainingIgnoreCaseOrImoNumberContainingIgnoreCaseOrVesselTypeContainingIgnoreCase(
            String name, String imoNumber, String vesselType, Pageable pageable);
}
