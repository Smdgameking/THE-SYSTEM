package com.thesystem.modules.task.entity;

import com.thesystem.modules.task.enums.DependencyStatus;
import com.thesystem.modules.task.enums.DependencyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_dependencies")
public class TaskDependency {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "depends_on_task_id", nullable = false)
    private UUID dependsOnTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false)
    private DependencyType dependencyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DependencyStatus status = DependencyStatus.PENDING;

    @Column(name = "resolved_date")
    private Instant resolvedDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public TaskDependency() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public UUID getDependsOnTaskId() {
        return dependsOnTaskId;
    }

    public void setDependsOnTaskId(UUID dependsOnTaskId) {
        this.dependsOnTaskId = dependsOnTaskId;
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }

    public void setDependencyType(DependencyType dependencyType) {
        this.dependencyType = dependencyType;
    }

    public DependencyStatus getStatus() {
        return status;
    }

    public void setStatus(DependencyStatus status) {
        this.status = status;
    }

    public Instant getResolvedDate() {
        return resolvedDate;
    }

    public void setResolvedDate(Instant resolvedDate) {
        this.resolvedDate = resolvedDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
