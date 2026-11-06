package com.yunrui.edu.points;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PointsServiceTest {
    @Test
    void duplicatedBusinessEventIsAcceptedOnlyOnce() {
        FakeRepository repository = new FakeRepository();
        PointsService service = new PointsService(
                repository,
                new FakeBoard(),
                Clock.fixed(Instant.parse("2026-10-04T08:00:00Z"), ZoneOffset.UTC));

        assertThat(service.add("lesson-completed-1", 10L, 5, "完成章节")).isTrue();
        assertThat(service.add("lesson-completed-1", 10L, 5, "完成章节")).isFalse();
        assertThat(repository.events).hasSize(1);
        assertThat(service.currentSeason()).isEqualTo("202610");
    }

    private static class FakeRepository implements PointsRecordRepository {
        private final List<String> events = new ArrayList<>();

        @Override
        public boolean append(String eventId, long userId, int points, String reason, String season) {
            if (events.contains(eventId)) return false;
            events.add(eventId);
            return true;
        }

        @Override public List<PointsRecord> findPending(int limit) { return List.of(); }
        @Override public void markRanked(long id) { }
        @Override public void replaceArchive(String season, List<PointsBoard.Entry> entries) { }
    }

    private static class FakeBoard implements PointsBoard {
        @Override public boolean applyOnce(PointsRecord record) { return true; }
        @Override public List<Entry> top(String season, int limit) { return List.of(); }
        @Override public Entry findUser(String season, long userId) { return null; }
    }
}
