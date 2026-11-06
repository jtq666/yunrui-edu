package com.yunrui.edu.learning.infrastructure;

import com.yunrui.edu.learning.application.CoursePurchasedHandler;
import com.yunrui.edu.messaging.outbox.CoursePurchasedEvent;
import com.yunrui.edu.messaging.rabbit.RabbitTopology;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CoursePurchasedRabbitListener {
    private final CoursePurchasedHandler handler;

    public CoursePurchasedRabbitListener(CoursePurchasedHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(queues = RabbitTopology.COURSE_PURCHASED_QUEUE)
    public void onMessage(CoursePurchasedEvent event) {
        handler.handle(event);
    }
}
