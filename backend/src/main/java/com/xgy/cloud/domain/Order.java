package com.xgy.cloud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(length = 128)
    private String campus;

    @Column(name = "amount_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountTotal = BigDecimal.ZERO;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "amount_refund", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountRefund = BigDecimal.ZERO;

    @Column(nullable = false, length = 32)
    private String status = "PENDING";

    @Column(length = 64)
    private String channel;

    @Column(name = "partner_user_id")
    private Long partnerUserId;

    @Column(length = 512)
    private String remark;
}
