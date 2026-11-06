package com.yunrui.edu.trade.application;

import com.yunrui.edu.messaging.outbox.OutboxMessage;
import com.yunrui.edu.messaging.outbox.OutboxRepository;
import com.yunrui.edu.promotion.CouponRepository;
import com.yunrui.edu.trade.domain.OrderStatus;
import com.yunrui.edu.trade.domain.PurchaseOrder;
import com.yunrui.edu.trade.domain.PurchaseOrderRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PaymentCallbackServiceTest {
    private final Instant now = Instant.parse("2026-10-04T08:00:00Z");
    private final InMemoryOrderRepository orders = new InMemoryOrderRepository();
    private final InMemoryOutboxRepository outbox = new InMemoryOutboxRepository();
    private final PaymentCallbackService service = new PaymentCallbackService(
            orders, outbox, Clock.fixed(now, ZoneOffset.UTC), mock(CouponRepository.class));

    @Test
    void repeatedCallbackWithSameTransactionCreatesOnlyOneMessage() {
        orders.save(pendingOrder());

        PaymentCallbackResult first = service.confirm(command("pay-001"));
        PaymentCallbackResult second = service.confirm(command("pay-001"));

        assertThat(first.firstProcessed()).isTrue();
        assertThat(second.firstProcessed()).isFalse();
        assertThat(outbox.messages).hasSize(1);
        assertThat(orders.orders.get(1L).getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void callbackWithAnotherTransactionIsRejectedAfterPayment() {
        orders.save(pendingOrder());
        service.confirm(command("pay-001"));

        assertThatThrownBy(() -> service.confirm(command("pay-002")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("另一笔支付");
    }

    private PurchaseOrder pendingOrder() {
        return new PurchaseOrder(1L, 10L, 20L, OrderStatus.PENDING_PAYMENT,
                new java.math.BigDecimal("299.00"), java.math.BigDecimal.ZERO,
                new java.math.BigDecimal("299.00"));
    }

    private PaymentCallbackCommand command(String transactionId) {
        return new PaymentCallbackCommand(1L, transactionId, new java.math.BigDecimal("299.00"));
    }

    private static class InMemoryOrderRepository implements PurchaseOrderRepository {
        private final Map<Long, PurchaseOrder> orders = new HashMap<>();

        @Override
        public void insert(PurchaseOrder order) {
            orders.put(order.getId(), order);
        }

        @Override
        public Optional<PurchaseOrder> findByIdForUpdate(long orderId) {
            return Optional.ofNullable(orders.get(orderId));
        }

        @Override
        public Optional<PurchaseOrder> findById(long orderId) {
            return Optional.ofNullable(orders.get(orderId));
        }

        @Override
        public List<PurchaseOrder> findByUserId(long userId) {
            return orders.values().stream().filter(order -> order.getUserId() == userId).toList();
        }

        @Override
        public void save(PurchaseOrder order) {
            orders.put(order.getId(), order);
        }
    }

    private static class InMemoryOutboxRepository implements OutboxRepository {
        private final List<OutboxMessage> messages = new ArrayList<>();

        @Override
        public void append(OutboxMessage message) {
            messages.add(message);
        }

        @Override
        public List<OutboxMessage> findPublishable(Instant now, int limit) {
            return List.of();
        }

        @Override
        public void save(OutboxMessage message) {
        }
    }
}
