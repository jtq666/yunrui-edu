package com.yunrui.edu.messaging.outbox;

import java.time.Instant;
import java.util.UUID;

public record CoursePurchasedEvent(
        UUID eventId,
        long orderId,
        long userId,
        long courseId,
        Instant occurredAt
) {
}
