package com.xgy.cloud.service;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.common.RoleType;
import com.xgy.cloud.domain.Order;
import com.xgy.cloud.repository.CoursePackageRepository;
import com.xgy.cloud.repository.OrderRepository;
import com.xgy.cloud.repository.StudentRepository;
import com.xgy.cloud.security.SecurityUtils;
import com.xgy.cloud.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
