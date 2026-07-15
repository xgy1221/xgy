package com.xgy.cloud.web.finance;

import com.xgy.cloud.common.ApiResponse;
import com.xgy.cloud.security.UserPrincipal;
import com.xgy.cloud.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(financeService.summary(principal));
    }

    @GetMapping("/orders")
    public ApiResponse<List<Map<String, Object>>> orders(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(financeService.orders(principal));
    }
}
