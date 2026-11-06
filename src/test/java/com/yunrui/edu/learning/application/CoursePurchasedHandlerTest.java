package com.yunrui.edu.learning.application;

import com.yunrui.edu.learning.domain.CourseEntitlement;
import com.yunrui.edu.learning.domain.CourseEntitlementRepository;
import com.yunrui.edu.messaging.outbox.CoursePurchasedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CoursePurchasedHandlerTest {
    @Test
    void duplicateMessageDoesNotGrantCourseTwice() {
        InMemoryRepository repository = new InMemoryRepository();
        CoursePurchasedHandler handler = new CoursePurchasedHandler(repository);
        CoursePurchasedEvent event = new CoursePurchasedEvent(
                UUID.randomUUID(), 1L, 10L, 20L, Instant.parse("2026-10-04T08:00:00Z"));

        boolean first = handler.handle(event);
        boolean duplicate = handler.handle(event);

        assertThat(first).isTrue();
        assertThat(duplicate).isFalse();
        assertThat(repository.entitlements).hasSize(1);
    }

    private static class InMemoryRepository implements CourseEntitlementRepository {
        private final List<CourseEntitlement> entitlements = new ArrayList<>();

        @Override
        public boolean existsByOrderId(long orderId) {
            return entitlements.stream().anyMatch(item -> item.orderId() == orderId);
        }

        @Override
        public boolean grantIfAbsent(CourseEntitlement entitlement) {
            if (existsByOrderId(entitlement.orderId())) {
                return false;
            }
            entitlements.add(entitlement);
            return true;
        }

        @Override
        public boolean existsByUserAndCourse(long userId, long courseId) {
            return entitlements.stream().anyMatch(value ->
                    value.userId() == userId && value.courseId() == courseId);
        }
    }
}
