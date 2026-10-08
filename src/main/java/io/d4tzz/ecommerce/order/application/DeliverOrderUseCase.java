package io.d4tzz.ecommerce.order.application;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.order.application.command.DeliverOrderCommand;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderId;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import io.d4tzz.ecommerce.order.domain.PaymentStatus;
import io.d4tzz.ecommerce.order.domain.event.OrderDeliveredEvent;
import io.d4tzz.ecommerce.payment.application.RecordPaymentUseCase;
import io.d4tzz.ecommerce.payment.application.command.RecordPaymentCommand;
import io.d4tzz.ecommerce.payment.domain.PaymentMethod;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The carrier delivered the goods. Called by an admin for now, later by the carrier's webhook.
 *
 * <p>For a COD order the customer paid the shipper on delivery, so the cash is recorded as a payment in the same
 * transaction; the order then becomes PAID like for any other payment.
 */
@Service
@Transactional
public class DeliverOrderUseCase {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RecordPaymentUseCase recordPaymentUseCase;

    public DeliverOrderUseCase(
        OrderRepository orderRepository,
        RecordPaymentUseCase recordPaymentUseCase,
        ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.recordPaymentUseCase = recordPaymentUseCase;
    }

    /**
     * Marks the order as delivered. If it was already delivered, nothing happens.
     *
     * @throws NotFoundException if the order does not exist
     * @throws InvalidOperationException if the order is not SHIPPING
     */
    public void execute(DeliverOrderCommand command) {
        Order order = orderRepository
            .findForUpdate(OrderId.of(command.orderId()))
            .orElseThrow(() -> new NotFoundException("Order not found with id: " + command.orderId()));

        Instant now = Instant.now();
        if (!order.markDelivered(now)) {
            return;
        }

        orderRepository.save(order);

        // COD with something to collect (a free order is already PAID)
        if (!order.requiresPrepayment() && order.getPaymentStatus() == PaymentStatus.UNPAID) {
            // Cash has no shop account and no transaction number
            recordPaymentUseCase.execute(
                new RecordPaymentCommand(
                    order.getOrderCode().toString(),
                    PaymentMethod.COD,
                    null,
                    order.getTotalAmount().amount(),
                    null,
                    "Collected on delivery"
                )
            );
        }

        eventPublisher.publishEvent(
            new OrderDeliveredEvent(order.getId(), order.getOrderCode(), order.getUserId(), now)
        );
    }
}
