package io.d4tzz.ecommerce.order.domain.event;

import io.d4tzz.ecommerce.user.domain.UserId;
import io.d4tzz.ecommerce.order.domain.OrderCode;
import io.d4tzz.ecommerce.order.domain.OrderId;
import java.time.Instant;

public record OrderRefundedEvent(OrderId orderId, OrderCode orderCode, UserId userId, Instant occurredAt) {}
