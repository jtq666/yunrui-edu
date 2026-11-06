package com.yunrui.edu.trade.application;

import com.yunrui.edu.messaging.outbox.OutboxMessage;
import com.yunrui.edu.messaging.outbox.OutboxRepository;
import com.yunrui.edu.trade.domain.PurchaseOrder;
import com.yunrui.edu.trade.domain.PurchaseOrderRepository;
import com.yunrui.edu.promotion.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class PaymentCallbackService {
    private final PurchaseOrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final Clock clock;
    private final CouponRepository couponRepository;

    public PaymentCallbackService(PurchaseOrderRepository orderRepository,
                                  OutboxRepository outboxRepository,
                                  Clock clock,
                                  CouponRepository couponRepository) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.clock = clock;
        this.couponRepository = couponRepository;
    }

    /**
     * 订单状态修改和本地消息写入处于同一个数据库事务中。
     * 即使MQ暂时不可用，提交后的消息仍可由后台任务继续投递。
     */
    @Transactional
    public PaymentCallbackResult confirm(PaymentCallbackCommand command) {
        PurchaseOrder order = orderRepository.findByIdForUpdate(command.orderId())
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + command.orderId()));

        if (order.getPayableAmount().compareTo(command.paidAmount()) != 0) {
            throw new IllegalArgumentException("支付金额与订单应付金额不一致");
        }

        Instant now = clock.instant();
        boolean firstProcessed = order.confirmPayment(command.transactionId(), now);
        if (!firstProcessed) {
            return new PaymentCallbackResult(order.getId(), false);
        }

        orderRepository.save(order);
        couponRepository.confirmUsedByOrder(order.getId());
        outboxRepository.append(OutboxMessage.coursePurchased(
                order.getId(), order.getUserId(), order.getCourseId(), now));
        return new PaymentCallbackResult(order.getId(), true);
    }
}
