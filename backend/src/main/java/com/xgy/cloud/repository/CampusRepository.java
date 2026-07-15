package com.xgy.cloud.repository;

import com.xgy.cloud.domain.Campus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampusRepository extends JpaRepository<Campus, Long> {
    List<Campus> findByOrgId(Long orgId);
}
