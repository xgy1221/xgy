package com.xgy.cloud.tenant;

public final class TenantContext {

    private static final ThreadLocal<Long> ORG_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setOrgId(Long orgId) {
        ORG_ID.set(orgId);
    }

    public static Long getOrgId() {
        return ORG_ID.get();
    }

    public static Long requireOrgId() {
        Long orgId = ORG_ID.get();
        if (orgId == null) {
            throw new com.xgy.cloud.common.BizException("缺少机构上下文");
        }
        return orgId;
    }

    public static void clear() {
        ORG_ID.remove();
    }
}
