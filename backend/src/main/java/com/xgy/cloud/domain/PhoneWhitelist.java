package com.xgy.cloud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "phone_whitelist")
public class PhoneWhitelist extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "parent_name", length = 64)
    private String parentName;

    @Column(length = 255)
    private String note;
}
