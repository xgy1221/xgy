package com.xgy.cloud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /** 新手机号是否必须先在教务白名单才能注册家长 */
    private boolean requireWhitelistForNewParent = true;

    /** 登录：每手机号窗口内最大次数 */
    private int loginMaxPerPhone = 8;

    /** 登录：每 IP 窗口内最大次数 */
    private int loginMaxPerIp = 30;

    private int loginWindowSeconds = 60;

    /** CORS 允许源；空则默认本机开发地址 */
    private List<String> corsAllowedOrigins = new ArrayList<>(List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:4173",
            "http://127.0.0.1:4173"
    ));
}
