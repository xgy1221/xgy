package com.xgy.cloud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "teachers")
public class Teacher extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 64)
    private String title;

    @Column(length = 128)
    private String campus;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(length = 255)
    private String subjects;
}
