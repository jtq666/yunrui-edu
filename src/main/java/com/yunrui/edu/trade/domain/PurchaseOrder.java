package com.yunrui.edu.trade.domain;

import java.math.BigDecimal;
import java.time.Instant;

public class PurchaseOrder {
    private final long id;
    private final long userId;
    private final long courseId;
    private final BigDecimal originalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal payableAmount;
    private OrderStatus status;
    private String paymentTransactionId;
    private Instant paidAt;

    public PurchaseOrder(long id, long userId, long courseId, OrderStatus status,
                         BigDecimal originalAmount, BigDecimal discountAmount,
                         BigDecimal payableAmount) {
        this.id = id;
        this.userId = userId;
        this.courseId = courseId;
        this.status = status;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.payableAmount = payableAmount;
    }

    public static PurchaseOrder restore(long id,
                                        long userId,
                                        long courseId,
                                        OrderStatus status,
                                        BigDecimal originalAmount,
                                        BigDecimal discountAmount,
                                        BigDecimal payableAmount,
                                        String paymentTransactionId,
                                        Instant paidAt) {
        PurchaseOrder order = new PurchaseOrder(id, userId, courseId, status,
                originalAmount, discountAmount, payableAmount);
        order.paymentTransactionId = paymentTransactionId;
        order.paidAt = paidAt;
        return order;
    }

    public boolean confirmPayment(String transactionId, Instant now) {
        if (status == OrderStatus.PAID) {
            if (!transactionId.equals(paymentTransactionId)) {
                throw new IllegalStateException("订单已由另一笔支付完成");
            }
            return false;
        }
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("当前订单状态不允许支付: " + status);
        }
        status = OrderStatus.PAID;
        paymentTransactionId = transactionId;
        paidAt = now;
        return true;
    }

    public void close() {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("只有待支付订单可以关闭");
        }
        status = OrderStatus.CLOSED;
    }

    public void refund() {
        if (status != OrderStatus.PAID) {
            throw new IllegalStateException("只有已支付订单可以退款");
        }
        status = OrderStatus.REFUNDED;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public long getCourseId() {
        return courseId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getOriginalAmount() { return originalAmount; }

    public BigDecimal getDiscountAmount() { return discountAmount; }

    public BigDecimal getPayableAmount() { return payableAmount; }

    public String getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}
