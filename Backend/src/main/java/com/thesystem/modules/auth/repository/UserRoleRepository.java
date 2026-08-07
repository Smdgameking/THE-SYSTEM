package com.thesystem.modules.auth.repository;

import com.thesystem.modules.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    Optional<UserRole> findByUserIdAndRoleIdAndDeletedAtIsNull(UUID userId, UUID roleId);

    boolean existsByUserIdAndRoleIdAndDeletedAtIsNull(UUID userId, UUID roleId);

    List<UserRole> findByUserIdAndDeletedAtIsNull(UUID userId);
}
