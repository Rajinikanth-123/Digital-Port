package com.portshipping.repository;

import com.portshipping.entity.UserProfilePreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfilePreferenceRepository extends JpaRepository<UserProfilePreference, Long> {
    Optional<UserProfilePreference> findByUserId(Long userId);
    Optional<UserProfilePreference> findByUserEmailIgnoreCase(String email);
}
