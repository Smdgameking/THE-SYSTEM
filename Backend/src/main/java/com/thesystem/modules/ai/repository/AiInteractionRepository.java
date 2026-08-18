package com.thesystem.modules.ai.repository;

import com.thesystem.modules.ai.entity.AiInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiInteractionRepository extends JpaRepository<AiInteraction, UUID> {

    List<AiInteraction> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

    Optional<AiInteraction> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
}
