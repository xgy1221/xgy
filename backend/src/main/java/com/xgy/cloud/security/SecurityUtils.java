package com.xgy.cloud.security;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.common.RoleType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BizException(401, "未登录");
        }
        return principal;
    }

    public static void requireRole(RoleType... allowed) {
        UserPrincipal user = currentUser();
        String current = user.getCurrentRole();
        if (current == null) {
            throw new BizException(403, "未选择角色");
        }
        RoleType role = RoleType.from(current);
        for (RoleType a : allowed) {
            if (a == role) {
                return;
            }
        }
        throw new BizException(403, "当前角色无权限");
    }

    public static boolean isParent() {
        UserPrincipal user = currentUser();
        return user.getCurrentRole() != null
                && RoleType.PARENT.name().equalsIgnoreCase(user.getCurrentRole());
    }

    public static Long requireOrgId() {
        UserPrincipal user = currentUser();
        if (user.getOrgId() == null) {
            throw new BizException("缺少机构上下文，请先切换角色或学员");
        }
        return user.getOrgId();
    }
}
