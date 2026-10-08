package io.d4tzz.ecommerce.order.application;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.order.application.command.MarkOrderRefundedCommand;
import io.d4tzz.ecommerce.order.domain.MarkRefundedResult;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderCode;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import io.d4tzz.ecommerce.order.domain.event.OrderRefundedEvent;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies a full refund recorded by the payment module. Never throws for an unexpected refund: it is reported
 * in the result (and logged when a person should look at it), so the triggering event is not retried forever.
 */
@Service
@Transactional
public class MarkOrderRefundedUseCase {

    private static final Logger log = LoggerFactory.getLogger(MarkOrderRefundedUseCase.class);

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MarkOrderRefundedUseCase(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * @return what happened to the refund; an unexpected refund is reported here (and logged when a person should
     *     look at it), not thrown
     * @throws InvalidArgumentException if the order code is blank
     */
    public MarkRefundedResult execute(MarkOrderRefundedCommand command) {
        Optional<Order> found = orderRepository.findForUpdateByOrderCode(OrderCode.of(command.orderCode()));
        if (found.isEmpty()) {
            // e.g. the refund of a payment made for a mistyped order code
            return MarkRefundedResult.ORDER_NOT_FOUND;
        }
        Order order = found.get();

        Instant now = Instant.now();
        MarkRefundedResult result = order.markRefunded(command.paymentId().toString(), now);

        if (result == MarkRefundedResult.REFUNDED) {
            orderRepository.save(order);
            eventPublisher.publishEvent(
                new OrderRefundedEvent(order.getId(), order.getOrderCode(), order.getUserId(), now)
            );
        } else if (result == MarkRefundedResult.ORDER_STILL_ACTIVE) {
            log.warn(
                "Payment {} of order {} was refunded while the order is {}: needs a manual check",
                command.paymentId(),
                order.getOrderCode(),
                order.getStatus()
            );
        }
        return result;
    }
}
