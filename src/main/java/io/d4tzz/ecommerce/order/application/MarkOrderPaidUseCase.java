package io.d4tzz.ecommerce.order.application;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.order.application.command.MarkOrderPaidCommand;
import io.d4tzz.ecommerce.payment.application.RequestRefundUseCase;
import io.d4tzz.ecommerce.payment.application.command.RequestRefundCommand;
import io.d4tzz.ecommerce.order.domain.MarkPaidResult;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderCode;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import io.d4tzz.ecommerce.order.domain.event.OrderPaidEvent;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marks an order as paid after the payment module has received money for it.
 *
 * <p>If the order cannot accept the money (expired, cancelled, wrong amount, or already paid), this
 * is not treated as an error, because the customer has already paid. Instead, the payment is sent back for a
 * refund. We return a result rather than throw, since throwing would also undo the refund request.
 *
 * <p>Called by {@link io.d4tzz.ecommerce.order.inbound.listener.MarkOrderPaidOnPaymentReceivedListener}
 * when the payment module reports money received for an order.
 */
@Service
@Transactional
public class MarkOrderPaidUseCase {

    private static final Logger log = LoggerFactory.getLogger(MarkOrderPaidUseCase.class);

    private final OrderRepository orderRepository;
    private final RequestRefundUseCase requestRefundUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public MarkOrderPaidUseCase(
        OrderRepository orderRepository,
        RequestRefundUseCase requestRefundUseCase,
        ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.requestRefundUseCase = requestRefundUseCase;
        this.eventPublisher = eventPublisher;
    }

    public MarkPaidResult execute(MarkOrderPaidCommand command) {
        Optional<Order> found = orderRepository.findForUpdateByOrderCode(OrderCode.of(command.orderCode()));
        if (found.isEmpty()) {
            // Probably a mistyped code: keep the money unmatched for a person to assign, do not refund it
            log.warn("Payment {} is for unknown order {}: left unmatched", command.paymentId(), command.orderCode());
            return MarkPaidResult.ORDER_NOT_FOUND;
        }
        Order order = found.get();

        Instant now = Instant.now();
        MarkPaidResult result = order.markPaid(command.paymentId().toString(), Money.of(command.amount()), now);

        if (result == MarkPaidResult.ACCEPTED) {
            orderRepository.save(order);
            eventPublisher.publishEvent(
                new OrderPaidEvent(order.getId(), order.getOrderCode(), order.getUserId(), now)
            );
        } else if (result.isRejected()) {
            requestRefundUseCase.execute(new RequestRefundCommand(
                command.paymentId(),
                "Order " + order.getOrderCode() + " did not accept the payment: " + result
            ));
        }
        return result;
    }
}
