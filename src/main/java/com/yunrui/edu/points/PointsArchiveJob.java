package com.yunrui.edu.points;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PointsArchiveJob {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private final PointsRecordRepository repository;
    private final PointsBoard board;
    private final Clock clock;

    public PointsArchiveJob(PointsRecordRepository repository, PointsBoard board, Clock clock) {
        this.repository = repository;
        this.board = board;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.points.archive-cron:0 10 0 1 * *}")
    public void archivePreviousSeason() {
        String season = YearMonth.now(clock.withZone(ZoneOffset.UTC))
                .minusMonths(1)
                .format(FORMATTER);
        List<PointsBoard.Entry> entries = board.top(season, 10000);
        repository.replaceArchive(season, entries);
    }
}
