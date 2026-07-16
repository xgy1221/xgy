package com.xgy.cloud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class UserAccount extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(length = 64)
    private String name;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "avatar_text", length = 8)
    private String avatarText;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";
}
