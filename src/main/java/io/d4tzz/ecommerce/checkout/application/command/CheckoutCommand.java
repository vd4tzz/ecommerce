package io.d4tzz.ecommerce.checkout.application.command;

import io.d4tzz.ecommerce.order.application.command.PlaceOrderCommand.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/** What the customer buys, where it goes, who delivers it and how it is paid. Prices come from the catalog. */
public record CheckoutCommand(
    @NotNull Long userId,
    @NotBlank String recipientName,
    @NotBlank String recipientPhone,
    @NotBlank String province,
    @NotBlank String district,
    @NotBlank String ward,
    @NotBlank String detailAddress,
    @NotBlank String carrier,
    @NotEmpty List<@Valid Item> items,
    @NotNull PaymentMode paymentMode,
    @Size(max = 500) String note
) {
    public record Item(@NotNull Long variantId, @Positive int quantity) {}
}
