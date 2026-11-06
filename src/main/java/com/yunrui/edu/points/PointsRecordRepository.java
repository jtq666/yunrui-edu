package com.yunrui.edu.points;

import java.util.List;

public interface PointsRecordRepository {
    boolean append(String eventId, long userId, int points, String reason, String season);

    List<PointsRecord> findPending(int limit);

    void markRanked(long id);

    void replaceArchive(String season, List<PointsBoard.Entry> entries);
}
