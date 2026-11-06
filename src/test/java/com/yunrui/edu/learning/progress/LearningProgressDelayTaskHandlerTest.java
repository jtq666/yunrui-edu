package com.yunrui.edu.learning.progress;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LearningProgressDelayTaskHandlerTest {
    private final ProgressBuffer buffer = mock(ProgressBuffer.class);
    private final LearningProgressRepository repository = mock(LearningProgressRepository.class);
    private final LearningProgressDelayTaskHandler handler =
            new LearningProgressDelayTaskHandler(buffer, repository, 20_000);

    @Test
    void persistsWhenRedisStillContainsTheTaskSnapshot() {
        ProgressSnapshot snapshot = snapshot(30, 3);
        when(buffer.find(1, 2, 3)).thenReturn(Optional.of(snapshot));
        when(buffer.acknowledgeIfUnchanged(snapshot)).thenReturn(true);

        assertThat(handler.persistIfUnchanged(snapshot)).isTrue();

        verify(repository).upsertBatch(java.util.List.of(snapshot));
        verify(buffer).acknowledgeIfUnchanged(snapshot);
    }

    @Test
    void skipsOldTaskWhenRedisAlreadyContainsNewerProgress() {
        ProgressSnapshot oldSnapshot = snapshot(30, 3);
        ProgressSnapshot newerSnapshot = snapshot(45, 4);
        when(buffer.find(1, 2, 3)).thenReturn(Optional.of(newerSnapshot));

        assertThat(handler.persistIfUnchanged(oldSnapshot)).isFalse();

        verify(repository, never()).upsertBatch(anyList());
        verify(buffer, never()).acknowledgeIfUnchanged(oldSnapshot);
    }

    private ProgressSnapshot snapshot(int position, long sequence) {
        return new ProgressSnapshot(1, 2, 3, position, 100,
                Instant.parse("2026-10-10T00:00:00Z").plusSeconds(sequence));
    }
}
