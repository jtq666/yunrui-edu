package com.yunrui.edu.common.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 使用 Redis ZSet 实现按用户维度的滑动窗口限流。
 * 清理旧请求、统计窗口内请求、写入当前请求在同一个 Lua 脚本中完成。
 */
@Component
public class RedisSlidingWindowRateLimiter {
    private static final DefaultRedisScript<Long> ALLOW = new DefaultRedisScript<>("""
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, now - window)
            local count = redis.call('ZCARD', KEYS[1])
            if count >= limit then
                return 0
            end
            redis.call('ZADD', KEYS[1], now, ARGV[4])
            redis.call('EXPIRE', KEYS[1], math.ceil(window / 1000))
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;

    public RedisSlidingWindowRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean allow(String key, int limit, Duration window) {
        long now = System.currentTimeMillis();
        long windowMillis = window.toMillis();
        Long result = redis.execute(
                ALLOW,
                List.of(key),
                Long.toString(now),
                Long.toString(windowMillis),
                Integer.toString(limit),
                UUID.randomUUID().toString());
        return result != null && result == 1L;
    }
}
