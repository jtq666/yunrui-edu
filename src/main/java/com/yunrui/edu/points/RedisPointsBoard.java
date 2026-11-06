package com.yunrui.edu.points;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class RedisPointsBoard implements PointsBoard {
    private static final DefaultRedisScript<Long> APPLY_ONCE = new DefaultRedisScript<>("""
            if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
                return 0
            end
            redis.call('ZINCRBY', KEYS[1], ARGV[3], ARGV[2])
            redis.call('SADD', KEYS[2], ARGV[1])
            redis.call('EXPIRE', KEYS[1], ARGV[4])
            redis.call('EXPIRE', KEYS[2], ARGV[4])
            return 1
            """, Long.class);
    private static final Duration RETENTION = Duration.ofDays(400);

    private final StringRedisTemplate redis;

    public RedisPointsBoard(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean applyOnce(PointsRecord record) {
        Long applied = redis.execute(
                APPLY_ONCE,
                List.of(boardKey(record.season()), eventKey(record.season())),
                record.eventId(),
                Long.toString(record.userId()),
                Integer.toString(record.points()),
                Long.toString(RETENTION.toSeconds()));
        return applied != null && applied == 1L;
    }

    @Override
    public List<Entry> top(String season, int limit) {
        Set<ZSetOperations.TypedTuple<String>> tuples = redis.opsForZSet()
                .reverseRangeWithScores(boardKey(season), 0, Math.max(0, limit - 1));
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }
        List<Entry> result = new ArrayList<>();
        long rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            result.add(new Entry(
                    Long.parseLong(tuple.getValue()),
                    tuple.getScore().intValue(),
                    rank++));
        }
        return result;
    }

    @Override
    public Entry findUser(String season, long userId) {
        Long zeroBasedRank = redis.opsForZSet().reverseRank(boardKey(season), Long.toString(userId));
        Double score = redis.opsForZSet().score(boardKey(season), Long.toString(userId));
        if (zeroBasedRank == null || score == null) {
            return null;
        }
        return new Entry(userId, score.intValue(), zeroBasedRank + 1);
    }

    private String boardKey(String season) {
        return "points:board:" + season;
    }

    private String eventKey(String season) {
        return "points:events:" + season;
    }
}
