package com.xgy.cloud.service;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.common.RoleType;
import com.xgy.cloud.config.AppConfig;
import com.xgy.cloud.domain.Org;
import com.xgy.cloud.domain.Student;
import com.xgy.cloud.domain.UserAccount;
import com.xgy.cloud.domain.UserRole;
import com.xgy.cloud.repository.OrgRepository;
import com.xgy.cloud.repository.StudentRepository;
import com.xgy.cloud.repository.UserAccountRepository;
import com.xgy.cloud.repository.UserRoleRepository;
import com.xgy.cloud.security.JwtProperties;
import com.xgy.cloud.security.JwtService;
import com.xgy.cloud.security.UserPrincipal;
import com.xgy.cloud.tenant.TenantContext;
import com.xgy.cloud.web.auth.LoginRequest;
import com.xgy.cloud.web.auth.SwitchRoleRequest;
import com.xgy.cloud.web.auth.SwitchStudentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final StudentRepository studentRepository;
    private final OrgRepository orgRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final SessionRedisService sessionRedisService;
    private final AppConfig.SmsProperties smsProperties;

    @Transactional(readOnly = true)
    public Map<String, Object> login(LoginRequest request) {
        if (!smsProperties.getDemoCode().equals(request.getSmsCode())) {
            throw new BizException("验证码错误");
        }
        UserAccount user = userAccountRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BizException("账号不存在，请联系机构开通"));
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BizException("账号已停用");
        }

        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        if (userRoles.isEmpty()) {
            throw new BizException("账号未分配角色");
        }

        List<Map<String, Object>> roleViews = buildRoleViews(userRoles);
        String lastRole = sessionRedisService.getLastRole(user.getPhone());
        Map<String, Object> selected = pickDefaultRole(roleViews, lastRole);

        Long orgId = selected.get("orgId") != null ? ((Number) selected.get("orgId")).longValue() : null;
        String currentRole = (String) selected.get("role");
        Long currentStudentId = null;

        if (RoleType.PARENT.name().equals(currentRole)) {
            currentStudentId = resolveDefaultStudent(user.getPhone());
            if (currentStudentId != null) {
                Student stu = studentRepository.findById(currentStudentId).orElse(null);
                if (stu != null) {
                    orgId = stu.getOrgId();
                }
            }
        }

        List<String> roleNames = roleViews.stream()
                .map(r -> (String) r.get("role"))
                .distinct()
                .collect(Collectors.toList());

        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getPhone(), orgId, roleNames, currentRole, currentStudentId);
        String token = jwtService.createToken(principal);

        Map<String, Object> sessionMeta = new HashMap<>();
        sessionMeta.put("phone", user.getPhone());
        sessionMeta.put("currentRole", currentRole);
        sessionMeta.put("orgId", orgId);
        sessionMeta.put("currentStudentId", currentStudentId);
        sessionMeta.put("loginAt", Instant.now().toString());
        sessionRedisService.saveSession(user.getId(), sessionMeta, Duration.ofDays(jwtProperties.getExpireDays()));
        sessionRedisService.saveLastRole(user.getPhone(), currentRole);
        if (currentStudentId != null) {
            sessionRedisService.saveLastStudent(user.getPhone(), currentStudentId);
        }

        return buildAuthPayload(token, user, roleViews, currentRole, orgId, currentStudentId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> me(UserPrincipal principal) {
        UserAccount user = userAccountRepository.findById(principal.getUserId())
                .orElseThrow(() -> new BizException("用户不存在"));
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        List<Map<String, Object>> roleViews = buildRoleViews(userRoles);
        return buildAuthPayload(null, user, roleViews,
                principal.getCurrentRole(), principal.getOrgId(), principal.getCurrentStudentId());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> switchRole(UserPrincipal principal, SwitchRoleRequest request) {
        RoleType roleType = RoleType.from(request.getRole());
        List<UserRole> userRoles = userRoleRepository.findByUserId(principal.getUserId());
        List<Map<String, Object>> roleViews = buildRoleViews(userRoles);

        Map<String, Object> matched = roleViews.stream()
                .filter(r -> roleType.name().equals(r.get("role")))
                .filter(r -> {
                    if (request.getOrgId() == null) {
                        return true;
                    }
                    Object oid = r.get("orgId");
                    return oid != null && request.getOrgId().equals(((Number) oid).longValue());
                })
                .findFirst()
                .orElseThrow(() -> new BizException("无权切换到该角色"));

        Long orgId = matched.get("orgId") != null ? ((Number) matched.get("orgId")).longValue() : null;
        Long currentStudentId = principal.getCurrentStudentId();

        if (roleType == RoleType.PARENT) {
            currentStudentId = resolveDefaultStudent(principal.getPhone());
            if (currentStudentId != null) {
                Student stu = studentRepository.findById(currentStudentId).orElse(null);
                if (stu != null) {
                    orgId = stu.getOrgId();
                }
            } else {
                orgId = null;
            }
        } else if (orgId == null && request.getOrgId() != null) {
            orgId = request.getOrgId();
        }

        List<String> roleNames = roleViews.stream()
                .map(r -> (String) r.get("role"))
                .distinct()
                .collect(Collectors.toList());

        UserPrincipal next = new UserPrincipal(
                principal.getUserId(), principal.getPhone(), orgId, roleNames, roleType.name(), currentStudentId);
        String token = jwtService.createToken(next);

        if (principal.getRawToken() != null) {
            Instant exp = jwtService.getExpireAt(principal.getRawToken());
            sessionRedisService.blacklistToken(principal.getRawToken(), exp);
        }

        sessionRedisService.saveLastRole(principal.getPhone(), roleType.name());
        Map<String, Object> sessionMeta = new HashMap<>();
        sessionMeta.put("currentRole", roleType.name());
        sessionMeta.put("orgId", orgId);
        sessionMeta.put("currentStudentId", currentStudentId);
        sessionRedisService.saveSession(principal.getUserId(), sessionMeta, Duration.ofDays(jwtProperties.getExpireDays()));

        TenantContext.setOrgId(orgId);
        UserAccount user = userAccountRepository.findById(principal.getUserId())
                .orElseThrow(() -> new BizException("用户不存在"));
        return buildAuthPayload(token, user, roleViews, roleType.name(), orgId, currentStudentId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> switchStudent(UserPrincipal principal, SwitchStudentRequest request) {
        if (!RoleType.PARENT.name().equalsIgnoreCase(principal.getCurrentRole())) {
            throw new BizException("仅家长可切换学员");
        }
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new BizException("学员不存在"));
        if (!principal.getPhone().equals(student.getParentPhone())) {
            throw new BizException("无权切换该学员");
        }

        List<UserRole> userRoles = userRoleRepository.findByUserId(principal.getUserId());
        List<Map<String, Object>> roleViews = buildRoleViews(userRoles);
        List<String> roleNames = roleViews.stream()
                .map(r -> (String) r.get("role"))
                .distinct()
                .collect(Collectors.toList());

        UserPrincipal next = new UserPrincipal(
                principal.getUserId(), principal.getPhone(), student.getOrgId(),
                roleNames, RoleType.PARENT.name(), student.getId());
        String token = jwtService.createToken(next);

        if (principal.getRawToken() != null) {
            sessionRedisService.blacklistToken(principal.getRawToken(), jwtService.getExpireAt(principal.getRawToken()));
        }
        sessionRedisService.saveLastStudent(principal.getPhone(), student.getId());
        Map<String, Object> sessionMeta = new HashMap<>();
        sessionMeta.put("currentRole", RoleType.PARENT.name());
        sessionMeta.put("orgId", student.getOrgId());
        sessionMeta.put("currentStudentId", student.getId());
        sessionRedisService.saveSession(principal.getUserId(), sessionMeta, Duration.ofDays(jwtProperties.getExpireDays()));

        TenantContext.setOrgId(student.getOrgId());
        UserAccount user = userAccountRepository.findById(principal.getUserId())
                .orElseThrow(() -> new BizException("用户不存在"));
        return buildAuthPayload(token, user, roleViews, RoleType.PARENT.name(), student.getOrgId(), student.getId());
    }

    public void logout(UserPrincipal principal) {
        if (principal.getRawToken() != null) {
            Instant exp = jwtService.getExpireAt(principal.getRawToken());
            sessionRedisService.blacklistToken(principal.getRawToken(), exp);
        }
        sessionRedisService.clearSession(principal.getUserId());
    }

    private Long resolveDefaultStudent(String phone) {
        Long last = sessionRedisService.getLastStudent(phone);
        if (last != null) {
            Optional<Student> stu = studentRepository.findById(last);
            if (stu.isPresent() && phone.equals(stu.get().getParentPhone())) {
                return last;
            }
        }
        return studentRepository.findByParentPhoneOrderByOrgIdAscIdAsc(phone).stream()
                .findFirst()
                .map(Student::getId)
                .orElse(null);
    }

    private List<Map<String, Object>> buildRoleViews(List<UserRole> userRoles) {
        Map<Long, Org> orgCache = new HashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserRole ur : userRoles) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", ur.getRole());
            m.put("orgId", ur.getOrgId());
            if (ur.getOrgId() != null) {
                Org org = orgCache.computeIfAbsent(ur.getOrgId(),
                        id -> orgRepository.findById(id).orElse(null));
                if (org != null) {
                    m.put("orgCode", org.getCode());
                    m.put("orgName", org.getName());
                }
            } else {
                m.put("orgCode", null);
                m.put("orgName", null);
            }
            result.add(m);
        }
        return result;
    }

    private Map<String, Object> pickDefaultRole(List<Map<String, Object>> roleViews, String lastRole) {
        if (StringUtils.hasText(lastRole)) {
            Optional<Map<String, Object>> hit = roleViews.stream()
                    .filter(r -> lastRole.equalsIgnoreCase((String) r.get("role")))
                    .findFirst();
            if (hit.isPresent()) {
                return hit.get();
            }
        }
        return roleViews.get(0);
    }

    private Map<String, Object> buildAuthPayload(String token, UserAccount user,
                                                 List<Map<String, Object>> roleViews,
                                                 String currentRole, Long orgId, Long currentStudentId) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (token != null) {
            data.put("token", token);
        }
        data.put("userId", user.getId());
        data.put("phone", user.getPhone());
        data.put("name", user.getName());
        data.put("avatarText", user.getAvatarText());
        data.put("roles", roleViews);
        data.put("currentRole", currentRole);
        data.put("orgId", orgId);
        if (orgId != null) {
            orgRepository.findById(orgId).ifPresent(org -> {
                data.put("orgCode", org.getCode());
                data.put("orgName", org.getName());
            });
        } else {
            data.put("orgCode", null);
            data.put("orgName", null);
        }
        data.put("currentStudentId", currentStudentId);
        if (currentStudentId != null) {
            studentRepository.findById(currentStudentId).ifPresent(stu -> {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("id", stu.getId());
                s.put("studentName", stu.getStudentName());
                s.put("orgId", stu.getOrgId());
                s.put("grade", stu.getGrade());
                s.put("campus", stu.getCampus());
                orgRepository.findById(stu.getOrgId()).ifPresent(o -> {
                    s.put("orgCode", o.getCode());
                    s.put("orgName", o.getName());
                });
                data.put("currentStudent", s);
            });
        }
        if (RoleType.PARENT.name().equalsIgnoreCase(currentRole)) {
            List<Map<String, Object>> kids = studentRepository
                    .findByParentPhoneOrderByOrgIdAscIdAsc(user.getPhone())
                    .stream()
                    .map(stu -> {
                        Map<String, Object> s = new LinkedHashMap<>();
                        s.put("id", stu.getId());
                        s.put("studentName", stu.getStudentName());
                        s.put("orgId", stu.getOrgId());
                        s.put("grade", stu.getGrade());
                        s.put("campus", stu.getCampus());
                        orgRepository.findById(stu.getOrgId()).ifPresent(o -> {
                            s.put("orgCode", o.getCode());
                            s.put("orgName", o.getName());
                        });
                        return s;
                    })
                    .collect(Collectors.toList());
            data.put("students", kids);
        }
        return data;
    }
}
