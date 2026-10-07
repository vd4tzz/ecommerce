package io.d4tzz.ecommerce.order.application;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.order.application.command.ReturnOrderCommand;
import io.d4tzz.ecommerce.payment.application.RequestRefundUseCase;
import io.d4tzz.ecommerce.payment.application.command.RequestRefundCommand;
import io.d4tzz.ecommerce.inventory.application.RestockReturnedUseCase;
import io.d4tzz.ecommerce.inventory.application.command.RestockReturnedCommand;
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
    private final RestockReturnedUseCase restockReturnedUseCase;
    private final RequestRefundUseCase requestRefundUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public ReturnOrderUseCase(
        OrderRepository orderRepository,
        RestockReturnedUseCase restockReturnedUseCase,
        RequestRefundUseCase requestRefundUseCase,
        ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.restockReturnedUseCase = restockReturnedUseCase;
        this.requestRefundUseCase = requestRefundUseCase;
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
        restockReturnedUseCase.execute(new RestockReturnedCommand(order.getId().value()));
        // The customer had already paid: ask for the money to be given back
        if (order.qualifiesForRefund()) {
            requestRefundUseCase.execute(new RequestRefundCommand(
                Long.valueOf(order.getPaymentReference()),
                "Order " + order.getOrderCode() + " was returned after it was paid"
            ));
        }
        eventPublisher.publishEvent(new OrderReturnedEvent(order.getId(), order.getOrderCode(), order.getUserId(), now));
    }
}
