package com.yunrui.edu.points;

public record PointsRecord(
        long id,
        String eventId,
        long userId,
        int points,
        String reason,
        String season
) {
}
