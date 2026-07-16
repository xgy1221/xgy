package com.xgy.cloud.web.user;

import com.xgy.cloud.common.ApiResponse;
import com.xgy.cloud.security.UserPrincipal;
import com.xgy.cloud.service.UserAdminService;
import com.xgy.cloud.service.UserAdminService.GrantRoleRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserAdminService userAdminService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(userAdminService.list(principal));
    }

    @PostMapping("/grant-role")
    public ApiResponse<Map<String, Object>> grant(@AuthenticationPrincipal UserPrincipal principal,
                                                  @RequestBody GrantRoleRequest request) {
        return ApiResponse.ok(userAdminService.grantRole(principal, request));
    }

    @PostMapping("/{userId}/revoke-role")
    public ApiResponse<Map<String, Object>> revoke(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long userId,
                                                   @RequestBody RevokeRequest request) {
        return ApiResponse.ok(userAdminService.revokeRole(principal, userId, request.getRole()));
    }

    @Data
    public static class RevokeRequest {
        private String role;
    }
}
