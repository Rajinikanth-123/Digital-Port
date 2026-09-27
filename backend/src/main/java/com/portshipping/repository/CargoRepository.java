package com.portshipping.repository;

import com.portshipping.entity.Cargo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CargoRepository extends JpaRepository<Cargo, Long> {

    List<Cargo> findByShipmentId(Long shipmentId);

    long countByShipmentId(Long shipmentId);

    @Query("SELECT c FROM Cargo c WHERE " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.cargoType) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Cargo> searchCargo(@Param("query") String query, Pageable pageable);
}
