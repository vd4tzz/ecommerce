package io.d4tzz.ecommerce.order.application;

import io.d4tzz.ecommerce.inventory.application.ReleaseStockUseCase;
import io.d4tzz.ecommerce.inventory.application.command.ReleaseStockCommand;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderId;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import io.d4tzz.ecommerce.order.domain.event.OrderCancelledEvent;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cancels prepaid orders whose payment deadline has passed. Meant to run periodically; returns how many
 * orders were cancelled.
 *
 * <p>Each order is cancelled in its own transaction: an order that fails is logged and left for the next run,
 * and does not stop the others.
 */
@Service
public class ExpireOrdersUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExpireOrdersUseCase.class);

    private final OrderRepository orderRepository;
    private final ReleaseStockUseCase releaseStockUseCase;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public ExpireOrdersUseCase(
        OrderRepository orderRepository,
        ReleaseStockUseCase releaseStockUseCase,
        ApplicationEventPublisher eventPublisher,
        TransactionTemplate transactionTemplate
    ) {
        this.orderRepository = orderRepository;
        this.releaseStockUseCase = releaseStockUseCase;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * @return how many orders were cancelled in this run. An order that fails is logged and skipped, so this method
     *     does not throw for a single order.
     */
    public int execute() {
        Instant now = Instant.now();
        int expired = 0;

        for (OrderId orderId : orderRepository.findPaymentExpiredIds(now)) {
            try {
                if (Boolean.TRUE.equals(transactionTemplate.execute(status -> expire(orderId, now)))) {
                    expired++;
                }
            } catch (RuntimeException e) {
                log.error("Could not expire order {}; it will be tried again on the next run", orderId, e);
            }
        }
        return expired;
    }

    /** Returns false when the order no longer needs expiring. */
    private boolean expire(OrderId orderId, Instant now) {
        // Re-checked under lock: the payment may have arrived since the search
        Order order = orderRepository.findForUpdate(orderId).orElse(null);
        if (order == null || !order.expire(now)) {
            return false;
        }

        orderRepository.save(order);
        releaseStockUseCase.execute(new ReleaseStockCommand(order.getId().value()));
        eventPublisher.publishEvent(OrderCancelledEvent.of(order));
        return true;
    }
}
