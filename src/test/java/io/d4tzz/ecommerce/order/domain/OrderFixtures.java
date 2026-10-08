package io.d4tzz.ecommerce.order.domain;

import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.user.domain.PhoneNumber;
import io.d4tzz.ecommerce.user.domain.UserId;
import java.time.Instant;
import java.util.List;

public final class OrderFixtures {

    public static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    public static final Instant PAY_BY = NOW.plusSeconds(15 * 60);
    public static final Long ORDER_ID = 1L;
    public static final Long USER_ID = 7L;
    public static final OrderCode CODE = OrderCode.of("ORD260110ABCDE");
    /** 2 x 100000 */
    public static final Money SUBTOTAL = Money.of(200_000);
    public static final Money SHIPPING_FEE = Money.of(30_000);
    public static final Money TOTAL = Money.of(230_000);

    private OrderFixtures() {}

    public static OrderItem item(long unitPrice, int quantity) {
        return OrderItem.create(
            VariantId.of(1L),
            ProductId.of(10L),
            "Shirt",
            "SKU-1",
            "M / Red",
            null,
            Money.of(unitPrice),
            quantity
        );
    }

    public static Recipient recipient() {
        return new Recipient("Jane Doe", PhoneNumber.of("0901234567"));
    }

    public static Address address() {
        return Address.of("Ha Noi", "Cau Giay", "Dich Vong", "1 Xuan Thuy");
    }

    /** A new prepaid order: PENDING, UNPAID, to be paid within 15 minutes of {@link #NOW}. */
    public static Order newPrepaid() {
        return Order.createPrepaid(
            CODE,
            UserId.of(USER_ID),
            recipient(),
            address(),
            List.of(item(100_000, 2)),
            SHIPPING_FEE,
            null,
            PAY_BY,
            NOW
        );
    }

    /** A new cash-on-delivery order: PENDING, UNPAID. */
    public static Order newCod() {
        return Order.createCashOnDelivery(
            CODE,
            UserId.of(USER_ID),
            recipient(),
            address(),
            List.of(item(100_000, 2)),
            SHIPPING_FEE,
            null,
            NOW
        );
    }

    /** An order as loaded from the database, so it has an id. {@code paymentExpiresAt} null means cash on delivery. */
    public static Order restored(
        OrderStatus status,
        PaymentStatus paymentStatus,
        Instant paymentExpiresAt,
        String paymentReference
    ) {
        return Order.restore(
            OrderId.of(ORDER_ID),
            CODE,
            UserId.of(USER_ID),
            recipient(),
            address(),
            List.of(item(100_000, 2)),
            SHIPPING_FEE,
            TOTAL,
            status,
            paymentStatus,
            null,
            paymentExpiresAt,
            paymentStatus == PaymentStatus.UNPAID ? null : NOW,
            paymentReference,
            status == OrderStatus.CANCELLED ? "Changed my mind" : null,
            status == OrderStatus.CANCELLED ? NOW : null,
            null,
            NOW,
            NOW
        );
    }

    public static Order codOrder(OrderStatus status) {
        return restored(status, PaymentStatus.UNPAID, null, null);
    }

    public static Order prepaidUnpaid(OrderStatus status) {
        return restored(status, PaymentStatus.UNPAID, PAY_BY, null);
    }

    /**
     * A PENDING, UNPAID prepaid order whose deadline is an hour after the real clock. Use cases read
     * {@code Instant.now()}, so they need this instead of {@link #prepaidUnpaid}, whose deadline is in the past.
     */
    public static Order prepaidUnpaidWithTimeLeft() {
        return restored(OrderStatus.PENDING, PaymentStatus.UNPAID, Instant.now().plusSeconds(3600), null);
    }

    public static Order prepaidPaid(OrderStatus status, String paymentReference) {
        return restored(status, PaymentStatus.PAID, PAY_BY, paymentReference);
    }
}
