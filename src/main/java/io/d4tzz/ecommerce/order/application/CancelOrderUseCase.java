package io.d4tzz.ecommerce.order.application;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.order.application.command.CancelOrderCommand;
import io.d4tzz.ecommerce.order.application.port.PaymentRefunds;
import io.d4tzz.ecommerce.order.application.port.StockReservations;
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
    private final StockReservations stockReservations;
    private final PaymentRefunds paymentRefunds;
    private final ApplicationEventPublisher eventPublisher;

    public CancelOrderUseCase(
        OrderRepository orderRepository,
        StockReservations stockReservations,
        PaymentRefunds paymentRefunds,
        ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.stockReservations = stockReservations;
        this.paymentRefunds = paymentRefunds;
        this.eventPublisher = eventPublisher;
    }

    public void execute(CancelOrderCommand command) {
        Order order = orderRepository
            .findForUpdate(OrderId.of(command.orderId()))
            .orElseThrow(() -> new NotFoundException("Order not found with id: " + command.orderId()));

        if (!order.cancel(command.reason(), Instant.now())) {
            return;
        }

        orderRepository.save(order);
        stockReservations.release(order.getId());
        // If the customer had already paid: ask for the money to be given back
        if (order.qualifiesForRefund()) {
            paymentRefunds.requestRefund(
                Long.valueOf(order.getPaymentReference()),
                "Order " + order.getOrderCode() + " was cancelled after it was paid: " + order.getCancelReason()
            );
        }
        eventPublisher.publishEvent(OrderCancelledEvent.of(order));
    }
}
