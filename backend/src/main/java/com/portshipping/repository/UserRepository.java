package com.portshipping.repository;

import com.portshipping.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByEnabledTrue();
    long countByRoleIgnoreCaseAndEnabledTrue(String role);
    List<User> findByRoleIgnoreCase(String role);
    List<User> findByRoleIgnoreCaseIn(List<String> roles);
    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);
}
