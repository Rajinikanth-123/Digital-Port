package com.portshipping.repository;

import com.portshipping.entity.Container;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContainerRepository extends JpaRepository<Container, Long> {

    boolean existsByContainerNumberIgnoreCase(String containerNumber);

    boolean existsByContainerNumberIgnoreCaseAndIdNot(String containerNumber, Long id);

    long countByShipmentId(Long shipmentId);

    @Query("SELECT c FROM Container c WHERE " +
           "LOWER(c.containerNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.size) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.type) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.status) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Container> searchContainers(@Param("query") String query, Pageable pageable);
}
