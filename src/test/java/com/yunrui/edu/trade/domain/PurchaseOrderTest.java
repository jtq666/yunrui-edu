package com.yunrui.edu.trade.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseOrderTest {
    @Test
    void restoredPaidOrderAcceptsRepeatedCallbackFromSameTransaction() {
        Instant paidAt = Instant.parse("2026-10-04T08:00:00Z");
        PurchaseOrder restored = PurchaseOrder.restore(
                1L, 10L, 20L, OrderStatus.PAID,
                new BigDecimal("299.00"), BigDecimal.ZERO, new BigDecimal("299.00"),
                "pay-001", paidAt);

        boolean changed = restored.confirmPayment("pay-001", paidAt.plusSeconds(10));

        assertThat(changed).isFalse();
        assertThat(restored.getPaymentTransactionId()).isEqualTo("pay-001");
        assertThat(restored.getPaidAt()).isEqualTo(paidAt);
    }
}
