package com.xgy.cloud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "classes")
public class Clazz extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(length = 128)
    private String campus;

    @Column(length = 64)
    private String room;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";
}
