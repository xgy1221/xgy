package com.xgy.cloud.repository;

import com.xgy.cloud.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByOrgIdOrderByIdDesc(Long orgId);

    List<Student> findByParentPhoneOrderByOrgIdAscIdAsc(String parentPhone);

    List<Student> findByOrgIdAndParentPhone(Long orgId, String parentPhone);

    Optional<Student> findByOrgIdAndParentPhoneAndStudentName(Long orgId, String parentPhone, String studentName);

    Optional<Student> findByIdAndOrgId(Long id, Long orgId);
}
