package io.d4tzz.ecommerce.order.domain.event;

import io.d4tzz.ecommerce.order.domain.OrderCode;
import io.d4tzz.ecommerce.order.domain.OrderId;
import io.d4tzz.ecommerce.domain.user.UserId;
import java.time.Instant;

public record OrderPaidEvent(OrderId orderId, OrderCode orderCode, UserId userId, Instant occurredAt) {}
