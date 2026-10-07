package io.d4tzz.ecommerce.order.application;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.user.domain.UserId;
import io.d4tzz.ecommerce.order.application.command.CancelMyOrderCommand;
import io.d4tzz.ecommerce.payment.application.RequestRefundUseCase;
import io.d4tzz.ecommerce.payment.application.command.RequestRefundCommand;
import io.d4tzz.ecommerce.inventory.application.ReleaseStockUseCase;
import io.d4tzz.ecommerce.inventory.application.command.ReleaseStockCommand;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderCode;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import io.d4tzz.ecommerce.order.domain.event.OrderCancelledEvent;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Customer cancellation of their own order; the allowed statuses are decided by {@link Order#cancelByCustomer}. */
@Service
@Transactional
public class CancelMyOrderUseCase {

    private final OrderRepository orderRepository;
    private final ReleaseStockUseCase releaseStockUseCase;
    private final RequestRefundUseCase requestRefundUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public CancelMyOrderUseCase(
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

    /** Cancel an order, if the order was paid, a request for refund will be made. */
    public void execute(CancelMyOrderCommand command) {
        NotFoundException notFound = new NotFoundException("Order not found with code: " + command.orderCode());
        Order found = orderRepository.findByOrderCode(OrderCode.of(command.orderCode())).orElseThrow(() -> notFound);

        // Someone else's order is reported as missing, not as forbidden
        if (!found.getUserId().equals(UserId.of(command.userId()))) {
            throw notFound;
        }

        Order order = orderRepository.findForUpdate(found.getId()).orElseThrow(() -> notFound);
        if (!order.cancelByCustomer(command.reason(), Instant.now())) {
            return;
        }

        orderRepository.save(order);
        releaseStockUseCase.execute(new ReleaseStockCommand(order.getId().value()));

        // The customer had already paid: ask for the money to be given back
        if (order.qualifiesForRefund()) {
            requestRefundUseCase.execute(new RequestRefundCommand(
                Long.valueOf(order.getPaymentReference()),
                "Order " + order.getOrderCode() + " was cancelled after it was paid: " + order.getCancelReason()
            ));
        }
        eventPublisher.publishEvent(OrderCancelledEvent.of(order));
    }
}
