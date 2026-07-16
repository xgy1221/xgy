package com.xgy.cloud.service;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.common.RoleType;
import com.xgy.cloud.domain.ClassStudent;
import com.xgy.cloud.domain.Clazz;
import com.xgy.cloud.domain.Student;
import com.xgy.cloud.domain.Teacher;
import com.xgy.cloud.repository.ClassStudentRepository;
import com.xgy.cloud.repository.ClazzRepository;
import com.xgy.cloud.repository.CoursePackageRepository;
import com.xgy.cloud.repository.StudentRepository;
import com.xgy.cloud.repository.TeacherRepository;
import com.xgy.cloud.security.SecurityUtils;
import com.xgy.cloud.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClazzRepository clazzRepository;
    private final ClassStudentRepository classStudentRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final CoursePackageRepository coursePackageRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UserPrincipal principal, Boolean mine) {
        Long orgId = SecurityUtils.requireOrgId();
        List<Clazz> classes = clazzRepository.findByOrgIdOrderByIdDesc(orgId);
        if (Boolean.TRUE.equals(mine) || RoleType.TEACHER.name().equalsIgnoreCase(principal.getCurrentRole())) {
            Optional<Teacher> teacher = teacherRepository.findByOrgIdAndUserId(orgId, principal.getUserId());
            if (teacher.isPresent()) {
                Long teacherId = teacher.get().getId();
                classes = classes.stream()
                        .filter(c -> teacherId.equals(c.getTeacherId()))
                        .collect(Collectors.toList());
            } else if (Boolean.TRUE.equals(mine)) {
                classes = List.of();
            }
        }
        return classes.stream().map(this::toView).collect(Collectors.toList());
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

    @Transactional
    public Map<String, Object> update(UserPrincipal principal, Long id, ClassCreateRequest req) {
        if (SecurityUtils.isParent()) {
            throw new BizException(403, "家长无权操作");
        }
        Long orgId = SecurityUtils.requireOrgId();
        Clazz c = clazzRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new BizException("班级不存在"));
        if (StringUtils.hasText(req.getName())) {
            c.setName(req.getName());
        }
        if (req.getPackageId() != null) {
            c.setPackageId(req.getPackageId());
        }
        if (req.getTeacherId() != null) {
            c.setTeacherId(req.getTeacherId());
        }
        if (req.getCampus() != null) {
            c.setCampus(req.getCampus());
        }
        if (req.getRoom() != null) {
            c.setRoom(req.getRoom());
        }
        if (StringUtils.hasText(req.getStatus())) {
            c.setStatus(req.getStatus());
        }
        clazzRepository.save(c);
        return toView(c);
    }

    @Transactional
    public Map<String, Object> removeStudent(UserPrincipal principal, Long classId, Long studentId) {
        if (SecurityUtils.isParent()) {
            throw new BizException(403, "家长无权操作");
        }
        Long orgId = SecurityUtils.requireOrgId();
        Clazz c = clazzRepository.findByIdAndOrgId(classId, orgId)
                .orElseThrow(() -> new BizException("班级不存在"));
        ClassStudent cs = classStudentRepository.findByClassIdAndStudentId(c.getId(), studentId)
                .orElseThrow(() -> new BizException("学员不在班中"));
        classStudentRepository.delete(cs);
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
        if (c.getPackageId() != null) {
            coursePackageRepository.findById(c.getPackageId()).ifPresent(p -> {
                m.put("packageName", p.getName());
                m.put("lessonCount", p.getLessonCount());
                m.put("packagePrice", p.getPrice());
            });
        }
        if (c.getTeacherId() != null) {
            teacherRepository.findById(c.getTeacherId()).ifPresent(t -> m.put("teacherName", t.getName()));
        }
        List<Long> studentIds = classStudentRepository.findByClassId(c.getId()).stream()
                .map(ClassStudent::getStudentId).collect(Collectors.toList());
        m.put("studentIds", studentIds);
        m.put("studentCount", studentIds.size());
        List<Map<String, Object>> students = studentIds.stream().map(sid -> {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("id", sid);
            studentRepository.findById(sid).ifPresent(s -> {
                sm.put("studentName", s.getStudentName());
                sm.put("parentPhone", s.getParentPhone());
                sm.put("campus", s.getCampus());
            });
            return sm;
        }).collect(Collectors.toList());
        m.put("students", students);
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
