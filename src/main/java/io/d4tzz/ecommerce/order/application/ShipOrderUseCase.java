package io.d4tzz.ecommerce.order.application;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.order.application.command.ShipOrderCommand;
import io.d4tzz.ecommerce.order.application.port.StockReservations;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderId;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import io.d4tzz.ecommerce.order.domain.event.OrderShippedEvent;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The goods were handed over to the carrier. */
@Service
@Transactional
public class ShipOrderUseCase {

    private final OrderRepository orderRepository;
    private final StockReservations stockReservations;
    private final ApplicationEventPublisher eventPublisher;

    public ShipOrderUseCase(
        OrderRepository orderRepository,
        StockReservations stockReservations,
        ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.stockReservations = stockReservations;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ShipOrderCommand command) {
        Order order = orderRepository
            .findForUpdate(OrderId.of(command.orderId()))
            .orElseThrow(() -> new NotFoundException("Order not found with id: " + command.orderId()));

        Instant now = Instant.now();
        if (!order.markShipping(now)) {
            return;
        }

        orderRepository.save(order);
        stockReservations.commit(order.getId());
        eventPublisher.publishEvent(new OrderShippedEvent(order.getId(), order.getOrderCode(), order.getUserId(), now));
    }
}
