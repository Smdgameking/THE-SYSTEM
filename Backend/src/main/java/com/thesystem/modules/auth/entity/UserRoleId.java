package com.thesystem.modules.auth.entity;

import java.io.Serializable;
import java.util.UUID;

public class UserRoleId implements Serializable {

    private UUID userId;
    private UUID roleId;

    public UserRoleId() {
    }

    public UserRoleId(UUID userId, UUID roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRoleId)) return false;
        UserRoleId that = (UserRoleId) o;
        return userId != null && userId.equals(that.userId) &&
               roleId != null && roleId.equals(that.roleId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userId, roleId);
    }
}
