package com.thesystem.modules.task.repository;

import com.thesystem.modules.task.entity.TaskTimeEntry;
import com.thesystem.modules.task.enums.TaskTimeEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TaskTimeEntryRepository extends JpaRepository<TaskTimeEntry, UUID> {

    List<TaskTimeEntry> findByTaskIdAndDeletedAtIsNull(UUID taskId);

    List<TaskTimeEntry> findByTaskIdAndUserIdAndDeletedAtIsNull(UUID taskId, UUID userId);

    List<TaskTimeEntry> findByUserIdAndDeletedAtIsNull(UUID userId);

    List<TaskTimeEntry> findByTaskIdAndEntryTypeAndDeletedAtIsNull(UUID taskId, TaskTimeEntryType entryType);

    @Query("SELECT SUM(t.durationMinutes) FROM TaskTimeEntry t WHERE t.taskId = :taskId AND t.deletedAt IS NULL")
    Integer sumDurationMinutesByTaskId(@Param("taskId") UUID taskId);

    List<TaskTimeEntry> findByUserIdAndStartTimeGreaterThanEqualAndStartTimeLessThanAndDeletedAtIsNull(
            UUID userId, Instant from, Instant to);
}
