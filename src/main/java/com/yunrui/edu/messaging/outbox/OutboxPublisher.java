package com.yunrui.edu.messaging.outbox;

import java.time.Clock;

public class OutboxPublisher {
    private static final int BATCH_SIZE = 100;

    private final OutboxRepository repository;
    private final DomainEventPublisher publisher;
    private final Clock clock;

    public OutboxPublisher(OutboxRepository repository,
                           DomainEventPublisher publisher,
                           Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
    }

    public int publishBatch() {
        int published = 0;
        for (OutboxMessage message : repository.findPublishable(clock.instant(), BATCH_SIZE)) {
            try {
                publisher.publish(message.getEvent());
                message.markPublished();
                published++;
            } catch (RuntimeException ex) {
                message.markFailed(ex.getMessage(), clock.instant());
            }
            repository.save(message);
        }
        return published;
    }
}
