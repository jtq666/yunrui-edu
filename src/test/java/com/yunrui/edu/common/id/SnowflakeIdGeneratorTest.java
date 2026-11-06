package com.yunrui.edu.common.id;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SnowflakeIdGeneratorTest {
    @Test
    void generatedIdsAreUniqueAndIncreasing() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);
        Set<Long> ids = new HashSet<>();
        long previous = -1;
        for (int index = 0; index < 5000; index++) {
            long current = generator.nextId();
            ids.add(current);
            assertThat(current).isGreaterThan(previous);
            previous = current;
        }
        assertThat(ids).hasSize(5000);
    }
}
