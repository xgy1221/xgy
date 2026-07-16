package com.xgy.cloud.web.audit;

import com.xgy.cloud.common.ApiResponse;
import com.xgy.cloud.common.RoleType;
import com.xgy.cloud.security.SecurityUtils;
import com.xgy.cloud.security.UserPrincipal;
import com.xgy.cloud.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> recent(@AuthenticationPrincipal UserPrincipal principal) {
        SecurityUtils.requireRole(RoleType.ADMIN, RoleType.ACADEMIC);
        Long orgId = SecurityUtils.requireOrgId();
        return ApiResponse.ok(auditService.recent(orgId));
    }
}
