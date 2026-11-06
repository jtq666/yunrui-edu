package com.yunrui.edu.messaging.outbox;

import java.time.Instant;
import java.util.UUID;

public class OutboxMessage {
    private static final int MAX_RETRIES = 8;

    private final UUID id;
    private final String eventType;
    private final CoursePurchasedEvent event;
    private final Instant createdAt;
    private OutboxStatus status;
    private int retryCount;
    private Instant nextRetryAt;
    private String lastError;

    private OutboxMessage(UUID id,
                          String eventType,
                          CoursePurchasedEvent event,
                          Instant createdAt) {
        this.id = id;
        this.eventType = eventType;
        this.event = event;
        this.createdAt = createdAt;
        this.status = OutboxStatus.PENDING;
        this.nextRetryAt = createdAt;
    }

    public static OutboxMessage coursePurchased(long orderId, long userId, long courseId, Instant now) {
        UUID eventId = UUID.randomUUID();
        return new OutboxMessage(
                eventId,
                "course.purchased",
                new CoursePurchasedEvent(eventId, orderId, userId, courseId, now),
                now);
    }

    public static OutboxMessage restore(UUID id,
                                        String eventType,
                                        CoursePurchasedEvent event,
                                        Instant createdAt,
                                        OutboxStatus status,
                                        int retryCount,
                                        Instant nextRetryAt,
                                        String lastError) {
        OutboxMessage message = new OutboxMessage(id, eventType, event, createdAt);
        message.status = status;
        message.retryCount = retryCount;
        message.nextRetryAt = nextRetryAt;
        message.lastError = lastError;
        return message;
    }

    public void markPublished() {
        status = OutboxStatus.PUBLISHED;
        lastError = null;
    }

    public void markFailed(String error, Instant now) {
        retryCount++;
        lastError = error;
        if (retryCount >= MAX_RETRIES) {
            status = OutboxStatus.DEAD;
            return;
        }
        long delaySeconds = Math.min(300, 1L << Math.min(retryCount, 8));
        nextRetryAt = now.plusSeconds(delaySeconds);
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public CoursePurchasedEvent getEvent() {
        return event;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public String getLastError() {
        return lastError;
    }
}
