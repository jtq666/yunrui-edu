package com.yunrui.edu.promotion;

import com.yunrui.edu.common.lock.DistributedLockManager;
import com.yunrui.edu.common.ratelimit.RedisSlidingWindowRateLimiter;
import com.yunrui.edu.common.web.TooManyRequestsException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;

@Service
public class CouponClaimService {
    private final CouponRepository repository;
    private final DistributedLockManager lockManager;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final RedisSlidingWindowRateLimiter rateLimiter;
    private final int claimRateLimitPerMinute;

    public CouponClaimService(CouponRepository repository,
                              DistributedLockManager lockManager,
                              TransactionTemplate transactionTemplate,
                              Clock clock,
                              RedisSlidingWindowRateLimiter rateLimiter,
                              @org.springframework.beans.factory.annotation.Value("${app.promotion.claim-rate-limit-per-minute:10}")
                              int claimRateLimitPerMinute) {
        this.repository = repository;
        this.lockManager = lockManager;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.rateLimiter = rateLimiter;
        this.claimRateLimitPerMinute = claimRateLimitPerMinute;
    }

    /**
     * 锁只用于减少相同用户的重复请求；数据库条件更新和唯一索引才是最终正确性保障。
     * TransactionTemplate确保数据库事务先提交，再释放Redis锁。
     */
    public void claim(long userId, long couponId) {
        String rateLimitKey = "rate-limit:coupon-claim:user:" + userId;
        if (!rateLimiter.allow(rateLimitKey, claimRateLimitPerMinute, Duration.ofMinutes(1))) {
            throw new TooManyRequestsException("领取请求过于频繁，请稍后再试");
        }
        String lockKey = "lock:coupon:" + couponId + ":user:" + userId;
        try (DistributedLockManager.LockHandle ignored = lockManager
                .tryLock(lockKey, Duration.ofSeconds(5))
                .orElseThrow(() -> new IllegalStateException("领取请求正在处理中，请勿重复提交"))) {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    if (!repository.claimStock(couponId, clock.instant())) {
                        throw new IllegalStateException("优惠券不存在、已过期或已领完");
                    }
                    repository.createUserCoupon(couponId, userId, clock.instant());
                } catch (DuplicateKeyException ex) {
                    throw new IllegalStateException("每位用户只能领取一次该优惠券");
                }
            });
        }
    }
}
