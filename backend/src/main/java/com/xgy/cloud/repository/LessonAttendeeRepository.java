package com.xgy.cloud.repository;

import com.xgy.cloud.domain.LessonAttendee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonAttendeeRepository extends JpaRepository<LessonAttendee, Long> {
    List<LessonAttendee> findByLessonId(Long lessonId);

    Optional<LessonAttendee> findByLessonIdAndStudentId(Long lessonId, Long studentId);

    List<LessonAttendee> findByStudentId(Long studentId);
}
