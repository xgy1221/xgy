package com.xgy.cloud.service;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.common.RoleType;
import com.xgy.cloud.domain.Org;
import com.xgy.cloud.domain.UserAccount;
import com.xgy.cloud.domain.UserRole;
import com.xgy.cloud.repository.OrgRepository;
import com.xgy.cloud.repository.UserAccountRepository;
import com.xgy.cloud.repository.UserRoleRepository;
import com.xgy.cloud.security.SecurityUtils;
import com.xgy.cloud.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final OrgRepository orgRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UserPrincipal principal) {
        requireAdmin(principal);
        Long orgId = SecurityUtils.requireOrgId();
        Set<Long> staffIds = userRoleRepository.findAll().stream()
                .filter(r -> orgId.equals(r.getOrgId()))
                .map(UserRole::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return staffIds.stream()
                .map(userAccountRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(UserAccount::getId))
                .map(u -> toView(u, orgId))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> grantRole(UserPrincipal principal, GrantRoleRequest req) {
        requireAdmin(principal);
        Long orgId = SecurityUtils.requireOrgId();
        if (!StringUtils.hasText(req.getPhone()) || !StringUtils.hasText(req.getRole())) {
            throw new BizException("请指定手机号与角色");
        }
        RoleType role = RoleType.from(req.getRole());
        if (role == RoleType.PARENT) {
            throw new BizException("请勿通过此接口授予家长角色");
        }
        UserAccount user = userAccountRepository.findByPhone(req.getPhone())
                .orElseGet(() -> {
                    UserAccount u = new UserAccount();
                    u.setPhone(req.getPhone());
                    u.setName(StringUtils.hasText(req.getName()) ? req.getName() : "员工");
                    u.setAvatarText((StringUtils.hasText(req.getName()) ? req.getName() : "员").substring(0, 1));
                    u.setStatus("ACTIVE");
                    return userAccountRepository.save(u);
                });
        if (StringUtils.hasText(req.getName())) {
            user.setName(req.getName());
            user.setAvatarText(req.getName().substring(0, 1));
            userAccountRepository.save(user);
        }
        boolean exists = userRoleRepository.findByUserId(user.getId()).stream()
                .anyMatch(r -> role.name().equals(r.getRole()) && orgId.equals(r.getOrgId()));
        if (!exists) {
            UserRole ur = new UserRole();
            ur.setUserId(user.getId());
            ur.setRole(role.name());
            ur.setOrgId(orgId);
            userRoleRepository.save(ur);
        }
        auditService.log(principal, "GRANT_ROLE", "user", user.getId(), role.name() + " / " + req.getPhone());
        return toView(user, orgId);
    }

    @Transactional
    public Map<String, Object> revokeRole(UserPrincipal principal, Long userId, String role) {
        requireAdmin(principal);
        Long orgId = SecurityUtils.requireOrgId();
        RoleType roleType = RoleType.from(role);
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));
        List<UserRole> roles = userRoleRepository.findByUserId(userId);
        roles.stream()
                .filter(r -> roleType.name().equals(r.getRole()) && orgId.equals(r.getOrgId()))
                .findFirst()
                .ifPresent(userRoleRepository::delete);
        auditService.log(principal, "REVOKE_ROLE", "user", userId, roleType.name());
        return toView(user, orgId);
    }

    private void requireAdmin(UserPrincipal principal) {
        if (!RoleType.ADMIN.name().equalsIgnoreCase(principal.getCurrentRole())) {
            throw new BizException(403, "仅管理员可管理账号");
        }
    }

    private Map<String, Object> toView(UserAccount u, Long orgId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("phone", u.getPhone());
        m.put("name", u.getName());
        m.put("avatarText", u.getAvatarText());
        m.put("status", u.getStatus());
        List<Map<String, Object>> roles = userRoleRepository.findByUserId(u.getId()).stream()
                .filter(r -> orgId.equals(r.getOrgId()) || r.getOrgId() == null)
                .map(r -> {
                    Map<String, Object> rm = new LinkedHashMap<>();
                    rm.put("role", r.getRole());
                    rm.put("orgId", r.getOrgId());
                    if (r.getOrgId() != null) {
                        orgRepository.findById(r.getOrgId()).ifPresent(o -> {
                            rm.put("orgCode", o.getCode());
                            rm.put("orgName", o.getName());
                        });
                    }
                    return rm;
                }).collect(Collectors.toList());
        m.put("roles", roles);
        return m;
    }

    @Data
    public static class GrantRoleRequest {
        private String phone;
        private String name;
        private String role;
    }
}
