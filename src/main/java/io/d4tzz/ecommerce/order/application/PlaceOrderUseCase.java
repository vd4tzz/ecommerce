package io.d4tzz.ecommerce.order.application;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.user.domain.PhoneNumber;
import io.d4tzz.ecommerce.user.domain.UserId;
import io.d4tzz.ecommerce.order.application.command.PlaceOrderCommand;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderCode;
import io.d4tzz.ecommerce.order.domain.OrderItem;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import io.d4tzz.ecommerce.order.domain.Recipient;
import io.d4tzz.ecommerce.order.domain.event.OrderPlacedEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the order. Called by checkout, which also reserves stock in the same transaction, so a failed
 * reservation rolls the order back. Returns the new order id.
 */
@Service
@Transactional
public class PlaceOrderUseCase {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PlaceOrderUseCase(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * @return the id of the new order
     * @throws InvalidArgumentException if the recipient, phone number or address is invalid, there are no items, an
     *     item has a blank name or a quantity of zero, or a prepaid order has no payment expiry or one that is not
     *     in the future
     */
    public Long execute(PlaceOrderCommand command) {
        Instant now = Instant.now();
        UserId userId = UserId.of(command.userId());
        Recipient recipient = new Recipient(command.recipientName(), PhoneNumber.of(command.recipientPhone()));
        Address address = Address.of(command.province(), command.district(), command.ward(), command.detailAddress());
        List<OrderItem> items = command.items().stream().map(PlaceOrderUseCase::toOrderItem).toList();
        Money shippingFee = Money.of(command.shippingFee());

        // A duplicate order code fails on the unique constraint
        Order order = switch (command.paymentMode()) {
            case COD -> Order.createCashOnDelivery(
                OrderCode.generate(),
                userId,
                recipient,
                address,
                items,
                shippingFee,
                command.note(),
                now
            );
            case PREPAID -> {
                if (command.paymentExpiresAt() == null) {
                    throw new InvalidArgumentException("Payment expiry is required for prepaid orders");
                }
                yield Order.createPrepaid(
                    OrderCode.generate(),
                    userId,
                    recipient,
                    address,
                    items,
                    shippingFee,
                    command.note(),
                    command.paymentExpiresAt(),
                    now
                );
            }
        };

        order = orderRepository.save(order);

        eventPublisher.publishEvent(
            new OrderPlacedEvent(
                order.getId(),
                order.getOrderCode(),
                order.getUserId(),
                order.lines(),
                order.getTotalAmount(),
                now
            )
        );
        return order.getId().value();
    }

    private static OrderItem toOrderItem(PlaceOrderCommand.Item item) {
        return OrderItem.create(
            VariantId.of(item.variantId()),
            ProductId.of(item.productId()),
            item.productName(),
            item.sku(),
            item.optionsDescription(),
            item.thumbnailUrl(),
            Money.of(item.unitPrice()),
            item.quantity()
        );
    }
}
