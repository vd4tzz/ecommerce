package io.d4tzz.ecommerce.order.application;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.order.application.command.ReturnOrderCommand;
import io.d4tzz.ecommerce.order.application.port.PaymentRefunds;
import io.d4tzz.ecommerce.order.application.port.StockReservations;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderId;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import io.d4tzz.ecommerce.order.domain.event.OrderReturnedEvent;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Delivery failed or the customer returned the goods, which came back to the warehouse. */
@Service
@Transactional
public class ReturnOrderUseCase {

    private final OrderRepository orderRepository;
    private final StockReservations stockReservations;
    private final PaymentRefunds paymentRefunds;
    private final ApplicationEventPublisher eventPublisher;

    public ReturnOrderUseCase(
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

    public void execute(ReturnOrderCommand command) {
        Order order = orderRepository
            .findForUpdate(OrderId.of(command.orderId()))
            .orElseThrow(() -> new NotFoundException("Order not found with id: " + command.orderId()));

        Instant now = Instant.now();
        if (!order.markReturned(now)) {
            return;
        }

        orderRepository.save(order);
        stockReservations.restock(order.getId());
        // The customer had already paid: ask for the money to be given back
        if (order.qualifiesForRefund()) {
            paymentRefunds.requestRefund(
                Long.valueOf(order.getPaymentReference()),
                "Order " + order.getOrderCode() + " was returned after it was paid"
            );
        }
        eventPublisher.publishEvent(new OrderReturnedEvent(order.getId(), order.getOrderCode(), order.getUserId(), now));
    }
}
