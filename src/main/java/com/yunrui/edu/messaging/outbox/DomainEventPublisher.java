package com.yunrui.edu.messaging.outbox;

public interface DomainEventPublisher {
    void publish(CoursePurchasedEvent event);
}
