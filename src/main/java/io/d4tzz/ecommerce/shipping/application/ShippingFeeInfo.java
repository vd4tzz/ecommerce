package io.d4tzz.ecommerce.shipping.application;

import io.d4tzz.ecommerce.domain.user.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * What to price: where the order goes, which carrier delivers it, and the packages (one per warehouse the
 * order ships from).
 */
public record ShippingFeeInfo(
    @NotNull Address destination,
    @NotBlank String carrier,
    @NotEmpty List<@Valid Package> packages
) {
    public record Package(@NotNull Long warehouseId, @NotEmpty List<@Valid Item> items) {}

    public record Item(@NotNull Long variantId, @Positive int quantity) {}
}
