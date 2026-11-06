package com.yunrui.edu.trade.application;

import com.yunrui.edu.common.id.IdGenerator;
import com.yunrui.edu.course.CourseCatalog;
import com.yunrui.edu.course.Course;
import com.yunrui.edu.promotion.Coupon;
import com.yunrui.edu.promotion.CouponCombinationService;
import com.yunrui.edu.promotion.CouponRepository;
import com.yunrui.edu.trade.domain.OrderStatus;
import com.yunrui.edu.trade.domain.PurchaseOrder;
import com.yunrui.edu.trade.domain.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OrderApplicationService {
    private final PurchaseOrderRepository repository;
    private final CourseCatalog courseCatalog;
    private final IdGenerator idGenerator;
    private final CouponRepository couponRepository;
    private final CouponCombinationService combinationService;

    public OrderApplicationService(PurchaseOrderRepository repository,
                                   CourseCatalog courseCatalog,
                                   IdGenerator idGenerator,
                                   CouponRepository couponRepository,
                                   CouponCombinationService combinationService) {
        this.repository = repository;
        this.courseCatalog = courseCatalog;
        this.idGenerator = idGenerator;
        this.couponRepository = couponRepository;
        this.combinationService = combinationService;
    }

    @Transactional
    public long create(CreateOrderCommand command) {
        Course course = courseCatalog.findPublished(command.courseId())
                .orElseThrow(() -> new IllegalArgumentException("课程不存在或尚未发布"));
        long orderId = idGenerator.nextId();
        Instant now = Instant.now();
        List<Coupon> available = couponRepository.findAvailableByUser(command.userId(), now);
        CouponCombinationService.CouponPlan plan =
                combinationService.selectedPlan(course.price(), available, command.couponIds(), now);
        for (Coupon coupon : plan.coupons()) {
            if (!couponRepository.lockForOrder(coupon.id(), command.userId(), orderId)) {
                throw new IllegalStateException("优惠券状态已变化，请重新下单");
            }
        }
        repository.insert(new PurchaseOrder(
                orderId, command.userId(), command.courseId(), OrderStatus.PENDING_PAYMENT,
                course.price(), plan.discountAmount(), plan.payableAmount()));
        return orderId;
    }

    @Transactional
    public void cancel(long userId, long orderId) {
        PurchaseOrder order = repository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));
        if (order.getUserId() != userId) {
            throw new IllegalStateException("不能取消其他用户的订单");
        }
        order.close();
        repository.save(order);
        couponRepository.releaseByOrder(orderId);
    }
}
