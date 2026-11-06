package com.yunrui.edu.trade.application;

import java.math.BigDecimal;

public record PaymentCallbackCommand(long orderId, String transactionId, BigDecimal paidAmount) {
    public PaymentCallbackCommand {
        if (orderId <= 0) {
            throw new IllegalArgumentException("orderId必须为正数");
        }
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId不能为空");
        }
        if (paidAmount == null || paidAmount.signum() < 0) {
            throw new IllegalArgumentException("paidAmount不合法");
        }
    }
}
