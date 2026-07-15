package com.xgy.cloud.repository;

import com.xgy.cloud.domain.PhoneWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhoneWhitelistRepository extends JpaRepository<PhoneWhitelist, Long> {
    List<PhoneWhitelist> findByOrgIdOrderByIdDesc(Long orgId);

    Optional<PhoneWhitelist> findByOrgIdAndPhone(Long orgId, String phone);
}
