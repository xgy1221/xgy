package com.xgy.cloud.web.lesson;

import com.xgy.cloud.common.ApiResponse;
import com.xgy.cloud.security.UserPrincipal;
import com.xgy.cloud.service.LessonService;
import com.xgy.cloud.service.LessonService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from != null || to != null) {
            return ApiResponse.ok(lessonService.listByRange(principal, from, to));
        }
        return ApiResponse.ok(lessonService.listByDate(principal, date));
    }

    @GetMapping("/student-package")
    public ApiResponse<List<Map<String, Object>>> studentPackage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long studentId,
            @RequestParam Long packageId) {
        return ApiResponse.ok(lessonService.listStudentPackage(principal, studentId, packageId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long id) {
        return ApiResponse.ok(lessonService.detail(principal, id));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@AuthenticationPrincipal UserPrincipal principal,
                                                   @RequestBody LessonCreateRequest request) {
        return ApiResponse.ok(lessonService.create(principal, request));
    }

    @PostMapping("/{id}/finish")
    public ApiResponse<Map<String, Object>> finish(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long id) {
        return ApiResponse.ok(lessonService.finish(principal, id));
    }

    @PostMapping("/{id}/absent")
    public ApiResponse<Map<String, Object>> markAbsent(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable Long id,
                                                       @RequestBody AbsentRequest request) {
        return ApiResponse.ok(lessonService.markAbsent(principal, id, request));
    }

    @PostMapping("/{id}/makeup")
    public ApiResponse<Map<String, Object>> makeup(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long id,
                                                   @RequestBody MakeupRequest request) {
        return ApiResponse.ok(lessonService.makeup(principal, id, request));
    }

    @PostMapping("/{id}/rate-by-teacher")
    public ApiResponse<Map<String, Object>> rateByTeacher(@AuthenticationPrincipal UserPrincipal principal,
                                                          @PathVariable Long id,
                                                          @RequestBody TeacherRateRequest request) {
        return ApiResponse.ok(lessonService.rateByTeacher(principal, id, request));
    }

    @PostMapping("/{id}/rate-by-student")
    public ApiResponse<Map<String, Object>> rateByStudent(@AuthenticationPrincipal UserPrincipal principal,
                                                          @PathVariable Long id,
                                                          @RequestBody StudentRateRequest request) {
        return ApiResponse.ok(lessonService.rateByStudent(principal, id, request));
    }
}
