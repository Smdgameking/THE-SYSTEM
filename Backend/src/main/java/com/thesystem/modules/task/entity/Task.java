package com.thesystem.modules.task.entity;

import com.thesystem.modules.task.enums.TaskExecutionType;
import com.thesystem.modules.task.enums.TaskPriority;
import com.thesystem.modules.task.enums.TaskStatus;
import com.thesystem.modules.task.enums.TaskVisibility;
import com.thesystem.modules.task.enums.TaskDifficulty;
import com.thesystem.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "goal_id")
    private UUID goalId;

    @Column(name = "parent_task_id")
    private UUID parentTaskId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status = TaskStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private TaskPriority priority = TaskPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_type")
    private TaskExecutionType executionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private TaskDifficulty difficulty;

    @Column(name = "category")
    private String category;

    @Column(name = "estimated_duration")
    private Integer estimatedDuration;

    @Column(name = "actual_duration")
    private Integer actualDuration;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "completed_date")
    private Instant completedDate;

    @Column(name = "reminder_date")
    private Instant reminderDate;

    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring = false;

    @Column(name = "recurring_config_id")
    private UUID recurringConfigId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private String tags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "jsonb")
    private String attachments;

    @Column(name = "notes")
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "completion_evidence", columnDefinition = "jsonb")
    private String completionEvidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_state", columnDefinition = "jsonb")
    private String executionState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_metadata", columnDefinition = "jsonb")
    private String customMetadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private TaskVisibility visibility = TaskVisibility.PRIVATE;

    public Task() {
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

    public UUID getGoalId() {
        return goalId;
    }

    public void setGoalId(UUID goalId) {
        this.goalId = goalId;
    }

    public UUID getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(UUID parentTaskId) {
        this.parentTaskId = parentTaskId;
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

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public TaskExecutionType getExecutionType() {
        return executionType;
    }

    public void setExecutionType(TaskExecutionType executionType) {
        this.executionType = executionType;
    }

    public TaskDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(TaskDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(Integer estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public Integer getActualDuration() {
        return actualDuration;
    }

    public void setActualDuration(Integer actualDuration) {
        this.actualDuration = actualDuration;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }

    public Instant getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Instant completedDate) {
        this.completedDate = completedDate;
    }

    public Instant getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(Instant reminderDate) {
        this.reminderDate = reminderDate;
    }

    public Boolean getIsRecurring() {
        return isRecurring;
    }

    public void setIsRecurring(Boolean isRecurring) {
        this.isRecurring = isRecurring;
    }

    public UUID getRecurringConfigId() {
        return recurringConfigId;
    }

    public void setRecurringConfigId(UUID recurringConfigId) {
        this.recurringConfigId = recurringConfigId;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getAttachments() {
        return attachments;
    }

    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCompletionEvidence() {
        return completionEvidence;
    }

    public void setCompletionEvidence(String completionEvidence) {
        this.completionEvidence = completionEvidence;
    }

    public String getExecutionState() {
        return executionState;
    }

    public void setExecutionState(String executionState) {
        this.executionState = executionState;
    }

    public String getCustomMetadata() {
        return customMetadata;
    }

    public void setCustomMetadata(String customMetadata) {
        this.customMetadata = customMetadata;
    }

    public TaskVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(TaskVisibility visibility) {
        this.visibility = visibility;
    }
}
