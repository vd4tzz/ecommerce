package io.d4tzz.ecommerce.application.cart;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

public interface CartProductDetailProvider {

    record VariantDetail(
        Long variantId,
        String sku,
        String productName,
        String productSlug,
        String mediaUrl,
        BigDecimal price,
        boolean active
    ) {}

    Map<Long, VariantDetail> getVariantDetails(Collection<Long> variantIds);
}
