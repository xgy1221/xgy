package com.xgy.cloud.service;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.domain.Activity;
import com.xgy.cloud.domain.ActivitySignup;
import com.xgy.cloud.domain.Student;
import com.xgy.cloud.repository.ActivityRepository;
import com.xgy.cloud.repository.ActivitySignupRepository;
import com.xgy.cloud.repository.StudentRepository;
import com.xgy.cloud.security.SecurityUtils;
import com.xgy.cloud.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivitySignupRepository activitySignupRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UserPrincipal principal, String tab, Long studentId) {
        Long orgId = resolveOrgId(principal, studentId);
        String t = StringUtils.hasText(tab) ? tab.trim().toLowerCase() : "open";
        LocalDate today = LocalDate.now();

        if ("mine".equals(t)) {
            return listMine(principal, orgId, studentId);
        }

        List<Activity> activities;
        if ("past".equals(t)) {
            activities = activityRepository
                    .findByOrgIdAndPublishedTrueAndStatusAndStartDateLessThanOrderByStartDateDescIdDesc(
                            orgId, "ACTIVE", today);
        } else {
            activities = activityRepository
                    .findByOrgIdAndPublishedTrueAndStatusAndStartDateGreaterThanEqualOrderByStartDateAscIdAsc(
                            orgId, "ACTIVE", today);
        }
        return activities.stream().map(a -> toListView(a, principal)).collect(Collectors.toList());
    }

    private List<Map<String, Object>> listMine(UserPrincipal principal, Long orgId, Long studentId) {
        List<ActivitySignup> signups;
        if (SecurityUtils.isParent()) {
            if (studentId != null) {
                Student student = studentRepository.findById(studentId)
                        .orElseThrow(() -> new BizException("学员不存在"));
                assertParentOwnsStudent(principal, student);
                if (!orgId.equals(student.getOrgId())) {
                    throw new BizException(403, "学员不属于当前机构");
                }
                signups = activitySignupRepository.findByOrgIdAndStudentIdAndStatusOrderByIdDesc(
                        orgId, studentId, "CONFIRMED");
            } else {
                signups = activitySignupRepository.findByOrgIdAndParentPhoneAndStatusOrderByIdDesc(
                        orgId, principal.getPhone(), "CONFIRMED");
            }
        } else {
            if (studentId == null) {
                throw new BizException("请指定学员 studentId");
            }
            Student student = studentRepository.findByIdAndOrgId(studentId, orgId)
                    .orElseThrow(() -> new BizException("学员不存在"));
            signups = activitySignupRepository.findByOrgIdAndStudentIdAndStatusOrderByIdDesc(
                    orgId, student.getId(), "CONFIRMED");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (ActivitySignup signup : signups) {
            activityRepository.findById(signup.getActivityId()).ifPresent(activity -> {
                Map<String, Object> m = toListView(activity, principal);
                m.put("signupId", signup.getId());
                m.put("signupStatus", signup.getStatus());
                m.put("studentId", signup.getStudentId());
                m.put("studentName", signup.getStudentName());
                result.add(m);
            });
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(UserPrincipal principal, Long id) {
        Long orgId = resolveOrgId(principal, null);
        Activity activity = activityRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new BizException("活动不存在"));
        Map<String, Object> m = toDetailView(activity, principal);
        long confirmed = activitySignupRepository.countByActivityIdAndStatus(activity.getId(), "CONFIRMED");
        m.put("signupCount", confirmed);
        m.put("remainSlots", Math.max(0, activity.getCapacity() - (int) confirmed));
        return m;
    }

    @Transactional
    public Map<String, Object> signup(UserPrincipal principal, Long activityId, SignupRequest req) {
        if (req == null || req.getStudentId() == null) {
            throw new BizException("请指定学员 studentId");
        }
        Student student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new BizException("学员不存在"));

        if (SecurityUtils.isParent()) {
            assertParentOwnsStudent(principal, student);
        } else {
            Long orgId = SecurityUtils.requireOrgId();
            if (!orgId.equals(student.getOrgId())) {
                throw new BizException(403, "学员不属于当前机构");
            }
        }

        Activity activity = activityRepository.findByIdAndOrgId(activityId, student.getOrgId())
                .orElseThrow(() -> new BizException("活动不存在"));
        if (!Boolean.TRUE.equals(activity.getPublished()) || !"ACTIVE".equals(activity.getStatus())) {
            throw new BizException("活动未开放报名");
        }
        LocalDate today = LocalDate.now();
        if (activity.getEnrollDeadline() != null && today.isAfter(activity.getEnrollDeadline())) {
            throw new BizException("已过报名截止日");
        }
        activitySignupRepository.findByActivityIdAndStudentIdAndStatus(activityId, student.getId(), "CONFIRMED")
                .ifPresent(s -> {
                    throw new BizException("该学员已报名");
                });
        long confirmed = activitySignupRepository.countByActivityIdAndStatus(activityId, "CONFIRMED");
        if (activity.getCapacity() != null && activity.getCapacity() > 0 && confirmed >= activity.getCapacity()) {
            throw new BizException("名额已满");
        }

        ActivitySignup signup = new ActivitySignup();
        signup.setOrgId(activity.getOrgId());
        signup.setActivityId(activity.getId());
        signup.setStudentId(student.getId());
        signup.setStudentName(student.getStudentName());
        signup.setParentPhone(student.getParentPhone());
        signup.setStatus("CONFIRMED");
        activitySignupRepository.save(signup);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", signup.getId());
        m.put("activityId", signup.getActivityId());
        m.put("studentId", signup.getStudentId());
        m.put("studentName", signup.getStudentName());
        m.put("status", signup.getStatus());
        return m;
    }

    @Transactional
    public Map<String, Object> cancel(UserPrincipal principal, Long signupId) {
        ActivitySignup signup = activitySignupRepository.findById(signupId)
                .orElseThrow(() -> new BizException("报名记录不存在"));

        if (SecurityUtils.isParent()) {
            if (!principal.getPhone().equals(signup.getParentPhone())) {
                throw new BizException(403, "无权取消该报名");
            }
        } else {
            Long orgId = SecurityUtils.requireOrgId();
            if (!orgId.equals(signup.getOrgId())) {
                throw new BizException(403, "报名不属于当前机构");
            }
        }

        if ("CANCELLED".equals(signup.getStatus())) {
            throw new BizException("报名已取消");
        }
        signup.setStatus("CANCELLED");
        activitySignupRepository.save(signup);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", signup.getId());
        m.put("activityId", signup.getActivityId());
        m.put("studentId", signup.getStudentId());
        m.put("status", signup.getStatus());
        return m;
    }

    private Long resolveOrgId(UserPrincipal principal, Long studentId) {
        if (SecurityUtils.isParent()) {
            if (studentId != null) {
                Student student = studentRepository.findById(studentId)
                        .orElseThrow(() -> new BizException("学员不存在"));
                assertParentOwnsStudent(principal, student);
                return student.getOrgId();
            }
            if (principal.getOrgId() != null) {
                return principal.getOrgId();
            }
            if (principal.getCurrentStudentId() != null) {
                return studentRepository.findById(principal.getCurrentStudentId())
                        .map(Student::getOrgId)
                        .orElseThrow(() -> new BizException("缺少机构上下文"));
            }
            throw new BizException("缺少机构上下文，请先切换学员");
        }
        return SecurityUtils.requireOrgId();
    }

    private void assertParentOwnsStudent(UserPrincipal principal, Student student) {
        if (!principal.getPhone().equals(student.getParentPhone())) {
            throw new BizException(403, "无权操作该学员");
        }
    }

    private Map<String, Object> toListView(Activity a, UserPrincipal principal) {
        Map<String, Object> m = baseView(a);
        long confirmed = activitySignupRepository.countByActivityIdAndStatus(a.getId(), "CONFIRMED");
        m.put("signupCount", confirmed);
        m.put("remainSlots", Math.max(0, a.getCapacity() - (int) confirmed));
        m.put("enrolled", isEnrolled(a, principal));
        return m;
    }

    private Map<String, Object> toDetailView(Activity a, UserPrincipal principal) {
        Map<String, Object> m = baseView(a);
        m.put("summary", a.getSummary());
        m.put("highlights", a.getHighlights());
        m.put("gallery", a.getGallery());
        m.put("recap", a.getRecap());
        m.put("address", a.getAddress());
        m.put("enrolled", isEnrolled(a, principal));
        return m;
    }

    private boolean isEnrolled(Activity a, UserPrincipal principal) {
        if (!SecurityUtils.isParent()) {
            return false;
        }
        Long studentId = principal.getCurrentStudentId();
        if (studentId == null) {
            return activitySignupRepository.findByOrgIdAndParentPhoneAndStatusOrderByIdDesc(
                            a.getOrgId(), principal.getPhone(), "CONFIRMED").stream()
                    .anyMatch(s -> Objects.equals(s.getActivityId(), a.getId()));
        }
        return activitySignupRepository
                .findByActivityIdAndStudentIdAndStatus(a.getId(), studentId, "CONFIRMED")
                .isPresent();
    }

    private Map<String, Object> baseView(Activity a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("orgId", a.getOrgId());
        m.put("title", a.getTitle());
        m.put("category", a.getCategory());
        m.put("coverTone", a.getCoverTone());
        m.put("campus", a.getCampus());
        m.put("startDate", a.getStartDate());
        m.put("startTime", a.getStartTime());
        m.put("endTime", a.getEndTime());
        m.put("enrollDeadline", a.getEnrollDeadline());
        m.put("capacity", a.getCapacity());
        m.put("fee", a.getFee());
        m.put("targetGrade", a.getTargetGrade());
        m.put("published", a.getPublished());
        m.put("status", a.getStatus());
        return m;
    }

    @Data
    public static class SignupRequest {
        private Long studentId;
    }
}
