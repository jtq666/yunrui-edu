package com.yunrui.edu.points;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PointsService {
    private static final DateTimeFormatter SEASON_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final PointsRecordRepository repository;
    private final PointsBoard board;
    private final Clock clock;

    public PointsService(PointsRecordRepository repository, PointsBoard board, Clock clock) {
        this.repository = repository;
        this.board = board;
        this.clock = clock;
    }

    public boolean add(String eventId, long userId, int points, String reason) {
        if (eventId == null || eventId.isBlank() || userId <= 0 || points <= 0) {
            throw new IllegalArgumentException("积分事件参数不合法");
        }
        return repository.append(eventId, userId, points, reason, currentSeason());
    }

    public List<PointsBoard.Entry> top(String season, int limit) {
        validateSeason(season);
        return board.top(season, Math.min(Math.max(limit, 1), 100));
    }

    public PointsBoard.Entry mine(String season, long userId) {
        validateSeason(season);
        return board.findUser(season, userId);
    }

    public String currentSeason() {
        return YearMonth.now(clock.withZone(ZoneOffset.UTC)).format(SEASON_FORMATTER);
    }

    private void validateSeason(String season) {
        if (season == null || !season.matches("\\d{6}")) {
            throw new IllegalArgumentException("赛季格式必须为yyyyMM");
        }
    }
}
