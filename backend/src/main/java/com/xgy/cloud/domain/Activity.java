package com.xgy.cloud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "activities")
public class Activity extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 64)
    private String category;

    @Column(name = "cover_tone", length = 32)
    private String coverTone = "teal";

    @Column(length = 128)
    private String campus;

    @Column(length = 255)
    private String address;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "enroll_deadline", nullable = false)
    private LocalDate enrollDeadline;

    @Column(nullable = false)
    private Integer capacity = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "target_grade", length = 64)
    private String targetGrade;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String highlights;

    @Column(columnDefinition = "TEXT")
    private String gallery;

    @Column(columnDefinition = "TEXT")
    private String recap;

    @Column(nullable = false)
    private Boolean published = true;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";
}
