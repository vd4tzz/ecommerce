package io.d4tzz.ecommerce.order.domain.event;

import io.d4tzz.ecommerce.domain.user.UserId;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderCode;
import io.d4tzz.ecommerce.order.domain.OrderId;
import io.d4tzz.ecommerce.order.domain.PaymentStatus;
import java.time.Instant;
import java.util.List;

public record OrderCancelledEvent(
    OrderId orderId,
    OrderCode orderCode,
    UserId userId,
    List<OrderLine> lines,
    PaymentStatus paymentStatus,
    String reason,
    Instant occurredAt
) {
    public OrderCancelledEvent {
        lines = List.copyOf(lines);
    }

    public static OrderCancelledEvent of(Order order) {
        return new OrderCancelledEvent(
            order.getId(),
            order.getOrderCode(),
            order.getUserId(),
            order.lines(),
            order.getPaymentStatus(),
            order.getCancelReason(),
            order.getCancelledAt()
        );
    }
}
