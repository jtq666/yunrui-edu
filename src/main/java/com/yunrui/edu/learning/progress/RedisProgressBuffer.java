package com.yunrui.edu.learning.progress;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class RedisProgressBuffer implements ProgressBuffer {
    private static final Duration TTL = Duration.ofDays(7);
    private static final DefaultRedisScript<Long> ACK_IF_UNCHANGED = new DefaultRedisScript<>("""
            local current = redis.call('HGET', KEYS[1], ARGV[1])
            if current ~= ARGV[2] then
                return 0
            end
            redis.call('HDEL', KEYS[1], ARGV[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;

    public RedisProgressBuffer(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void save(ProgressSnapshot snapshot) {
        String key = key(snapshot.userId(), snapshot.courseId());
        redis.opsForHash().put(key, Long.toString(snapshot.sectionId()), snapshot.encode());
        redis.expire(key, TTL);
    }

    @Override
    public Optional<ProgressSnapshot> find(long userId, long courseId, long sectionId) {
        Object value = redis.opsForHash().get(key(userId, courseId), Long.toString(sectionId));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(ProgressSnapshot.decode(userId, courseId, sectionId, value.toString()));
    }

    @Override
    public List<ProgressSnapshot> findByUserAndCourse(long userId, long courseId) {
        Map<Object, Object> entries = redis.opsForHash().entries(key(userId, courseId));
        return entries.entrySet().stream()
                .map(entry -> ProgressSnapshot.decode(
                        userId, courseId, Long.parseLong(entry.getKey().toString()), entry.getValue().toString()))
                .toList();
    }

    @Override
    public boolean acknowledgeIfUnchanged(ProgressSnapshot snapshot) {
        Long acknowledged = redis.execute(
                ACK_IF_UNCHANGED,
                List.of(key(snapshot.userId(), snapshot.courseId())),
                Long.toString(snapshot.sectionId()), snapshot.encode());
        return acknowledged != null && acknowledged == 1L;
    }

    private String key(long userId, long courseId) {
        return "learning:progress:" + userId + ":" + courseId;
    }

}
