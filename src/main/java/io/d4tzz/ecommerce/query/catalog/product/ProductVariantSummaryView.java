package io.d4tzz.ecommerce.query.catalog.product;

import java.math.BigDecimal;
import java.util.Map;

public record ProductVariantSummaryView(
    Long variantId,
    Long productId,
    String sku,
    String productName,
    String productSlug,
    String productStatus,
    BigDecimal price,
    String mediaUrl,
    Map<String, String> combination
) {}
