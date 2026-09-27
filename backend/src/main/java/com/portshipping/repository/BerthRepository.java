package com.portshipping.repository;

import com.portshipping.entity.Berth;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BerthRepository extends JpaRepository<Berth, Long> {
    List<Berth> findByTerminalId(Long terminalId);
    List<Berth> findByStatusIgnoreCase(String status);
    long countByTerminalId(Long terminalId);

    boolean existsByTerminalIdAndNameIgnoreCase(Long terminalId, String name);
    boolean existsByTerminalIdAndNameIgnoreCaseAndIdNot(Long terminalId, String name, Long id);

    Page<Berth> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
