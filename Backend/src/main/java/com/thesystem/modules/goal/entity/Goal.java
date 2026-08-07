package com.thesystem.modules.goal.entity;

import com.thesystem.modules.goal.enums.CompletionStrategy;
import com.thesystem.modules.goal.enums.GoalDifficulty;
import com.thesystem.modules.goal.enums.GoalPriority;
import com.thesystem.modules.goal.enums.GoalStatus;
import com.thesystem.modules.goal.enums.GoalVisibility;
import com.thesystem.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "goals")
public class Goal extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "category")
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private GoalPriority priority = GoalPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private GoalDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GoalStatus status = GoalStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private GoalVisibility visibility = GoalVisibility.PRIVATE;

    @Column(name = "estimated_xp", nullable = false)
    private Integer estimatedXp = 0;

    @Column(name = "current_progress", nullable = false)
    private Integer currentProgress = 0;

    @Column(name = "completion_percentage", nullable = false)
    private Double completionPercentage = 0.0;

    @Column(name = "target_date")
    private java.time.Instant targetDate;

    @Column(name = "completed_date")
    private java.time.Instant completedDate;

    @Column(name = "archived_date")
    private java.time.Instant archivedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_strategy")
    private CompletionStrategy completionStrategy;

    @Column(name = "tags", columnDefinition = "jsonb")
    private String tags;

    @Column(name = "custom_metadata", columnDefinition = "jsonb")
    private String customMetadata;

    public Goal() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public GoalPriority getPriority() {
        return priority;
    }

    public void setPriority(GoalPriority priority) {
        this.priority = priority;
    }

    public GoalDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(GoalDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public void setStatus(GoalStatus status) {
        this.status = status;
    }

    public GoalVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(GoalVisibility visibility) {
        this.visibility = visibility;
    }

    public Integer getEstimatedXp() {
        return estimatedXp;
    }

    public void setEstimatedXp(Integer estimatedXp) {
        this.estimatedXp = estimatedXp;
    }

    public Integer getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(Integer currentProgress) {
        this.currentProgress = currentProgress;
    }

    public Double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(Double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public java.time.Instant getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(java.time.Instant targetDate) {
        this.targetDate = targetDate;
    }

    public java.time.Instant getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(java.time.Instant completedDate) {
        this.completedDate = completedDate;
    }

    public java.time.Instant getArchivedDate() {
        return archivedDate;
    }

    public void setArchivedDate(java.time.Instant archivedDate) {
        this.archivedDate = archivedDate;
    }

    public CompletionStrategy getCompletionStrategy() {
        return completionStrategy;
    }

    public void setCompletionStrategy(CompletionStrategy completionStrategy) {
        this.completionStrategy = completionStrategy;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getCustomMetadata() {
        return customMetadata;
    }

    public void setCustomMetadata(String customMetadata) {
        this.customMetadata = customMetadata;
    }
}
