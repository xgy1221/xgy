package com.xgy.cloud.repository;

import com.xgy.cloud.domain.Org;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrgRepository extends JpaRepository<Org, Long> {
    Optional<Org> findByCode(String code);
}
