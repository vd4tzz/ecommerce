package io.d4tzz.ecommerce.inventory.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class JpaReservationLine {

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public JpaReservationLine() {}

    public JpaReservationLine(Long variantId, int quantity) {
        this.variantId = variantId;
        this.quantity = quantity;
    }

    public Long getVariantId() {
        return variantId;
    }

    public int getQuantity() {
        return quantity;
    }
}
