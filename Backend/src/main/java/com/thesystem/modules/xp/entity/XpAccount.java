package com.thesystem.modules.xp.entity;

import com.thesystem.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "xp_accounts")
public class XpAccount extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "current_xp", nullable = false)
    private Integer currentXp = 0;

    @Column(name = "current_level", nullable = false)
    private Integer currentLevel = 1;

    @Column(name = "total_xp_earned", nullable = false)
    private Integer totalXpEarned = 0;

    @Column(name = "total_xp_spent", nullable = false)
    private Integer totalXpSpent = 0;

    @Column(name = "lifetime_xp", nullable = false)
    private Integer lifetimeXp = 0;

    @Column(name = "level_progress", nullable = false)
    private Double levelProgress = 0.0;

    public XpAccount() {
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

    public Integer getCurrentXp() {
        return currentXp;
    }

    public void setCurrentXp(Integer currentXp) {
        this.currentXp = currentXp;
    }

    public Integer getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Integer currentLevel) {
        this.currentLevel = currentLevel;
    }

    public Integer getTotalXpEarned() {
        return totalXpEarned;
    }

    public void setTotalXpEarned(Integer totalXpEarned) {
        this.totalXpEarned = totalXpEarned;
    }

    public Integer getTotalXpSpent() {
        return totalXpSpent;
    }

    public void setTotalXpSpent(Integer totalXpSpent) {
        this.totalXpSpent = totalXpSpent;
    }

    public Integer getLifetimeXp() {
        return lifetimeXp;
    }

    public void setLifetimeXp(Integer lifetimeXp) {
        this.lifetimeXp = lifetimeXp;
    }

    public Double getLevelProgress() {
        return levelProgress;
    }

    public void setLevelProgress(Double levelProgress) {
        this.levelProgress = levelProgress;
    }
}
