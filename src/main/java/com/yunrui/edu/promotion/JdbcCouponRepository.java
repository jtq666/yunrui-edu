package com.yunrui.edu.promotion;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class JdbcCouponRepository implements CouponRepository {
    private final JdbcClient jdbc;

    public JdbcCouponRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean claimStock(long couponId, Instant now) {
        return jdbc.sql("""
                UPDATE edu_coupon
                SET claimed_quantity = claimed_quantity + 1
                WHERE id = :couponId
                  AND status = 'ACTIVE'
                  AND valid_from <= :now
                  AND valid_until > :now
                  AND claimed_quantity < total_quantity
                """)
                .param("couponId", couponId)
                .param("now", now)
                .update() == 1;
    }

    @Override
    public void createUserCoupon(long couponId, long userId, Instant now) {
        jdbc.sql("""
                INSERT INTO edu_user_coupon(coupon_id, user_id, claimed_at)
                VALUES (:couponId, :userId, :now)
                """)
                .param("couponId", couponId)
                .param("userId", userId)
                .param("now", now)
                .update();
    }

    @Override
    public List<Coupon> findAvailableByUser(long userId, Instant now) {
        return jdbc.sql("""
                SELECT c.id, c.name, c.threshold_amount, c.discount_amount,
                       c.total_quantity, c.claimed_quantity, c.valid_from,
                       c.valid_until, c.stackable
                FROM edu_user_coupon uc
                JOIN edu_coupon c ON c.id = uc.coupon_id
                WHERE uc.user_id = :userId
                  AND uc.status = 'AVAILABLE'
                  AND c.valid_from <= :now
                  AND c.valid_until > :now
                """)
                .param("userId", userId)
                .param("now", now)
                .query((rs, rowNum) -> new Coupon(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getBigDecimal("threshold_amount"),
                        rs.getBigDecimal("discount_amount"),
                        rs.getInt("total_quantity"),
                        rs.getInt("claimed_quantity"),
                        rs.getTimestamp("valid_from").toInstant(),
                        rs.getTimestamp("valid_until").toInstant(),
                        rs.getBoolean("stackable")))
                .list();
    }

    @Override
    public boolean lockForOrder(long couponId, long userId, long orderId) {
        return jdbc.sql("""
                UPDATE edu_user_coupon
                SET status = 'LOCKED', used_order_id = :orderId
                WHERE coupon_id = :couponId AND user_id = :userId AND status = 'AVAILABLE'
                """)
                .param("couponId", couponId)
                .param("userId", userId)
                .param("orderId", orderId)
                .update() == 1;
    }

    @Override
    public void confirmUsedByOrder(long orderId) {
        jdbc.sql("""
                UPDATE edu_user_coupon SET status = 'USED'
                WHERE used_order_id = :orderId AND status = 'LOCKED'
                """)
                .param("orderId", orderId)
                .update();
    }

    @Override
    public void releaseByOrder(long orderId) {
        jdbc.sql("""
                UPDATE edu_user_coupon
                SET status = 'AVAILABLE', used_order_id = NULL
                WHERE used_order_id = :orderId AND status = 'LOCKED'
                """)
                .param("orderId", orderId)
                .update();
    }
}
