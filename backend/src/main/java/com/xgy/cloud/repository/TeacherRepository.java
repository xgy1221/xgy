package com.xgy.cloud.repository;

import com.xgy.cloud.domain.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    List<Teacher> findByOrgIdOrderByIdDesc(Long orgId);

    Optional<Teacher> findByIdAndOrgId(Long id, Long orgId);

    Optional<Teacher> findByOrgIdAndUserId(Long orgId, Long userId);
}
