package com.xgy.cloud.web.whitelist;

import com.xgy.cloud.common.ApiResponse;
import com.xgy.cloud.security.UserPrincipal;
import com.xgy.cloud.service.WhitelistService;
import com.xgy.cloud.service.WhitelistService.WhitelistAddRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/whitelist")
@RequiredArgsConstructor
public class WhitelistController {

    private final WhitelistService whitelistService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(whitelistService.list(principal));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> add(@AuthenticationPrincipal UserPrincipal principal,
                                                @RequestBody WhitelistAddRequest request) {
        return ApiResponse.ok(whitelistService.add(principal, request));
    }
}
