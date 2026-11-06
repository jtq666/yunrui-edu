package com.yunrui.edu.points;

import java.util.List;

public interface PointsBoard {
    boolean applyOnce(PointsRecord record);

    List<Entry> top(String season, int limit);

    Entry findUser(String season, long userId);

    record Entry(long userId, int points, long rank) {
    }
}
