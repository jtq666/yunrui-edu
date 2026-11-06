package com.yunrui.edu.messaging.rabbit;

import com.yunrui.edu.messaging.outbox.CoursePurchasedEvent;
import com.yunrui.edu.messaging.outbox.DomainEventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitDomainEventPublisher implements DomainEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public RabbitDomainEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(CoursePurchasedEvent event) {
        rabbitTemplate.invoke(operations -> {
            operations.convertAndSend(
                    RabbitTopology.EDUCATION_EXCHANGE,
                    RabbitTopology.COURSE_PURCHASED_KEY,
                    event);
            operations.waitForConfirmsOrDie(5_000);
            return null;
        });
    }
}
