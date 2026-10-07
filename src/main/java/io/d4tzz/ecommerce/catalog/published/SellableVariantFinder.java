package io.d4tzz.ecommerce.catalog.published;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/** What catalog tells other modules about the variants that can be bought. */
public interface SellableVariantFinder {
    /** What a variant looks like to a buyer, as it is now. */
    record SellableVariant(
        Long productId,
        String productName,
        String sku,
        String optionsDescription,
        String thumbnailUrl,
        BigDecimal price
    ) {}

    /** The variants that can be bought, by id. Variants that do not exist or are not published are left out. */
    Map<VariantId, SellableVariant> findSellable(Collection<VariantId> variantIds);
}
