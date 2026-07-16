package com.xgy.cloud.web.student;

import com.xgy.cloud.common.ApiResponse;
import com.xgy.cloud.security.UserPrincipal;
import com.xgy.cloud.service.StudentService;
import com.xgy.cloud.service.StudentService.StudentSaveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@AuthenticationPrincipal UserPrincipal principal,
                                                       @RequestParam(required = false) String phone) {
        return ApiResponse.ok(studentService.list(principal, phone));
    }

    @GetMapping("/by-phone")
    public ApiResponse<List<Map<String, Object>>> byPhone(@AuthenticationPrincipal UserPrincipal principal,
                                                          @RequestParam String phone) {
        return ApiResponse.ok(studentService.byPhone(principal, phone));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@AuthenticationPrincipal UserPrincipal principal,
                                                   @RequestBody StudentSaveRequest request) {
        return ApiResponse.ok(studentService.create(principal, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long id,
                                                   @RequestBody StudentSaveRequest request) {
        return ApiResponse.ok(studentService.update(principal, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> archive(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable Long id) {
        return ApiResponse.ok(studentService.archive(principal, id));
    }
}
