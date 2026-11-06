package com.yunrui.edu.trade.application;

import java.util.List;

public record CreateOrderCommand(long userId, long courseId, List<Long> couponIds) {
    public CreateOrderCommand {
        if (userId <= 0 || courseId <= 0) {
            throw new IllegalArgumentException("userId和courseId必须为正数");
        }
    }

    public CreateOrderCommand(long userId, long courseId) {
        this(userId, courseId, null);
    }
}
