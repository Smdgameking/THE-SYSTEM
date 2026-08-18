package com.thesystem.modules.notification.repository;

import com.thesystem.modules.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

    List<Notification> findByUserIdAndReadAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId, boolean read);

    Optional<Notification> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    boolean existsByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    long countByUserIdAndReadAndDeletedAtIsNull(UUID userId, boolean read);
}
