package com.xgy.cloud.web.activity;

import com.xgy.cloud.common.ApiResponse;
import com.xgy.cloud.security.UserPrincipal;
import com.xgy.cloud.service.ActivityService;
import com.xgy.cloud.service.ActivityService.ActivitySaveRequest;
import com.xgy.cloud.service.ActivityService.SignupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "open") String tab,
            @RequestParam(required = false) Long studentId) {
        return ApiResponse.ok(activityService.list(principal, tab, studentId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long id) {
        return ApiResponse.ok(activityService.detail(principal, id));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@AuthenticationPrincipal UserPrincipal principal,
                                                   @RequestBody ActivitySaveRequest request) {
        return ApiResponse.ok(activityService.create(principal, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long id,
                                                   @RequestBody ActivitySaveRequest request) {
        return ApiResponse.ok(activityService.update(principal, id, request));
    }

    @PostMapping("/{id}/signup")
    public ApiResponse<Map<String, Object>> signup(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long id,
                                                   @RequestBody SignupRequest request) {
        return ApiResponse.ok(activityService.signup(principal, id, request));
    }

    @PostMapping("/signups/{signupId}/cancel")
    public ApiResponse<Map<String, Object>> cancel(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long signupId) {
        return ApiResponse.ok(activityService.cancel(principal, signupId));
    }
}
