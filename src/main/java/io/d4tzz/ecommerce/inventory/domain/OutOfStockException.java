package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;

/** A variant has fewer sellable units than an order wants. Answered with a 409 over HTTP. */
public class OutOfStockException extends InvalidOperationException {

    public OutOfStockException(VariantId variantId, int wanted, int available) {
        super("Variant " + variantId + " is out of stock: " + wanted + " wanted, " + available + " available");
    }
}
