package com.xgy.cloud.repository;

import com.xgy.cloud.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop100ByOrgIdOrderByIdDesc(Long orgId);
}
