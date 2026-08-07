package com.thesystem.modules.user.repository;

import com.thesystem.modules.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<UserProfile> findByUsernameAndDeletedAtIsNull(String username);

    boolean existsByUsernameAndDeletedAtIsNull(String username);

    boolean existsByUserIdAndDeletedAtIsNull(UUID userId);
}
