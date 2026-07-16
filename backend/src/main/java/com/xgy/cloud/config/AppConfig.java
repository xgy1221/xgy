package com.xgy.cloud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class AppConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    @ConfigurationProperties(prefix = "app.sms")
    public SmsProperties smsProperties() {
        return new SmsProperties();
    }

    @lombok.Data
    public static class SmsProperties {
        /** 演示验证码；生产请接入真实短信网关并关闭固定码 */
        private String demoCode = "123456";
    }
}
