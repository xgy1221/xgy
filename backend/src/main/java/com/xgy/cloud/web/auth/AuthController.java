package com.xgy.cloud.web.auth;

import com.xgy.cloud.common.ApiResponse;
import com.xgy.cloud.security.UserPrincipal;
import com.xgy.cloud.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(authService.me(principal));
    }

    @PostMapping("/switch-role")
    public ApiResponse<Map<String, Object>> switchRole(@AuthenticationPrincipal UserPrincipal principal,
                                                       @Valid @RequestBody SwitchRoleRequest request) {
        return ApiResponse.ok(authService.switchRole(principal, request));
    }

    @PostMapping("/switch-student")
    public ApiResponse<Map<String, Object>> switchStudent(@AuthenticationPrincipal UserPrincipal principal,
                                                          @Valid @RequestBody SwitchStudentRequest request) {
        return ApiResponse.ok(authService.switchStudent(principal, request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal);
        return ApiResponse.ok();
    }
}
