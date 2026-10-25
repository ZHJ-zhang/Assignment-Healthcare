package com.xunfang.healthcare.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 令牌黑名单。登出后把令牌写入 Redis，令牌本身过期前都无法再次使用。
 * Redis 不可用时降级为放行，避免缓存故障导致所有鉴权接口异常。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklist {

    private static final String KEY_PREFIX = "healthcare:token:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 将令牌加入黑名单。
     *
     * @param token         令牌
     * @param expireSeconds 令牌剩余有效期（秒）
     */
    public void add(String token, long expireSeconds) {
        if (token == null || token.isBlank() || expireSeconds <= 0) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + token, "1", Duration.ofSeconds(expireSeconds));
        } catch (Exception e) {
            log.warn("Redis 不可用，令牌黑名单写入失败（登出后令牌仍可能在过期前有效）: {}", e.getMessage());
        }
    }

    /**
     * 判断令牌是否已失效。
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(KEY_PREFIX + token));
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过令牌黑名单校验: {}", e.getMessage());
            return false;
        }
    }
}
