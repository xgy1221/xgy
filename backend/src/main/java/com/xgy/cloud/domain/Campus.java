package com.xgy.cloud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "campuses")
public class Campus extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";
}
