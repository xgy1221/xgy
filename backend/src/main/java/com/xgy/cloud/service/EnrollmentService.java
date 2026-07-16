package com.xgy.cloud.service;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.domain.CoursePackage;
import com.xgy.cloud.domain.Enrollment;
import com.xgy.cloud.domain.Student;
import com.xgy.cloud.repository.CoursePackageRepository;
import com.xgy.cloud.repository.EnrollmentRepository;
import com.xgy.cloud.repository.StudentRepository;
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
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CoursePackageRepository coursePackageRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByStudent(UserPrincipal principal, Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BizException("学员不存在"));
        assertStudentAccess(principal, student);
        return enrollmentRepository.findByStudentIdOrderByIdDesc(studentId)
                .stream().map(this::toView).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByOrg(UserPrincipal principal) {
        SecurityUtils.requireStaff();
        Long orgId = SecurityUtils.requireOrgId();
        return enrollmentRepository.findByOrgIdOrderByIdDesc(orgId).stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> upsert(UserPrincipal principal, EnrollmentUpsertRequest req) {
        SecurityUtils.requireAcademicOrAdmin();
        Long orgId = SecurityUtils.requireOrgId();
        Student student = studentRepository.findByIdAndOrgId(req.getStudentId(), orgId)
                .orElseThrow(() -> new BizException("学员不存在"));
        CoursePackage pkg = coursePackageRepository.findByIdAndOrgId(req.getPackageId(), orgId)
                .orElseThrow(() -> new BizException("教案不存在"));

        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndPackageId(student.getId(), pkg.getId())
                .orElseGet(Enrollment::new);

        if (enrollment.getId() == null) {
            enrollment.setOrgId(orgId);
            enrollment.setStudentId(student.getId());
            enrollment.setPackageId(pkg.getId());
            int total = req.getTotalLessons() != null ? req.getTotalLessons() : pkg.getLessonCount();
            enrollment.setTotalLessons(total);
            enrollment.setRemainLessons(req.getRemainLessons() != null ? req.getRemainLessons() : total);
            enrollment.setStatus(normalizeStatus(req.getStatus(), "ACTIVE"));
            enrollment.setSource(StringUtils.hasText(req.getSource()) ? req.getSource() : "教务代录");
        } else {
            if (req.getTotalLessons() != null) {
                enrollment.setTotalLessons(req.getTotalLessons());
            }
            if (req.getRemainLessons() != null) {
                enrollment.setRemainLessons(req.getRemainLessons());
            }
            if (StringUtils.hasText(req.getStatus())) {
                enrollment.setStatus(normalizeStatus(req.getStatus(), enrollment.getStatus()));
            }
            if (StringUtils.hasText(req.getSource())) {
                enrollment.setSource(req.getSource());
            }
        }
        enrollmentRepository.save(enrollment);
        return toView(enrollment);
    }

    /** 兼容前端中文状态：学习中/已结业 → ACTIVE/FINISHED */
    private String normalizeStatus(String status, String fallback) {
        if (!StringUtils.hasText(status)) {
            return fallback;
        }
        String s = status.trim();
        if ("学习中".equals(s) || "在读".equals(s) || "active".equalsIgnoreCase(s)) {
            return "ACTIVE";
        }
        if ("已结业".equals(s) || "已完成".equals(s) || "finished".equalsIgnoreCase(s)
                || "completed".equalsIgnoreCase(s)) {
            return "FINISHED";
        }
        if ("暂停".equals(s) || "suspended".equalsIgnoreCase(s)) {
            return "SUSPENDED";
        }
        return s.toUpperCase();
    }

    private void assertStudentAccess(UserPrincipal principal, Student student) {
        if (SecurityUtils.isParent()) {
            if (!principal.getPhone().equals(student.getParentPhone())) {
                throw new BizException(403, "无权查看该学员报读");
            }
            return;
        }
        Long orgId = SecurityUtils.requireOrgId();
        if (!orgId.equals(student.getOrgId())) {
            throw new BizException(403, "学员不属于当前机构");
        }
    }

    private Map<String, Object> toView(Enrollment e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("orgId", e.getOrgId());
        m.put("studentId", e.getStudentId());
        m.put("packageId", e.getPackageId());
        m.put("totalLessons", e.getTotalLessons());
        m.put("remainLessons", e.getRemainLessons());
        int total = e.getTotalLessons() != null ? e.getTotalLessons() : 0;
        int remain = e.getRemainLessons() != null ? e.getRemainLessons() : 0;
        int used = Math.max(0, total - remain);
        m.put("usedLessons", used);
        m.put("progress", total > 0 ? Math.round(used * 1000.0 / total) / 10.0 : 0);
        m.put("paidAmount", e.getPaidAmount());
        m.put("status", e.getStatus());
        m.put("source", e.getSource());
        studentRepository.findById(e.getStudentId()).ifPresent(s -> {
            m.put("studentName", s.getStudentName());
            m.put("parentPhone", s.getParentPhone());
            m.put("campus", s.getCampus());
        });
        coursePackageRepository.findById(e.getPackageId()).ifPresent(pkg -> {
            m.put("packageName", pkg.getName());
            m.put("subject", pkg.getSubject());
            m.put("grade", pkg.getGrade());
            m.put("price", pkg.getPrice());
        });
        return m;
    }

    @Data
    public static class EnrollmentUpsertRequest {
        private Long studentId;
        private Long packageId;
        private Integer totalLessons;
        private Integer remainLessons;
        private String status;
        private String source;
    }
}
