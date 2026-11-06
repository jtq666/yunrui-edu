package com.yunrui.edu.messaging.outbox;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPublisherTest {
    @Test
    void failedMessageIsKeptForRetryInsteadOfBeingLost() {
        Instant now = Instant.parse("2026-10-04T08:00:00Z");
        OutboxMessage message = OutboxMessage.coursePurchased(1L, 10L, 20L, now);
        InMemoryRepository repository = new InMemoryRepository(message);
        DomainEventPublisher failingPublisher = event -> {
            throw new IllegalStateException("RabbitMQ暂时不可用");
        };
        OutboxPublisher publisher = new OutboxPublisher(
                repository, failingPublisher, Clock.fixed(now, ZoneOffset.UTC));

        int count = publisher.publishBatch();

        assertThat(count).isZero();
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(message.getRetryCount()).isEqualTo(1);
        assertThat(message.getNextRetryAt()).isAfter(now);
        assertThat(message.getLastError()).contains("RabbitMQ");
    }

    private static class InMemoryRepository implements OutboxRepository {
        private final List<OutboxMessage> messages = new ArrayList<>();

        private InMemoryRepository(OutboxMessage message) {
            messages.add(message);
        }

        @Override
        public void append(OutboxMessage message) {
            messages.add(message);
        }

        @Override
        public List<OutboxMessage> findPublishable(Instant now, int limit) {
            return messages.stream()
                    .filter(item -> item.getStatus() == OutboxStatus.PENDING)
                    .filter(item -> !item.getNextRetryAt().isAfter(now))
                    .limit(limit)
                    .toList();
        }

        @Override
        public void save(OutboxMessage message) {
        }
    }
}
