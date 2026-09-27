package com.portshipping.repository;

import com.portshipping.entity.Terminal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TerminalRepository extends JpaRepository<Terminal, Long> {
    Optional<Terminal> findByCodeIgnoreCase(String code);
    List<Terminal> findByPortId(Long portId);
    long countByPortId(Long portId);

    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    Page<Terminal> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code, Pageable pageable);
}
