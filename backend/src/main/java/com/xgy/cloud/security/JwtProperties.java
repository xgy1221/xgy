package com.xgy.cloud.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;
    /** JWT 有效天数，默认 7 */
    private int expireDays = 7;
}
