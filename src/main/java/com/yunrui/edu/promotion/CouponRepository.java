package com.yunrui.edu.promotion;

import java.time.Instant;
import java.util.List;

public interface CouponRepository {
    boolean claimStock(long couponId, Instant now);

    void createUserCoupon(long couponId, long userId, Instant now);

    List<Coupon> findAvailableByUser(long userId, Instant now);

    boolean lockForOrder(long couponId, long userId, long orderId);

    void confirmUsedByOrder(long orderId);

    void releaseByOrder(long orderId);
}
