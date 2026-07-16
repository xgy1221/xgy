package com.xgy.cloud.web;

import com.xgy.cloud.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/api/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.ok(Map.of("ok", true, "service", "xueguan-yun"));
    }
}
