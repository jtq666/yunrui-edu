package com.yunrui.edu.messaging.outbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class OutboxSchedulingConfiguration {
    @Bean
    OutboxPublisher outboxPublisher(OutboxRepository repository,
                                    DomainEventPublisher publisher,
                                    Clock clock) {
        return new OutboxPublisher(repository, publisher, clock);
    }
}
