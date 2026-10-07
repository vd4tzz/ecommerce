package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;

public record ReservationLine(VariantId variantId, int quantity) {
    public ReservationLine {
        if (quantity <= 0) {
            throw new InvalidArgumentException("Reservation quantity must be greater than zero");
        }
    }
}
