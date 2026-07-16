package com.xgy.cloud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "activity_signups")
public class ActivitySignup extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", length = 64)
    private String studentName;

    @Column(name = "parent_phone", nullable = false, length = 20)
    private String parentPhone;

    @Column(nullable = false, length = 32)
    private String status = "CONFIRMED";
}
