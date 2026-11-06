package com.yunrui.edu.learning.application;

import com.yunrui.edu.learning.domain.CourseEntitlement;
import com.yunrui.edu.learning.domain.CourseEntitlementRepository;
import com.yunrui.edu.messaging.outbox.CoursePurchasedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoursePurchasedHandler {
    private final CourseEntitlementRepository repository;

    public CoursePurchasedHandler(CourseEntitlementRepository repository) {
        this.repository = repository;
    }

    /**
     * RabbitMQ至少投递一次，因此消费者必须允许同一事件重复到达。
     * 数据库还需要为order_id建立唯一索引，作为最终幂等防线。
     */
    @Transactional
    public boolean handle(CoursePurchasedEvent event) {
        if (repository.existsByOrderId(event.orderId())) {
            return false;
        }
        return repository.grantIfAbsent(new CourseEntitlement(
                event.orderId(),
                event.userId(),
                event.courseId(),
                event.eventId(),
                event.occurredAt()));
    }
}
