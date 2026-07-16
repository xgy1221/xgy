package com.xgy.cloud.repository;

import com.xgy.cloud.domain.ActivitySignup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivitySignupRepository extends JpaRepository<ActivitySignup, Long> {

    Optional<ActivitySignup> findByIdAndOrgId(Long id, Long orgId);

    List<ActivitySignup> findByOrgIdAndParentPhoneAndStatusOrderByIdDesc(
            Long orgId, String parentPhone, String status);

    List<ActivitySignup> findByOrgIdAndStudentIdAndStatusOrderByIdDesc(
            Long orgId, Long studentId, String status);

    Optional<ActivitySignup> findByActivityIdAndStudentIdAndStatus(
            Long activityId, Long studentId, String status);

    long countByActivityIdAndStatus(Long activityId, String status);

    List<ActivitySignup> findByActivityIdAndStatus(Long activityId, String status);
}
