package com.yunrui.edu.messaging.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublishJob {
    private final OutboxPublisher publisher;

    public OutboxPublishJob(OutboxPublisher publisher) {
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:1000}")
    public void publish() {
        publisher.publishBatch();
    }
}
