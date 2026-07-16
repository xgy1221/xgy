package com.xgy.cloud.repository;

import com.xgy.cloud.domain.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    Optional<Activity> findByIdAndOrgId(Long id, Long orgId);

    List<Activity> findByOrgIdAndPublishedTrueAndStatusOrderByStartDateAscIdAsc(
            Long orgId, String status);

    List<Activity> findByOrgIdAndPublishedTrueAndStatusAndStartDateGreaterThanEqualOrderByStartDateAscIdAsc(
            Long orgId, String status, LocalDate from);

    List<Activity> findByOrgIdAndPublishedTrueAndStatusAndStartDateLessThanOrderByStartDateDescIdDesc(
            Long orgId, String status, LocalDate before);
}
