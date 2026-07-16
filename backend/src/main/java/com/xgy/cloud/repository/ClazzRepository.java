package com.xgy.cloud.repository;

import com.xgy.cloud.domain.Clazz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClazzRepository extends JpaRepository<Clazz, Long> {
    List<Clazz> findByOrgIdOrderByIdDesc(Long orgId);

    Optional<Clazz> findByIdAndOrgId(Long id, Long orgId);
}
