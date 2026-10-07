package io.d4tzz.ecommerce.order.application.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Built by checkout: prices, shipping fee and item snapshots are already resolved.
 *
 * <p>{@code paymentExpiresAt} is required for {@link PaymentMode#PREPAID} and ignored for COD.
 */
public record PlaceOrderCommand(
    @NotNull Long userId,
    @NotBlank String recipientName,
    @NotBlank String recipientPhone,
    @NotBlank String province,
    @NotBlank String district,
    @NotBlank String ward,
    @NotBlank String detailAddress,
    @NotEmpty List<@Valid Item> items,
    @NotNull @PositiveOrZero BigDecimal shippingFee,
    @NotNull PaymentMode paymentMode,
    Instant paymentExpiresAt,
    @Size(max = 500) String note
) {
    public enum PaymentMode {
        COD,
        PREPAID,
    }

    public record Item(
        @NotNull Long variantId,
        @NotNull Long productId,
        @NotBlank String productName,
        @NotBlank String sku,
        @NotNull String attributesDescription,
        String thumbnailUrl,
        @NotNull @PositiveOrZero BigDecimal unitPrice,
        @Positive int quantity
    ) {}
}
