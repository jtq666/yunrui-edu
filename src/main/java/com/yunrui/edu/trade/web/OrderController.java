package com.yunrui.edu.trade.web;

import com.yunrui.edu.trade.application.CreateOrderCommand;
import com.yunrui.edu.trade.application.OrderApplicationService;
import com.yunrui.edu.trade.application.PaymentCallbackCommand;
import com.yunrui.edu.trade.application.PaymentCallbackResult;
import com.yunrui.edu.trade.application.PaymentCallbackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.yunrui.edu.trade.domain.PurchaseOrder;
import com.yunrui.edu.trade.domain.PurchaseOrderRepository;
import com.yunrui.edu.auth.UserAccessGuard;
import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderApplicationService orderService;
    private final PaymentCallbackService paymentService;
    private final PurchaseOrderRepository orderRepository;
    private final UserAccessGuard accessGuard;

    public OrderController(OrderApplicationService orderService,
                           PaymentCallbackService paymentService,
                           PurchaseOrderRepository orderRepository,
                           UserAccessGuard accessGuard) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.orderRepository = orderRepository;
        this.accessGuard = accessGuard;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse create(@Valid @RequestBody CreateOrderRequest request) {
        accessGuard.require(request.userId());
        return new IdResponse(orderService.create(new CreateOrderCommand(
                request.userId(), request.courseId(), request.couponIds())));
    }

    @GetMapping("/orders/{orderId}")
    public PurchaseOrder detail(@PathVariable long orderId) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));
        accessGuard.require(order.getUserId());
        return order;
    }

    @GetMapping("/users/{userId}/orders")
    public List<PurchaseOrder> list(@PathVariable long userId) {
        accessGuard.require(userId);
        return orderRepository.findByUserId(userId);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public void cancel(@PathVariable long orderId, @Valid @RequestBody CancelOrderRequest request) {
        accessGuard.require(request.userId());
        orderService.cancel(request.userId(), orderId);
    }

    @PostMapping("/payments/callback")
    public PaymentCallbackResult callback(@Valid @RequestBody PaymentCallbackRequest request) {
        return paymentService.confirm(new PaymentCallbackCommand(
                request.orderId(), request.transactionId(), request.paidAmount()));
    }

    public record CreateOrderRequest(@Positive long userId,
                                     @Positive long courseId,
                                     List<Long> couponIds) {
    }

    public record PaymentCallbackRequest(@Positive long orderId,
                                         @NotBlank String transactionId,
                                         @DecimalMin("0.00") java.math.BigDecimal paidAmount) {
    }

    public record IdResponse(long id) {
    }

    public record CancelOrderRequest(@Positive long userId) {
    }
}
