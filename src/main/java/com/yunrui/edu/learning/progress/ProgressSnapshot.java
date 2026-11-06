package com.yunrui.edu.learning.progress;

import java.time.Instant;

public record ProgressSnapshot(
        long userId,
        long courseId,
        long sectionId,
        int positionSeconds,
        int durationSeconds,
        Instant reportedAt
) {
    public ProgressSnapshot {
        if (userId <= 0 || courseId <= 0 || sectionId <= 0) {
            throw new IllegalArgumentException("用户、课程和章节编号必须为正数");
        }
        if (positionSeconds < 0 || durationSeconds <= 0 || positionSeconds > durationSeconds) {
            throw new IllegalArgumentException("播放进度必须位于课程时长范围内");
        }
    }

    public boolean completed() {
        return positionSeconds >= Math.ceil(durationSeconds * 0.9);
    }

    public String encode() {
        return positionSeconds + ":" + durationSeconds + ":" + reportedAt.toEpochMilli();
    }

    public static ProgressSnapshot decode(long userId,
                                          long courseId,
                                          long sectionId,
                                          String encoded) {
        String[] parts = encoded.split(":", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("无法解析学习进度快照");
        }
        return new ProgressSnapshot(
                userId,
                courseId,
                sectionId,
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Instant.ofEpochMilli(Long.parseLong(parts[2])));
    }
}
