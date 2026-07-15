package com.xgy.cloud.web.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SwitchRoleRequest {
    @NotBlank(message = "角色不能为空")
    private String role;

    private Long orgId;
}
