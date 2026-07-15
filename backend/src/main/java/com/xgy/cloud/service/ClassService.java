package com.xgy.cloud.service;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.domain.ClassStudent;
import com.xgy.cloud.domain.Clazz;
import com.xgy.cloud.domain.Student;
import com.xgy.cloud.repository.ClassStudentRepository;
import com.xgy.cloud.repository.ClazzRepository;
import com.xgy.cloud.repository.StudentRepository;
import com.xgy.cloud.repository.TeacherRepository;
import com.xgy.cloud.security.SecurityUtils;
import com.xgy.cloud.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClazzRepository clazzRepository;
    private final ClassStudentRepository classStudentRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UserPrincipal principal) {
        Long orgId = SecurityUtils.requireOrgId();
        return clazzRepository.findByOrgIdOrderByIdDesc(orgId).stream().map(this::toView).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(UserPrincipal principal, ClassCreateRequest req) {
        if (SecurityUtils.isParent()) {
            throw new BizException(403, "家长无权操作");
        }
        Long orgId = SecurityUtils.requireOrgId();
        Clazz c = new Clazz();
        c.setOrgId(orgId);
        c.setName(req.getName());
        c.setPackageId(req.getPackageId());
        c.setTeacherId(req.getTeacherId());
        c.setCampus(req.getCampus());
        c.setRoom(req.getRoom());
        c.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus() : "ACTIVE");
        clazzRepository.save(c);
        return toView(c);
    }

    @Transactional
    public Map<String, Object> addStudent(UserPrincipal principal, Long classId, Long studentId) {
        if (SecurityUtils.isParent()) {
            throw new BizException(403, "家长无权操作");
        }
        Long orgId = SecurityUtils.requireOrgId();
        Clazz c = clazzRepository.findByIdAndOrgId(classId, orgId)
                .orElseThrow(() -> new BizException("班级不存在"));
        Student student = studentRepository.findByIdAndOrgId(studentId, orgId)
                .orElseThrow(() -> new BizException("学员不存在"));
        classStudentRepository.findByClassIdAndStudentId(c.getId(), student.getId())
                .ifPresent(cs -> {
                    throw new BizException("学员已在班中");
                });
        ClassStudent cs = new ClassStudent();
        cs.setClassId(c.getId());
        cs.setStudentId(student.getId());
        classStudentRepository.save(cs);
        return toView(c);
    }

    private Map<String, Object> toView(Clazz c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("orgId", c.getOrgId());
        m.put("name", c.getName());
        m.put("packageId", c.getPackageId());
        m.put("teacherId", c.getTeacherId());
        m.put("campus", c.getCampus());
        m.put("room", c.getRoom());
        m.put("status", c.getStatus());
        if (c.getTeacherId() != null) {
            teacherRepository.findById(c.getTeacherId()).ifPresent(t -> m.put("teacherName", t.getName()));
        }
        List<Long> studentIds = classStudentRepository.findByClassId(c.getId()).stream()
                .map(ClassStudent::getStudentId).collect(Collectors.toList());
        m.put("studentIds", studentIds);
        m.put("studentCount", studentIds.size());
        return m;
    }

    @Data
    public static class ClassCreateRequest {
        private String name;
        private Long packageId;
        private Long teacherId;
        private String campus;
        private String room;
        private String status;
    }
}
