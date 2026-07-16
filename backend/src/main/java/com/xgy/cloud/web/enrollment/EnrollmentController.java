package com.xgy.cloud.web.enrollment;

import com.xgy.cloud.common.ApiResponse;
import com.xgy.cloud.security.UserPrincipal;
import com.xgy.cloud.service.EnrollmentService;
import com.xgy.cloud.service.EnrollmentService.EnrollmentUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long studentId) {
        if (studentId != null) {
            return ApiResponse.ok(enrollmentService.listByStudent(principal, studentId));
        }
        return ApiResponse.ok(enrollmentService.listByOrg(principal));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> upsert(@AuthenticationPrincipal UserPrincipal principal,
                                                   @RequestBody EnrollmentUpsertRequest request) {
        return ApiResponse.ok(enrollmentService.upsert(principal, request));
    }
}
