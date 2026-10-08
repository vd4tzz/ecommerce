package io.d4tzz.ecommerce.order.application;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.order.application.command.CancelOrderCommand;
import io.d4tzz.ecommerce.payment.application.RequestRefundUseCase;
import io.d4tzz.ecommerce.payment.application.command.RequestRefundCommand;
import io.d4tzz.ecommerce.inventory.application.ReleaseStockUseCase;
import io.d4tzz.ecommerce.inventory.application.command.ReleaseStockCommand;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderId;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import io.d4tzz.ecommerce.order.domain.event.OrderCancelledEvent;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin cancellation: allowed until the goods are handed over to the carrier. */
@Service
@Transactional
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final ReleaseStockUseCase releaseStockUseCase;
    private final RequestRefundUseCase requestRefundUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public CancelOrderUseCase(
        OrderRepository orderRepository,
        ReleaseStockUseCase releaseStockUseCase,
        RequestRefundUseCase requestRefundUseCase,
        ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.releaseStockUseCase = releaseStockUseCase;
        this.requestRefundUseCase = requestRefundUseCase;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Cancels the order. If it was already cancelled, nothing happens. Otherwise the reserved stock is released, and
     * if the customer had already paid, a refund is requested.
     *
     * @throws NotFoundException if the order does not exist
     * @throws InvalidArgumentException if the reason is blank
     * @throws InvalidOperationException if the order is already SHIPPING, DELIVERED or RETURNED
     */
    public void execute(CancelOrderCommand command) {
        Order order = orderRepository
            .findForUpdate(OrderId.of(command.orderId()))
            .orElseThrow(() -> new NotFoundException("Order not found with id: " + command.orderId()));

        if (!order.cancel(command.reason(), Instant.now())) {
            return;
        }

        orderRepository.save(order);
        releaseStockUseCase.execute(new ReleaseStockCommand(order.getId().value()));
        // If the customer had already paid: ask for the money to be given back
        if (order.qualifiesForRefund()) {
            requestRefundUseCase.execute(new RequestRefundCommand(
                Long.valueOf(order.getPaymentReference()),
                "Order " + order.getOrderCode() + " was cancelled after it was paid: " + order.getCancelReason()
            ));
        }
        eventPublisher.publishEvent(OrderCancelledEvent.of(order));
    }
}
