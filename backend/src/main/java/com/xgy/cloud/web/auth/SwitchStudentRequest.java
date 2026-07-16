package com.xgy.cloud.web.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SwitchStudentRequest {
    @NotNull(message = "学员ID不能为空")
    private Long studentId;
}
