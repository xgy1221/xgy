package com.xgy.cloud.web.clazz;

import com.xgy.cloud.common.ApiResponse;
import com.xgy.cloud.security.UserPrincipal;
import com.xgy.cloud.service.ClassService;
import com.xgy.cloud.service.ClassService.ClassCreateRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(classService.list(principal));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@AuthenticationPrincipal UserPrincipal principal,
                                                   @RequestBody ClassCreateRequest request) {
        return ApiResponse.ok(classService.create(principal, request));
    }

    @PostMapping("/{id}/students")
    public ApiResponse<Map<String, Object>> addStudent(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable Long id,
                                                       @RequestBody AddStudentRequest request) {
        return ApiResponse.ok(classService.addStudent(principal, id, request.getStudentId()));
    }

    @Data
    public static class AddStudentRequest {
        private Long studentId;
    }
}
