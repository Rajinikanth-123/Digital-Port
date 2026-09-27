package com.portshipping.repository;

import com.portshipping.entity.Port;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PortRepository extends JpaRepository<Port, Long> {
    Optional<Port> findByCodeIgnoreCase(String code);
    List<Port> findByCountryIgnoreCase(String country);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    Page<Port> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrCountryContainingIgnoreCase(
            String name, String code, String country, Pageable pageable);
}
