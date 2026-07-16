package com.xgy.cloud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "course_packages")
public class CoursePackage extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 64)
    private String subject;

    @Column(length = 64)
    private String grade;

    @Column(name = "lesson_count", nullable = false)
    private Integer lessonCount = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false, length = 32)
    private String status = "ON_SHELF";

    @Column(columnDefinition = "TEXT")
    private String outline;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
