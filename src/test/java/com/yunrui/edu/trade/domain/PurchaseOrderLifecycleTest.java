package com.yunrui.edu.trade.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PurchaseOrderLifecycleTest {
    @Test
    void pendingOrderCanCloseButPaidOrderCannot() {
        PurchaseOrder pending = order(OrderStatus.PENDING_PAYMENT);
        pending.close();
        assertThat(pending.getStatus()).isEqualTo(OrderStatus.CLOSED);

        assertThatThrownBy(() -> order(OrderStatus.PAID).close())
                .isInstanceOf(IllegalStateException.class);
    }

    private PurchaseOrder order(OrderStatus status) {
        return new PurchaseOrder(1, 2, 3, status,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"));
    }
}
