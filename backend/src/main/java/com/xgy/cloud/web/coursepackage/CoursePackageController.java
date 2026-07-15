package com.xgy.cloud.web.coursepackage;

import com.xgy.cloud.common.ApiResponse;
import com.xgy.cloud.security.UserPrincipal;
import com.xgy.cloud.service.CoursePackageService;
import com.xgy.cloud.service.CoursePackageService.PackageSaveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class CoursePackageController {

    private final CoursePackageService coursePackageService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(coursePackageService.list(principal));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@AuthenticationPrincipal UserPrincipal principal,
                                                   @RequestBody PackageSaveRequest request) {
        return ApiResponse.ok(coursePackageService.create(principal, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long id,
                                                   @RequestBody PackageSaveRequest request) {
        return ApiResponse.ok(coursePackageService.update(principal, id, request));
    }
}
