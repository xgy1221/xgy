package com.xgy.cloud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "students")
public class Student extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "parent_phone", nullable = false, length = 20)
    private String parentPhone;

    @Column(name = "parent_name", length = 64)
    private String parentName;

    @Column(name = "student_name", nullable = false, length = 64)
    private String studentName;

    @Column(length = 16)
    private String gender;

    @Column(length = 32)
    private String grade;

    private LocalDate birthday;

    @Column(length = 128)
    private String campus;

    @Column(length = 512)
    private String remark;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
