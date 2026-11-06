package com.yunrui.edu.trade.application;

import com.yunrui.edu.promotion.CouponRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class ExpiredOrderCloseJob {
    private final JdbcClient jdbc;
    private final CouponRepository coupons;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public ExpiredOrderCloseJob(JdbcClient jdbc, CouponRepository coupons,
                                TransactionTemplate transactions, Clock clock) {
        this.jdbc = jdbc;
        this.coupons = coupons;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.order.close-delay-ms:60000}")
    public void closeExpired() {
        Instant deadline = clock.instant().minus(Duration.ofMinutes(30));
        List<Long> ids = jdbc.sql("""
                SELECT id FROM edu_purchase_order
                WHERE status = 'PENDING_PAYMENT' AND created_at < :deadline
                ORDER BY created_at LIMIT 100
                """).param("deadline", deadline).query(Long.class).list();
        for (Long id : ids) {
            transactions.executeWithoutResult(status -> {
                int changed = jdbc.sql("""
                        UPDATE edu_purchase_order SET status = 'CLOSED'
                        WHERE id = :id AND status = 'PENDING_PAYMENT'
                        """).param("id", id).update();
                if (changed == 1) {
                    coupons.releaseByOrder(id);
                }
            });
        }
    }
}
