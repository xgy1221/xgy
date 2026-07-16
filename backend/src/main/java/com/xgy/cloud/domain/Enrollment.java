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
@Table(name = "enrollments")
public class Enrollment extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "total_lessons", nullable = false)
    private Integer totalLessons = 0;

    @Column(name = "remain_lessons", nullable = false)
    private Integer remainLessons = 0;

    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(length = 64)
    private String source;
}
