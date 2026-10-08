package io.d4tzz.ecommerce.order.application;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.order.application.command.ConfirmOrderCommand;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderId;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import io.d4tzz.ecommerce.order.domain.event.OrderConfirmedEvent;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The shop accepts the order and the warehouse may start picking. A prepaid order must be paid first. */
@Service
@Transactional
public class ConfirmOrderUseCase {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ConfirmOrderUseCase(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Confirms the order. If it was already confirmed, nothing happens.
     *
     * @throws NotFoundException if the order does not exist
     * @throws InvalidOperationException if the order is a prepaid order that is not paid yet, or is not PENDING
     */
    public void execute(ConfirmOrderCommand command) {
        Order order = orderRepository
            .findForUpdate(OrderId.of(command.orderId()))
            .orElseThrow(() -> new NotFoundException("Order not found with id: " + command.orderId()));

        Instant now = Instant.now();
        if (!order.confirm(now)) {
            return;
        }

        orderRepository.save(order);
        eventPublisher.publishEvent(new OrderConfirmedEvent(order.getId(), order.getOrderCode(), order.getUserId(), now));
    }
}
