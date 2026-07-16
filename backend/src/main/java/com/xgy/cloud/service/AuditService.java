package com.xgy.cloud.service;

import com.xgy.cloud.domain.AuditLog;
import com.xgy.cloud.repository.AuditLogRepository;
import com.xgy.cloud.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(UserPrincipal principal, String action, String targetType, Object targetId, String detail) {
        AuditLog row = new AuditLog();
        if (principal != null) {
            row.setUserId(principal.getUserId());
            row.setPhone(principal.getPhone());
            row.setOrgId(principal.getOrgId());
        }
        row.setAction(action);
        row.setTargetType(targetType);
        row.setTargetId(targetId == null ? null : String.valueOf(targetId));
        row.setDetail(detail);
        row.setIp(resolveIp());
        row.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(row);
    }

    @Transactional
    public void logAnon(String phone, String action, String detail) {
        AuditLog row = new AuditLog();
        row.setPhone(phone);
        row.setAction(action);
        row.setDetail(detail);
        row.setIp(resolveIp());
        row.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(row);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> recent(Long orgId) {
        return auditLogRepository.findTop100ByOrgIdOrderByIdDesc(orgId).stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("action", a.getAction());
            m.put("phone", a.getPhone());
            m.put("targetType", a.getTargetType());
            m.put("targetId", a.getTargetId());
            m.put("detail", a.getDetail());
            m.put("createdAt", a.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
    }

    private String resolveIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
