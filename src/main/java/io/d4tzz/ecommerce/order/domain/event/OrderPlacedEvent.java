package io.d4tzz.ecommerce.order.domain.event;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.order.domain.OrderCode;
import io.d4tzz.ecommerce.order.domain.OrderId;
import io.d4tzz.ecommerce.domain.user.UserId;
import java.time.Instant;
import java.util.List;

public record OrderPlacedEvent(
    OrderId orderId,
    OrderCode orderCode,
    UserId userId,
    List<OrderLine> lines,
    Money totalAmount,
    Instant occurredAt
) {
    public OrderPlacedEvent {
        lines = List.copyOf(lines);
    }
}
