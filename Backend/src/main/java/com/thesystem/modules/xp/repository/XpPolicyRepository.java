package com.thesystem.modules.xp.repository;

import com.thesystem.modules.xp.entity.XpPolicy;
import com.thesystem.modules.xp.enums.PolicyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface XpPolicyRepository extends JpaRepository<XpPolicy, UUID> {

    Optional<XpPolicy> findByCodeAndDeletedAtIsNull(String code);

    List<XpPolicy> findByPolicyTypeAndIsActiveAndDeletedAtIsNull(PolicyType policyType, Boolean isActive);

    List<XpPolicy> findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(Boolean isActive);

    List<XpPolicy> findByDeletedAtIsNullOrderByPriorityDesc();
}
