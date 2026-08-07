package com.thesystem.modules.task.repository;

import com.thesystem.modules.task.entity.RecurringTaskConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringTaskConfigRepository extends JpaRepository<RecurringTaskConfig, UUID> {

    Optional<RecurringTaskConfig> findByTaskId(UUID taskId);

    List<RecurringTaskConfig> findByIsActive(Boolean isActive);

    boolean existsByTaskId(UUID taskId);
}
