package com.xgy.cloud.common;

public enum RoleType {
    PARENT,
    TEACHER,
    ACADEMIC,
    PARTNER,
    ADMIN;

    public static RoleType from(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException("角色不能为空");
        }
        try {
            return RoleType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BizException("无效角色: " + value);
        }
    }

    public boolean isStaff() {
        return this != PARENT;
    }

    public boolean canAccessFinance() {
        return this == PARTNER || this == ADMIN;
    }
}
