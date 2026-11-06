package com.yunrui.edu.promotion;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CouponCombinationServiceTest {
    private final CouponCombinationService service = new CouponCombinationService();
    private final Instant now = Instant.parse("2026-10-04T08:00:00Z");

    @Test
    void choosesLargestDiscountAndPrefersFewerCouponsOnTie() {
        Coupon c20 = coupon(1L, "减20", "100", "20", true);
        Coupon c30 = coupon(2L, "减30", "100", "30", true);
        Coupon c50 = coupon(3L, "单券减50", "100", "50", false);

        CouponCombinationService.CouponPlan plan = service.bestPlan(
                new BigDecimal("200"), List.of(c20, c30, c50), now);

        assertThat(plan.discountAmount()).isEqualByComparingTo("50");
        assertThat(plan.coupons()).hasSize(1);
        assertThat(plan.coupons().get(0).id()).isEqualTo(3L);
        assertThat(plan.payableAmount()).isEqualByComparingTo("150");
    }

    @Test
    void usesTheCouponsExplicitlySelectedByTheUser() {
        Coupon c20 = coupon(1L, "减20", "100", "20", true);
        Coupon c30 = coupon(2L, "减30", "100", "30", true);

        CouponCombinationService.CouponPlan plan = service.selectedPlan(
                new BigDecimal("200"), List.of(c20, c30), List.of(1L), now);

        assertThat(plan.discountAmount()).isEqualByComparingTo("20");
        assertThat(plan.coupons()).extracting(Coupon::id).containsExactly(1L);
    }

    @Test
    void rejectsStackingANonStackableCoupon() {
        Coupon stackable = coupon(1L, "减20", "100", "20", true);
        Coupon exclusive = coupon(2L, "单券减30", "100", "30", false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.selectedPlan(
                        new BigDecimal("200"), List.of(stackable, exclusive), List.of(1L, 2L), now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不可叠加");
    }

    @Test
    void emptySelectionMeansDoNotUseACoupon() {
        Coupon coupon = coupon(1L, "减20", "100", "20", true);

        CouponCombinationService.CouponPlan plan = service.selectedPlan(
                new BigDecimal("200"), List.of(coupon), List.of(), now);

        assertThat(plan.discountAmount()).isEqualByComparingTo("0");
        assertThat(plan.payableAmount()).isEqualByComparingTo("200");
    }

    private Coupon coupon(long id, String name, String threshold, String discount, boolean stackable) {
        return new Coupon(
                id, name, new BigDecimal(threshold), new BigDecimal(discount),
                100, 0, now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS), stackable);
    }
}
