package com.xgy.cloud.service;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.common.RoleType;
import com.xgy.cloud.domain.CoursePackage;
import com.xgy.cloud.domain.Enrollment;
import com.xgy.cloud.domain.Order;
import com.xgy.cloud.domain.Student;
import com.xgy.cloud.repository.CoursePackageRepository;
import com.xgy.cloud.repository.EnrollmentRepository;
import com.xgy.cloud.repository.OrderRepository;
import com.xgy.cloud.repository.StudentRepository;
import com.xgy.cloud.security.SecurityUtils;
import com.xgy.cloud.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final OrderRepository orderRepository;
    private final StudentRepository studentRepository;
    private final CoursePackageRepository coursePackageRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Map<String, Object> summary(UserPrincipal principal) {
        assertFinanceAccess(principal);
        Long orgId = SecurityUtils.requireOrgId();
        List<Order> orders = loadOrders(principal, orgId);

        BigDecimal signed = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal refund = BigDecimal.ZERO;
        BigDecimal pending = BigDecimal.ZERO;

        Map<String, BigDecimal[]> byCampusAgg = new LinkedHashMap<>();
        Map<Long, BigDecimal> byPackage = new LinkedHashMap<>();

        for (Order o : orders) {
            signed = signed.add(nullSafe(o.getAmountTotal()));
            paid = paid.add(nullSafe(o.getAmountPaid()));
            refund = refund.add(nullSafe(o.getAmountRefund()));
            BigDecimal due = nullSafe(o.getAmountTotal()).subtract(nullSafe(o.getAmountPaid()));
            if (due.compareTo(BigDecimal.ZERO) > 0 && !"REFUNDED".equalsIgnoreCase(o.getStatus())) {
                pending = pending.add(due);
            }
            String campus = o.getCampus() != null ? o.getCampus() : "未分校区";
            BigDecimal[] agg = byCampusAgg.computeIfAbsent(campus,
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            agg[0] = agg[0].add(nullSafe(o.getAmountPaid()));
            agg[1] = agg[1].add(nullSafe(o.getAmountRefund()));
            byPackage.merge(o.getPackageId(), nullSafe(o.getAmountPaid()), BigDecimal::add);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orgId", orgId);
        result.put("signedAmount", signed);
        result.put("paidAmount", paid);
        result.put("refundAmount", refund);
        result.put("pendingAmount", pending);
        result.put("netAmount", paid.subtract(refund));
        result.put("orderCount", orders.size());
        result.put("byCampus", byCampusAgg.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            BigDecimal received = e.getValue()[0];
            BigDecimal refunded = e.getValue()[1];
            m.put("campus", e.getKey());
            m.put("received", received);
            m.put("refund", refunded);
            m.put("net", received.subtract(refunded));
            m.put("netPaid", received.subtract(refunded));
            return m;
        }).collect(Collectors.toList()));
        result.put("byPackage", byPackage.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("packageId", e.getKey());
            m.put("paidAmount", e.getValue());
            coursePackageRepository.findById(e.getKey())
                    .ifPresent(p -> m.put("packageName", p.getName()));
            return m;
        }).collect(Collectors.toList()));

        if (RoleType.PARTNER.name().equalsIgnoreCase(principal.getCurrentRole())) {
            BigDecimal shareRatio = new BigDecimal("0.15");
            result.put("shareRatio", shareRatio);
            result.put("shareAmount", paid.subtract(refund).multiply(shareRatio).setScale(2, RoundingMode.HALF_UP));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> orders(UserPrincipal principal) {
        assertFinanceAccess(principal);
        Long orgId = SecurityUtils.requireOrgId();
        return loadOrders(principal, orgId).stream().map(this::toView).collect(Collectors.toList());
    }

    /** 教务/合伙/管理可录订单；可选同步创建报读 */
    @Transactional
    public Map<String, Object> createOrder(UserPrincipal principal, OrderCreateRequest req) {
        SecurityUtils.requireRole(RoleType.ACADEMIC, RoleType.PARTNER, RoleType.ADMIN);
        Long orgId = SecurityUtils.requireOrgId();
        if (req.getStudentId() == null || req.getPackageId() == null) {
            throw new BizException("请指定学员与教案");
        }
        Student student = studentRepository.findByIdAndOrgId(req.getStudentId(), orgId)
                .orElseThrow(() -> new BizException("学员不存在"));
        CoursePackage pkg = coursePackageRepository.findByIdAndOrgId(req.getPackageId(), orgId)
                .orElseThrow(() -> new BizException("教案不存在"));

        Order order = new Order();
        order.setOrgId(orgId);
        order.setStudentId(student.getId());
        order.setPackageId(pkg.getId());
        order.setCampus(StringUtils.hasText(req.getCampus()) ? req.getCampus() : student.getCampus());
        BigDecimal total = req.getAmountTotal() != null ? req.getAmountTotal() : nullSafe(pkg.getPrice());
        BigDecimal paid = req.getAmountPaid() != null ? req.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal refund = req.getAmountRefund() != null ? req.getAmountRefund() : BigDecimal.ZERO;
        order.setAmountTotal(total);
        order.setAmountPaid(paid);
        order.setAmountRefund(refund);
        order.setStatus(normalizeOrderStatus(req.getStatus(), paid, total));
        order.setChannel(StringUtils.hasText(req.getChannel()) ? req.getChannel() : "线下");
        order.setRemark(req.getRemark());
        if (RoleType.PARTNER.name().equalsIgnoreCase(principal.getCurrentRole())) {
            order.setPartnerUserId(principal.getUserId());
        } else if (req.getPartnerUserId() != null) {
            order.setPartnerUserId(req.getPartnerUserId());
        }
        orderRepository.save(order);

        if (Boolean.TRUE.equals(req.getCreateEnrollment())) {
            Enrollment enrollment = enrollmentRepository
                    .findByStudentIdAndPackageId(student.getId(), pkg.getId())
                    .orElseGet(Enrollment::new);
            if (enrollment.getId() == null) {
                enrollment.setOrgId(orgId);
                enrollment.setStudentId(student.getId());
                enrollment.setPackageId(pkg.getId());
                int totalLessons = pkg.getLessonCount() != null ? pkg.getLessonCount() : 0;
                enrollment.setTotalLessons(totalLessons);
                enrollment.setRemainLessons(totalLessons);
                enrollment.setStatus("ACTIVE");
                enrollment.setSource("订单同步");
                enrollment.setPaidAmount(paid);
                enrollmentRepository.save(enrollment);
            }
        }

        auditService.log(principal, "ORDER_CREATE", "order", order.getId(),
                student.getStudentName() + " / " + pkg.getName());
        return toView(order);
    }

    private String normalizeOrderStatus(String status, BigDecimal paid, BigDecimal total) {
        if (StringUtils.hasText(status)) {
            String s = status.trim();
            if ("已缴费".equals(s) || "PAID".equalsIgnoreCase(s)) return "PAID";
            if ("待缴费".equals(s) || "PENDING".equalsIgnoreCase(s)) return "PENDING";
            if ("已退费".equals(s) || "REFUNDED".equalsIgnoreCase(s)) return "REFUNDED";
            return s.toUpperCase();
        }
        if (paid.compareTo(BigDecimal.ZERO) <= 0) return "PENDING";
        if (paid.compareTo(total) >= 0) return "PAID";
        return "PARTIAL";
    }

    private List<Order> loadOrders(UserPrincipal principal, Long orgId) {
        if (RoleType.PARTNER.name().equalsIgnoreCase(principal.getCurrentRole())) {
            return orderRepository.findByOrgIdAndPartnerUserIdOrderByIdDesc(orgId, principal.getUserId());
        }
        return orderRepository.findByOrgIdOrderByIdDesc(orgId);
    }

    private void assertFinanceAccess(UserPrincipal principal) {
        RoleType role = RoleType.from(principal.getCurrentRole());
        if (!role.canAccessFinance()) {
            throw new BizException(403, "仅合伙人/管理员可访问财务");
        }
    }

    private Map<String, Object> toView(Order o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("orgId", o.getOrgId());
        m.put("studentId", o.getStudentId());
        m.put("packageId", o.getPackageId());
        m.put("campus", o.getCampus());
        m.put("amountTotal", o.getAmountTotal());
        m.put("amountPaid", o.getAmountPaid());
        m.put("amountRefund", o.getAmountRefund());
        m.put("status", o.getStatus());
        m.put("channel", o.getChannel());
        m.put("partnerUserId", o.getPartnerUserId());
        m.put("remark", o.getRemark());
        m.put("createdAt", o.getCreatedAt());
        studentRepository.findById(o.getStudentId()).ifPresent(s -> {
            m.put("studentName", s.getStudentName());
            m.put("parentPhone", s.getParentPhone());
        });
        coursePackageRepository.findById(o.getPackageId()).ifPresent(p -> {
            m.put("packageName", p.getName());
            m.put("lessonCount", p.getLessonCount());
        });
        return m;
    }

    private BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @Data
    public static class OrderCreateRequest {
        private Long studentId;
        private Long packageId;
        private String campus;
        private BigDecimal amountTotal;
        private BigDecimal amountPaid;
        private BigDecimal amountRefund;
        private String status;
        private String channel;
        private Long partnerUserId;
        private String remark;
        private Boolean createEnrollment;
    }
}
