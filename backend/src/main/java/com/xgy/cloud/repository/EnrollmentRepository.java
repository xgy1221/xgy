package com.xgy.cloud.repository;

import com.xgy.cloud.domain.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentIdOrderByIdDesc(Long studentId);

    List<Enrollment> findByOrgIdAndStudentId(Long orgId, Long studentId);

    Optional<Enrollment> findByStudentIdAndPackageId(Long studentId, Long packageId);

    Optional<Enrollment> findByIdAndOrgId(Long id, Long orgId);
}
