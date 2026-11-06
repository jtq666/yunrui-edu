package com.yunrui.edu.trade.application;

public record PaymentCallbackResult(long orderId, boolean firstProcessed) {
}
