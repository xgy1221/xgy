package com.xgy.cloud.repository;

import com.xgy.cloud.domain.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByOrgIdAndLessonDateOrderByStartTimeAsc(Long orgId, LocalDate lessonDate);

    List<Lesson> findByOrgIdAndLessonDateBetweenOrderByLessonDateAscStartTimeAsc(
            Long orgId, LocalDate from, LocalDate to);

    Optional<Lesson> findByIdAndOrgId(Long id, Long orgId);

    List<Lesson> findByOrgIdAndPackageIdOrderByLessonDateAscStartTimeAsc(Long orgId, Long packageId);

    List<Lesson> findByOrgIdAndPackageIdAndLessonDateBetweenOrderByLessonDateAscStartTimeAsc(
            Long orgId, Long packageId, LocalDate from, LocalDate to);
}
