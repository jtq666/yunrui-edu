package com.yunrui.edu.learning.progress;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressSnapshotTest {
    @Test
    void encodedSnapshotCanBeRestored() {
        ProgressSnapshot source = new ProgressSnapshot(
                1L, 2L, 3L, 95, 100, Instant.parse("2026-10-04T08:00:00Z"));

        ProgressSnapshot restored = ProgressSnapshot.decode(1L, 2L, 3L, source.encode());

        assertThat(restored).isEqualTo(source);
        assertThat(restored.completed()).isTrue();
    }
}
