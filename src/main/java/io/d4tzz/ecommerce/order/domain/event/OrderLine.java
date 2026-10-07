package io.d4tzz.ecommerce.order.domain.event;

import io.d4tzz.ecommerce.domain.catalog.product.VariantId;

public record OrderLine(VariantId variantId, int quantity) {}
