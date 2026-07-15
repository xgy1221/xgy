package com.xgy.cloud.service;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.domain.*;
import com.xgy.cloud.repository.*;
import com.xgy.cloud.security.SecurityUtils;
import com.xgy.cloud.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final LessonAttendeeRepository lessonAttendeeRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClazzRepository clazzRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByDate(UserPrincipal principal, LocalDate date) {
        Long orgId = resolveOrgId(principal);
        LocalDate d = date != null ? date : LocalDate.now();
        List<Lesson> lessons = lessonRepository.findByOrgIdAndLessonDateOrderByStartTimeAsc(orgId, d);

        if (SecurityUtils.isParent()) {
            Long studentId = principal.getCurrentStudentId();
            if (studentId == null) {
                return List.of();
            }
            Set<Long> lessonIds = lessonAttendeeRepository.findByStudentId(studentId).stream()
                    .map(LessonAttendee::getLessonId)
                    .collect(Collectors.toSet());
            lessons = lessons.stream().filter(l -> lessonIds.contains(l.getId())).collect(Collectors.toList());
        }

        return lessons.stream().map(this::toView).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(UserPrincipal principal, LessonCreateRequest req) {
        if (SecurityUtils.isParent()) {
            throw new BizException(403, "家长无权排课");
        }
        Long orgId = SecurityUtils.requireOrgId();
        Clazz clazz = clazzRepository.findByIdAndOrgId(req.getClassId(), orgId)
                .orElseThrow(() -> new BizException("班级不存在"));

        Lesson lesson = new Lesson();
        lesson.setOrgId(orgId);
        lesson.setClassId(clazz.getId());
        lesson.setTeacherId(req.getTeacherId() != null ? req.getTeacherId() : clazz.getTeacherId());
        lesson.setLessonDate(req.getLessonDate());
        lesson.setStartTime(req.getStartTime());
        lesson.setEndTime(req.getEndTime());
        lesson.setRoom(StringUtils.hasText(req.getRoom()) ? req.getRoom() : clazz.getRoom());
        lesson.setStatus("SCHEDULED");
        lessonRepository.save(lesson);

        List<ClassStudent> members = classStudentRepository.findByClassId(clazz.getId());
        for (ClassStudent cs : members) {
            LessonAttendee att = new LessonAttendee();
            att.setLessonId(lesson.getId());
            att.setStudentId(cs.getStudentId());
            att.setType("REGULAR");
            att.setHomeClassId(clazz.getId());
            enrollmentRepository.findByOrgIdAndStudentId(orgId, cs.getStudentId()).stream()
                    .filter(e -> clazz.getPackageId() != null && clazz.getPackageId().equals(e.getPackageId()))
                    .findFirst()
                    .ifPresent(e -> att.setEnrollmentId(e.getId()));
            att.setConsumed(false);
            att.setAbsent(false);
            lessonAttendeeRepository.save(att);
        }
        return toView(lesson);
    }

    @Transactional
    public Map<String, Object> makeup(UserPrincipal principal, Long lessonId, MakeupRequest req) {
        if (SecurityUtils.isParent()) {
            throw new BizException(403, "家长无权临补");
        }
        Long orgId = SecurityUtils.requireOrgId();
        Lesson lesson = lessonRepository.findByIdAndOrgId(lessonId, orgId)
                .orElseThrow(() -> new BizException("课次不存在"));
        Student student = studentRepository.findByIdAndOrgId(req.getStudentId(), orgId)
                .orElseThrow(() -> new BizException("学员不存在"));
        lessonAttendeeRepository.findByLessonIdAndStudentId(lessonId, student.getId())
                .ifPresent(a -> {
                    throw new BizException("学员已在本节课");
                });

        LessonAttendee att = new LessonAttendee();
        att.setLessonId(lesson.getId());
        att.setStudentId(student.getId());
        att.setType("MAKEUP");
        att.setHomeClassId(req.getHomeClassId());
        if (req.getEnrollmentId() != null) {
            att.setEnrollmentId(req.getEnrollmentId());
        } else {
            enrollmentRepository.findByOrgIdAndStudentId(orgId, student.getId()).stream()
                    .findFirst()
                    .ifPresent(e -> att.setEnrollmentId(e.getId()));
        }
        att.setConsumed(false);
        att.setAbsent(false);
        lessonAttendeeRepository.save(att);
        return toView(lesson);
    }

    @Transactional
    public Map<String, Object> rateByTeacher(UserPrincipal principal, Long lessonId, TeacherRateRequest req) {
        Long orgId = SecurityUtils.requireOrgId();
        Lesson lesson = lessonRepository.findByIdAndOrgId(lessonId, orgId)
                .orElseThrow(() -> new BizException("课次不存在"));
        LessonAttendee att = lessonAttendeeRepository.findByLessonIdAndStudentId(lessonId, req.getStudentId())
                .orElseThrow(() -> new BizException("考勤记录不存在"));

        att.setTeacherRating(req.getRating());
        att.setTeacherComment(req.getComment());
        if (Boolean.TRUE.equals(req.getAbsent())) {
            att.setAbsent(true);
        } else {
            att.setAbsent(false);
            if (!Boolean.TRUE.equals(att.getConsumed())) {
                consumeEnrollment(att);
                att.setConsumed(true);
            }
        }
        lessonAttendeeRepository.save(att);
        lesson.setStatus("FINISHED");
        lessonRepository.save(lesson);
        return toView(lesson);
    }

    @Transactional
    public Map<String, Object> rateByStudent(UserPrincipal principal, Long lessonId, StudentRateRequest req) {
        Long studentId = req.getStudentId() != null ? req.getStudentId() : principal.getCurrentStudentId();
        if (studentId == null) {
            throw new BizException("请指定学员");
        }
        if (SecurityUtils.isParent() && !Objects.equals(principal.getCurrentStudentId(), studentId)) {
            Student stu = studentRepository.findById(studentId)
                    .orElseThrow(() -> new BizException("学员不存在"));
            if (!principal.getPhone().equals(stu.getParentPhone())) {
                throw new BizException(403, "无权评价");
            }
        }
        LessonAttendee att = lessonAttendeeRepository.findByLessonIdAndStudentId(lessonId, studentId)
                .orElseThrow(() -> new BizException("考勤记录不存在"));
        att.setStudentRating(req.getRating());
        att.setStudentComment(req.getComment());
        lessonAttendeeRepository.save(att);

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BizException("课次不存在"));
        return toView(lesson);
    }

    private void consumeEnrollment(LessonAttendee att) {
        if (att.getEnrollmentId() == null) {
            return;
        }
        enrollmentRepository.findById(att.getEnrollmentId()).ifPresent(enroll -> {
            int remain = Math.max(0, enroll.getRemainLessons() - 1);
            enroll.setRemainLessons(remain);
            if (remain == 0) {
                enroll.setStatus("FINISHED");
            }
            enrollmentRepository.save(enroll);
        });
    }

    private Long resolveOrgId(UserPrincipal principal) {
        if (principal.getOrgId() != null) {
            return principal.getOrgId();
        }
        throw new BizException("缺少机构上下文");
    }

    private Map<String, Object> toView(Lesson lesson) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", lesson.getId());
        m.put("orgId", lesson.getOrgId());
        m.put("classId", lesson.getClassId());
        m.put("teacherId", lesson.getTeacherId());
        m.put("lessonDate", lesson.getLessonDate());
        m.put("startTime", lesson.getStartTime());
        m.put("endTime", lesson.getEndTime());
        m.put("room", lesson.getRoom());
        m.put("status", lesson.getStatus());
        clazzRepository.findById(lesson.getClassId()).ifPresent(c -> m.put("className", c.getName()));
        if (lesson.getTeacherId() != null) {
            teacherRepository.findById(lesson.getTeacherId()).ifPresent(t -> m.put("teacherName", t.getName()));
        }
        List<Map<String, Object>> attendees = lessonAttendeeRepository.findByLessonId(lesson.getId()).stream()
                .map(a -> {
                    Map<String, Object> am = new LinkedHashMap<>();
                    am.put("id", a.getId());
                    am.put("studentId", a.getStudentId());
                    am.put("type", a.getType());
                    am.put("homeClassId", a.getHomeClassId());
                    am.put("enrollmentId", a.getEnrollmentId());
                    am.put("teacherRating", a.getTeacherRating());
                    am.put("teacherComment", a.getTeacherComment());
                    am.put("studentRating", a.getStudentRating());
                    am.put("studentComment", a.getStudentComment());
                    am.put("consumed", a.getConsumed());
                    am.put("absent", a.getAbsent());
                    studentRepository.findById(a.getStudentId())
                            .ifPresent(s -> am.put("studentName", s.getStudentName()));
                    return am;
                }).collect(Collectors.toList());
        m.put("attendees", attendees);
        return m;
    }

    @Data
    public static class LessonCreateRequest {
        private Long classId;
        private Long teacherId;
        private LocalDate lessonDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String room;
    }

    @Data
    public static class MakeupRequest {
        private Long studentId;
        private Long homeClassId;
        private Long enrollmentId;
    }

    @Data
    public static class TeacherRateRequest {
        private Long studentId;
        private Integer rating;
        private String comment;
        private Boolean absent;
    }

    @Data
    public static class StudentRateRequest {
        private Long studentId;
        private Integer rating;
        private String comment;
    }
}
