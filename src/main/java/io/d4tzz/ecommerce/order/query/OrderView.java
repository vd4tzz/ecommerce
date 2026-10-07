package io.d4tzz.ecommerce.order.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * {@code paymentExpired} is computed from the deadline at read time, so it is already true before the expiry
 * job cancels the order.
 */
public record OrderView(
    Long id,
    String orderCode,
    Long userId,
    String recipientName,
    String recipientPhone,
    String province,
    String district,
    String ward,
    String detailAddress,
    List<Item> items,
    BigDecimal subtotal,
    BigDecimal shippingFee,
    BigDecimal totalAmount,
    String status,
    String paymentStatus,
    String note,
    Instant paymentExpiresAt,
    boolean paymentExpired,
    Instant paidAt,
    String cancelReason,
    Instant cancelledAt,
    Instant completedAt,
    Instant createdAt,
    Instant updatedAt
) {
    public record Item(
        Long id,
        Long variantId,
        Long productId,
        String productName,
        String sku,
        String attributesDescription,
        String thumbnailUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal totalPrice
    ) {}
}
