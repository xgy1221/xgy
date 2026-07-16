package com.xgy.cloud.web.org;

import com.xgy.cloud.common.ApiResponse;
import com.xgy.cloud.security.UserPrincipal;
import com.xgy.cloud.service.OrgService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orgs")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(orgService.list(principal));
    }
}
