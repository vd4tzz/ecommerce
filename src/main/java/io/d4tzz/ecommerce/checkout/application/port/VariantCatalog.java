package io.d4tzz.ecommerce.checkout.application.port;

import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/** What checkout needs to know about the catalog. */
public interface VariantCatalog {
    /** What the order keeps of a variant, as it is at checkout. */
    record VariantSnapshot(
        Long productId,
        String productName,
        String sku,
        String optionsDescription,
        String thumbnailUrl,
        BigDecimal price
    ) {}

    /** The variants that can be bought, by id. Variants that do not exist or are not published are left out. */
    Map<VariantId, VariantSnapshot> findForSale(Collection<VariantId> variantIds);
}
