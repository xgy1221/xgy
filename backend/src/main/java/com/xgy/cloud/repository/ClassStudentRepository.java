package com.xgy.cloud.repository;

import com.xgy.cloud.domain.ClassStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassStudentRepository extends JpaRepository<ClassStudent, Long> {
    List<ClassStudent> findByClassId(Long classId);

    Optional<ClassStudent> findByClassIdAndStudentId(Long classId, Long studentId);

    List<ClassStudent> findByStudentId(Long studentId);
}
