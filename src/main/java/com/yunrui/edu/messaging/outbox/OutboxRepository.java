package com.yunrui.edu.messaging.outbox;

import java.time.Instant;
import java.util.List;

public interface OutboxRepository {
    void append(OutboxMessage message);

    List<OutboxMessage> findPublishable(Instant now, int limit);

    void save(OutboxMessage message);
}
