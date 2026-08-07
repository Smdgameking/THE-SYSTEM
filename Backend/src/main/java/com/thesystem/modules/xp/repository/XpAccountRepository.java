package com.thesystem.modules.xp.repository;

import com.thesystem.modules.xp.entity.XpAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface XpAccountRepository extends JpaRepository<XpAccount, UUID> {

    Optional<XpAccount> findByUserIdAndDeletedAtIsNull(UUID userId);

    boolean existsByUserIdAndDeletedAtIsNull(UUID userId);

    List<XpAccount> findByCurrentLevelGreaterThanAndDeletedAtIsNull(int level);

    List<XpAccount> findByDeletedAtIsNullOrderByLifetimeXpDesc();
}
