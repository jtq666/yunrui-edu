package com.yunrui.edu.promotion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import com.yunrui.edu.auth.UserAccessGuard;
import com.yunrui.edu.course.Course;
import com.yunrui.edu.course.CourseCatalog;

@RestController
@RequestMapping("/api/coupons")
public class PromotionController {
    private final CouponClaimService claimService;
    private final CouponRepository repository;
    private final CouponCombinationService combinationService;
    private final Clock clock;
    private final UserAccessGuard accessGuard;
    private final CourseCatalog courseCatalog;

    public PromotionController(CouponClaimService claimService,
                               CouponRepository repository,
                               CouponCombinationService combinationService,
                               Clock clock, UserAccessGuard accessGuard,
                               CourseCatalog courseCatalog) {
        this.claimService = claimService;
        this.repository = repository;
        this.combinationService = combinationService;
        this.clock = clock;
        this.accessGuard = accessGuard;
        this.courseCatalog = courseCatalog;
    }

    @PostMapping("/claim")
    public ClaimResult claim(@Valid @RequestBody ClaimRequest request) {
        accessGuard.require(request.userId());
        claimService.claim(request.userId(), request.couponId());
        return new ClaimResult(true);
    }

    @PostMapping("/best-plan")
    public CouponCombinationService.CouponPlan bestPlan(@Valid @RequestBody BestPlanRequest request) {
        accessGuard.require(request.userId());
        List<Coupon> coupons = repository.findAvailableByUser(request.userId(), clock.instant());
        return combinationService.bestPlan(request.orderAmount(), coupons, clock.instant());
    }

    @PostMapping("/plans")
    public List<CouponCombinationService.CouponPlan> plans(@Valid @RequestBody PlansRequest request) {
        accessGuard.require(request.userId());
        Course course = courseCatalog.findPublished(request.courseId())
                .orElseThrow(() -> new IllegalArgumentException("课程不存在或尚未发布"));
        List<Coupon> coupons = repository.findAvailableByUser(request.userId(), clock.instant());
        return combinationService.availablePlans(course.price(), coupons, clock.instant());
    }

    public record ClaimRequest(@Positive long userId, @Positive long couponId) {
    }

    public record BestPlanRequest(@Positive long userId,
                                  @DecimalMin(value = "0.01") BigDecimal orderAmount) {
    }

    public record PlansRequest(@Positive long userId, @Positive long courseId) {
    }

    public record ClaimResult(boolean claimed) {
    }
}
