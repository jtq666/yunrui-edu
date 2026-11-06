package com.yunrui.edu.promotion;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CouponCombinationService {
    private static final int MAX_CANDIDATES = 12;

    public CouponPlan bestPlan(BigDecimal orderAmount, List<Coupon> coupons, Instant now) {
        return availablePlans(orderAmount, coupons, now).get(0);
    }

    public List<CouponPlan> availablePlans(BigDecimal orderAmount, List<Coupon> coupons, Instant now) {
        List<Coupon> candidates = coupons.stream()
                .filter(coupon -> coupon.applicable(orderAmount, now))
                .limit(MAX_CANDIDATES)
                .toList();

        List<CouponPlan> plans = new ArrayList<>();
        plans.add(new CouponPlan(List.of(), BigDecimal.ZERO, orderAmount));
        for (Coupon coupon : candidates) {
            if (!coupon.stackable()) {
                plans.add(plan(orderAmount, List.of(coupon)));
            }
        }

        List<Coupon> stackable = candidates.stream().filter(Coupon::stackable).toList();
        int combinations = 1 << stackable.size();
        for (int mask = 1; mask < combinations; mask++) {
            List<Coupon> selected = new ArrayList<>();
            for (int index = 0; index < stackable.size(); index++) {
                if ((mask & (1 << index)) != 0) {
                    selected.add(stackable.get(index));
                }
            }
            plans.add(plan(orderAmount, selected));
        }

        return plans.stream()
                .sorted(Comparator.comparing(CouponPlan::discountAmount).reversed()
                        .thenComparing(plan -> plan.coupons().size()))
                .toList();
    }

    public CouponPlan selectedPlan(BigDecimal orderAmount, List<Coupon> availableCoupons,
                                   List<Long> selectedCouponIds, Instant now) {
        if (selectedCouponIds == null) {
            return bestPlan(orderAmount, availableCoupons, now);
        }
        if (selectedCouponIds.isEmpty()) {
            return plan(orderAmount, List.of());
        }
        Set<Long> uniqueIds = new HashSet<>(selectedCouponIds);
        if (uniqueIds.size() != selectedCouponIds.size()) {
            throw new IllegalArgumentException("不能重复选择同一张优惠券");
        }
        List<Coupon> selected = availableCoupons.stream()
                .filter(coupon -> uniqueIds.contains(coupon.id()))
                .toList();
        if (selected.size() != uniqueIds.size()) {
            throw new IllegalArgumentException("所选优惠券不存在、已使用或已过期");
        }
        if (selected.stream().anyMatch(coupon -> !coupon.applicable(orderAmount, now))) {
            throw new IllegalArgumentException("所选优惠券不满足本订单使用条件");
        }
        if (selected.size() > 1 && selected.stream().anyMatch(coupon -> !coupon.stackable())) {
            throw new IllegalArgumentException("所选优惠券中包含不可叠加券");
        }
        return plan(orderAmount, selected);
    }

    private CouponPlan plan(BigDecimal orderAmount, List<Coupon> coupons) {
        BigDecimal discount = coupons.stream()
                .map(Coupon::discountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .min(orderAmount);
        return new CouponPlan(List.copyOf(coupons), discount, orderAmount.subtract(discount));
    }

    public record CouponPlan(List<Coupon> coupons,
                             BigDecimal discountAmount,
                             BigDecimal payableAmount) {
    }
}
