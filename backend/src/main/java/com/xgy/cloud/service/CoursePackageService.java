package com.xgy.cloud.service;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.domain.CoursePackage;
import com.xgy.cloud.repository.CoursePackageRepository;
import com.xgy.cloud.security.SecurityUtils;
import com.xgy.cloud.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoursePackageService {

    private final CoursePackageRepository coursePackageRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UserPrincipal principal) {
        Long orgId = resolveOrgId(principal);
        return coursePackageRepository.findByOrgIdOrderByIdDesc(orgId)
                .stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(UserPrincipal principal, PackageSaveRequest req) {
        if (SecurityUtils.isParent()) {
            throw new BizException(403, "家长无权操作");
        }
        Long orgId = SecurityUtils.requireOrgId();
        CoursePackage pkg = new CoursePackage();
        pkg.setOrgId(orgId);
        apply(pkg, req);
        coursePackageRepository.save(pkg);
        return toView(pkg);
    }

    @Transactional
    public Map<String, Object> update(UserPrincipal principal, Long id, PackageSaveRequest req) {
        if (SecurityUtils.isParent()) {
            throw new BizException(403, "家长无权操作");
        }
        Long orgId = SecurityUtils.requireOrgId();
        CoursePackage pkg = coursePackageRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new BizException("教案不存在"));
        apply(pkg, req);
        coursePackageRepository.save(pkg);
        return toView(pkg);
    }

    @Transactional
    public Map<String, Object> archive(UserPrincipal principal, Long id) {
        if (SecurityUtils.isParent()) {
            throw new BizException(403, "家长无权操作");
        }
        Long orgId = SecurityUtils.requireOrgId();
        CoursePackage pkg = coursePackageRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new BizException("教案不存在"));
        pkg.setDeletedAt(LocalDateTime.now());
        pkg.setStatus("OFF_SHELF");
        coursePackageRepository.save(pkg);
        return toView(pkg);
    }

    private Long resolveOrgId(UserPrincipal principal) {
        if (principal.getOrgId() != null) {
            return principal.getOrgId();
        }
        return SecurityUtils.requireOrgId();
    }

    private void apply(CoursePackage pkg, PackageSaveRequest req) {
        if (StringUtils.hasText(req.getName())) {
            pkg.setName(req.getName());
        }
        if (req.getSubject() != null) {
            pkg.setSubject(req.getSubject());
        }
        if (req.getGrade() != null) {
            pkg.setGrade(req.getGrade());
        }
        if (req.getLessonCount() != null) {
            pkg.setLessonCount(req.getLessonCount());
        }
        if (req.getPrice() != null) {
            pkg.setPrice(req.getPrice());
        }
        if (StringUtils.hasText(req.getStatus())) {
            String st = req.getStatus().trim();
            if ("上架".equals(st) || "on_shelf".equalsIgnoreCase(st)) {
                pkg.setStatus("ON_SHELF");
            } else if ("下架".equals(st) || "off_shelf".equalsIgnoreCase(st)) {
                pkg.setStatus("OFF_SHELF");
            } else {
                pkg.setStatus(st.toUpperCase());
            }
        }
        if (req.getOutline() != null) {
            pkg.setOutline(req.getOutline());
        }
    }

    private Map<String, Object> toView(CoursePackage pkg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", pkg.getId());
        m.put("orgId", pkg.getOrgId());
        m.put("name", pkg.getName());
        m.put("subject", pkg.getSubject());
        m.put("grade", pkg.getGrade());
        m.put("lessonCount", pkg.getLessonCount());
        m.put("price", pkg.getPrice());
        m.put("status", pkg.getStatus());
        m.put("outline", pkg.getOutline());
        m.put("deletedAt", pkg.getDeletedAt());
        return m;
    }

    @Data
    public static class PackageSaveRequest {
        private String name;
        private String subject;
        private String grade;
        private Integer lessonCount;
        private BigDecimal price;
        private String status;
        private String outline;
    }
}
