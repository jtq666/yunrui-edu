package com.yunrui.edu.points;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PointsProjectionJob {
    private final PointsRecordRepository repository;
    private final PointsBoard board;

    public PointsProjectionJob(PointsRecordRepository repository, PointsBoard board) {
        this.repository = repository;
        this.board = board;
    }

    @Scheduled(fixedDelayString = "${app.points.project-delay-ms:2000}")
    public void project() {
        List<PointsRecord> records = repository.findPending(200);
        for (PointsRecord record : records) {
            board.applyOnce(record);
            repository.markRanked(record.id());
        }
    }
}
