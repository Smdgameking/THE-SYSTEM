package com.thesystem.modules.settings.repository;

import com.thesystem.modules.settings.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettingRepository extends JpaRepository<Setting, UUID> {

    Optional<Setting> findByUserIdAndNamespaceAndKeyAndDeletedAtIsNull(UUID userId, String namespace, String key);

    Optional<Setting> findByUserIdIsNullAndNamespaceAndKeyAndDeletedAtIsNull(String namespace, String key);

    List<Setting> findByUserIdAndNamespaceAndDeletedAtIsNull(UUID userId, String namespace);

    List<Setting> findByUserIdIsNullAndNamespaceAndDeletedAtIsNull(String namespace);

    List<Setting> findByUserIdAndDeletedAtIsNull(UUID userId);

    boolean existsByUserIdAndNamespaceAndKeyAndDeletedAtIsNull(UUID userId, String namespace, String key);

    boolean existsByUserIdIsNullAndNamespaceAndKeyAndDeletedAtIsNull(String namespace, String key);
}
