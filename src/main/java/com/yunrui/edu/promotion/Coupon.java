package com.yunrui.edu.promotion;

import java.math.BigDecimal;
import java.time.Instant;

public record Coupon(
        long id,
        String name,
        BigDecimal thresholdAmount,
        BigDecimal discountAmount,
        int totalQuantity,
        int claimedQuantity,
        Instant validFrom,
        Instant validUntil,
        boolean stackable
) {
    public boolean applicable(BigDecimal orderAmount, Instant now) {
        return orderAmount.compareTo(thresholdAmount) >= 0
                && !now.isBefore(validFrom)
                && now.isBefore(validUntil);
    }
}
