package com.xgy.cloud.service;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.common.RoleType;
import com.xgy.cloud.domain.Org;
import com.xgy.cloud.domain.Student;
import com.xgy.cloud.repository.OrgRepository;
import com.xgy.cloud.repository.StudentRepository;
import com.xgy.cloud.security.SecurityUtils;
import com.xgy.cloud.security.UserPrincipal;
import com.xgy.cloud.tenant.TenantContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final OrgRepository orgRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UserPrincipal principal, String phone) {
        if (SecurityUtils.isParent()) {
            return studentRepository.findByParentPhoneOrderByOrgIdAscIdAsc(principal.getPhone())
                    .stream().filter(this::alive).map(this::toView).collect(Collectors.toList());
        }
        Long orgId = SecurityUtils.requireOrgId();
        if (StringUtils.hasText(phone)) {
            return studentRepository.findByOrgIdAndParentPhone(orgId, phone)
                    .stream().filter(this::alive).map(this::toView).collect(Collectors.toList());
        }
        return studentRepository.findByOrgIdOrderByIdDesc(orgId)
                .stream().filter(this::alive).map(this::toView).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> byPhone(UserPrincipal principal, String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new BizException("手机号不能为空");
        }
        if (SecurityUtils.isParent()) {
            if (!principal.getPhone().equals(phone)) {
                throw new BizException("家长只能查询自己的学员");
            }
            return studentRepository.findByParentPhoneOrderByOrgIdAscIdAsc(phone)
                    .stream().filter(this::alive).map(this::toView).collect(Collectors.toList());
        }
        Long orgId = SecurityUtils.requireOrgId();
        return studentRepository.findByOrgIdAndParentPhone(orgId, phone)
                .stream().filter(this::alive).map(this::toView).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(UserPrincipal principal, StudentSaveRequest req) {
        requireStaff();
        Long orgId = SecurityUtils.requireOrgId();
        studentRepository.findByOrgIdAndParentPhoneAndStudentName(orgId, req.getParentPhone(), req.getStudentName())
                .ifPresent(s -> {
                    throw new BizException("该学员已存在");
                });
        Student s = new Student();
        s.setOrgId(orgId);
        apply(s, req);
        studentRepository.save(s);
        TenantContext.setOrgId(orgId);
        return toView(s);
    }

    @Transactional
    public Map<String, Object> update(UserPrincipal principal, Long id, StudentSaveRequest req) {
        requireStaff();
        Long orgId = SecurityUtils.requireOrgId();
        Student s = studentRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new BizException("学员不存在"));
        if (StringUtils.hasText(req.getParentPhone()) && StringUtils.hasText(req.getStudentName())) {
            studentRepository.findByOrgIdAndParentPhoneAndStudentName(orgId, req.getParentPhone(), req.getStudentName())
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> {
                        throw new BizException("同机构下已存在同名学员");
                    });
        }
        apply(s, req);
        studentRepository.save(s);
        return toView(s);
    }

    @Transactional
    public Map<String, Object> archive(UserPrincipal principal, Long id) {
        requireStaff();
        Long orgId = SecurityUtils.requireOrgId();
        Student s = studentRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new BizException("学员不存在"));
        s.setDeletedAt(LocalDateTime.now());
        s.setStatus("ARCHIVED");
        studentRepository.save(s);
        return toView(s);
    }

    private boolean alive(Student s) {
        return s.getDeletedAt() == null;
    }

    private void apply(Student s, StudentSaveRequest req) {
        if (StringUtils.hasText(req.getParentPhone())) {
            s.setParentPhone(req.getParentPhone());
        }
        if (req.getParentName() != null) {
            s.setParentName(req.getParentName());
        }
        if (StringUtils.hasText(req.getStudentName())) {
            s.setStudentName(req.getStudentName());
        }
        if (req.getGender() != null) {
            s.setGender(req.getGender());
        }
        if (req.getGrade() != null) {
            s.setGrade(req.getGrade());
        }
        if (req.getBirthday() != null) {
            s.setBirthday(req.getBirthday());
        }
        if (req.getCampus() != null) {
            s.setCampus(req.getCampus());
        }
        if (req.getRemark() != null) {
            s.setRemark(req.getRemark());
        }
        if (StringUtils.hasText(req.getStatus())) {
            String st = req.getStatus().trim();
            if ("在读".equals(st) || "学习中".equals(st) || "active".equalsIgnoreCase(st)) {
                s.setStatus("ACTIVE");
            } else if ("停课".equals(st) || "暂停".equals(st) || "suspended".equalsIgnoreCase(st)) {
                s.setStatus("SUSPENDED");
            } else if ("结业".equals(st) || "已结业".equals(st) || "finished".equalsIgnoreCase(st)) {
                s.setStatus("FINISHED");
            } else {
                s.setStatus(st.toUpperCase());
            }
        }
    }

    private void requireStaff() {
        if (SecurityUtils.isParent()) {
            throw new BizException(403, "家长无权操作");
        }
        RoleType.from(SecurityUtils.currentUser().getCurrentRole());
    }

    private Map<String, Object> toView(Student s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("orgId", s.getOrgId());
        m.put("parentPhone", s.getParentPhone());
        m.put("parentName", s.getParentName());
        m.put("studentName", s.getStudentName());
        m.put("gender", s.getGender());
        m.put("grade", s.getGrade());
        m.put("birthday", s.getBirthday());
        m.put("campus", s.getCampus());
        m.put("remark", s.getRemark());
        m.put("status", s.getStatus());
        m.put("deletedAt", s.getDeletedAt());
        orgRepository.findById(s.getOrgId()).ifPresent(org -> {
            m.put("orgCode", org.getCode());
            m.put("orgName", org.getName());
        });
        return m;
    }

    @Data
    public static class StudentSaveRequest {
        private String parentPhone;
        private String parentName;
        private String studentName;
        private String gender;
        private String grade;
        private LocalDate birthday;
        private String campus;
        private String remark;
        private String status;
    }
}
