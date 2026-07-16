package com.xgy.cloud.service;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.domain.Teacher;
import com.xgy.cloud.repository.TeacherRepository;
import com.xgy.cloud.security.SecurityUtils;
import com.xgy.cloud.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UserPrincipal principal) {
        Long orgId = SecurityUtils.requireOrgId();
        return teacherRepository.findByOrgIdOrderByIdDesc(orgId).stream()
                .map(this::toView).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(UserPrincipal principal, TeacherCreateRequest req) {
        if (SecurityUtils.isParent()) {
            throw new BizException(403, "家长无权操作");
        }
        Long orgId = SecurityUtils.requireOrgId();
        Teacher t = new Teacher();
        t.setOrgId(orgId);
        t.setUserId(req.getUserId());
        t.setName(req.getName());
        t.setPhone(req.getPhone());
        t.setTitle(req.getTitle());
        t.setCampus(req.getCampus());
        t.setSubjects(req.getSubjects());
        t.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus() : "ACTIVE");
        teacherRepository.save(t);
        return toView(t);
    }

    @Transactional
    public Map<String, Object> update(UserPrincipal principal, Long id, TeacherCreateRequest req) {
        if (SecurityUtils.isParent()) {
            throw new BizException(403, "家长无权操作");
        }
        Long orgId = SecurityUtils.requireOrgId();
        Teacher t = teacherRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new BizException("教师不存在"));
        if (req.getUserId() != null) {
            t.setUserId(req.getUserId());
        }
        if (StringUtils.hasText(req.getName())) {
            t.setName(req.getName());
        }
        if (req.getPhone() != null) {
            t.setPhone(req.getPhone());
        }
        if (req.getTitle() != null) {
            t.setTitle(req.getTitle());
        }
        if (req.getCampus() != null) {
            t.setCampus(req.getCampus());
        }
        if (req.getSubjects() != null) {
            t.setSubjects(req.getSubjects());
        }
        if (StringUtils.hasText(req.getStatus())) {
            t.setStatus(req.getStatus());
        }
        teacherRepository.save(t);
        return toView(t);
    }

    private Map<String, Object> toView(Teacher t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("orgId", t.getOrgId());
        m.put("userId", t.getUserId());
        m.put("name", t.getName());
        m.put("phone", t.getPhone());
        m.put("title", t.getTitle());
        m.put("campus", t.getCampus());
        m.put("status", t.getStatus());
        m.put("subjects", t.getSubjects());
        return m;
    }

    @Data
    public static class TeacherCreateRequest {
        private Long userId;
        private String name;
        private String phone;
        private String title;
        private String campus;
        private String subjects;
        private String status;
    }
}
