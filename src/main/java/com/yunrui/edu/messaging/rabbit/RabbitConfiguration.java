package com.yunrui.edu.messaging.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {
    @Bean
    DirectExchange educationExchange() {
        return new DirectExchange(RabbitTopology.EDUCATION_EXCHANGE, true, false);
    }

    @Bean
    Queue coursePurchasedQueue() {
        return new Queue(RabbitTopology.COURSE_PURCHASED_QUEUE, true);
    }

    @Bean
    Binding coursePurchasedBinding(DirectExchange educationExchange, Queue coursePurchasedQueue) {
        return BindingBuilder.bind(coursePurchasedQueue)
                .to(educationExchange)
                .with(RabbitTopology.COURSE_PURCHASED_KEY);
    }

    @Bean
    Jackson2JsonMessageConverter rabbitJsonConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                  Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        return template;
    }
}
