package com.xgy.cloud.repository;

import com.xgy.cloud.domain.CoursePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoursePackageRepository extends JpaRepository<CoursePackage, Long> {
    List<CoursePackage> findByOrgIdOrderByIdDesc(Long orgId);

    Optional<CoursePackage> findByIdAndOrgId(Long id, Long orgId);
}
