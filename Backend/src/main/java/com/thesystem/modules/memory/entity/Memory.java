package com.thesystem.modules.memory.entity;

import com.thesystem.modules.memory.enums.MemoryImportance;
import com.thesystem.modules.memory.enums.MemorySource;
import com.thesystem.modules.memory.enums.MemoryType;
import com.thesystem.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "memories")
public class Memory extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MemoryType type = MemoryType.NOTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "importance", nullable = false)
    private MemoryImportance importance = MemoryImportance.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private MemorySource source = MemorySource.MANUAL;

    @Column(name = "source_id")
    private UUID sourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private String tags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_metadata", columnDefinition = "jsonb")
    private String customMetadata;

    public Memory() {
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MemoryType getType() {
        return type;
    }

    public void setType(MemoryType type) {
        this.type = type;
    }

    public MemoryImportance getImportance() {
        return importance;
    }

    public void setImportance(MemoryImportance importance) {
        this.importance = importance;
    }

    public MemorySource getSource() {
        return source;
    }

    public void setSource(MemorySource source) {
        this.source = source;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
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
