package com.yunrui.edu.learning.progress;

import com.yunrui.edu.learning.domain.CourseEntitlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LearningProgressServiceTest {
    private final ProgressBuffer buffer = mock(ProgressBuffer.class);
    private final LearningProgressRepository repository = mock(LearningProgressRepository.class);
    private final CourseEntitlementRepository entitlements = mock(CourseEntitlementRepository.class);
    private final LearningProgressDelayTaskHandler delayTasks = mock(LearningProgressDelayTaskHandler.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T00:00:00Z"), ZoneOffset.UTC);
    private LearningProgressService service;

    @BeforeEach
    void setUp() {
        service = new LearningProgressService(buffer, repository, clock, entitlements, delayTasks);
        when(entitlements.existsByUserAndCourse(1, 2)).thenReturn(true);
        when(repository.sectionBelongsToCourse(3, 2)).thenReturn(true);
    }

    @Test
    void ordinaryProgressIsBufferedAndScheduledInsteadOfWrittenImmediately() {
        assertThat(service.report(1, 2, 3, 30, 100)).isTrue();

        verify(delayTasks).schedule(any(ProgressSnapshot.class));
        verify(repository, never()).upsertBatch(anyList());
    }

    @Test
    void completedProgressIsWrittenImmediatelyWithoutDelayTask() {
        assertThat(service.report(1, 2, 3, 90, 100)).isTrue();

        verify(repository).upsertBatch(anyList());
        verify(buffer).acknowledgeIfUnchanged(any(ProgressSnapshot.class));
        verify(delayTasks, never()).schedule(any());
    }

    @Test
    void queryUsesNewerRedisSnapshotOverStoredSnapshot() {
        ProgressSnapshot stored = snapshot(30, 10);
        ProgressSnapshot cached = snapshot(45, 11);
        when(repository.findByUserAndCourse(1, 2)).thenReturn(List.of(stored));
        when(buffer.findByUserAndCourse(1, 2)).thenReturn(List.of(cached));

        assertThat(service.query(1, 2)).containsExactly(cached);
    }

    private ProgressSnapshot snapshot(int position, long sequence) {
        return new ProgressSnapshot(1, 2, 3, position, 100, clock.instant().plusSeconds(sequence));
    }
}
