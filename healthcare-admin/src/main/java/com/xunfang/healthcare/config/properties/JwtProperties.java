package com.xunfang.healthcare.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 相关配置，对应需求文档 12.3「会话超时时间」。
 */
@Data
@ConfigurationProperties(prefix = "healthcare.jwt")
public class JwtProperties {

    /**
     * 签名密钥，生产环境必须通过环境变量覆盖。
     */
    private String secret = "healthcare-admin-default-secret-key-please-override-in-production";

    /**
     * 令牌有效期（分钟），对应「会话超时时间」。
     */
    private Long expireMinutes = 30L;

    /**
     * 请求头名称。
     */
    private String header = "Authorization";

    /**
     * 令牌前缀。
     */
    private String tokenPrefix = "Bearer ";
}
