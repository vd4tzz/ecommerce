package io.d4tzz.ecommerce.cart.application.port;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import java.util.Collection;
import java.util.Set;

/** What cart needs to know about the catalog. */
public interface VariantCatalog {
    /** Returns the given variants that cannot be bought: they do not exist or their product is not published. */
    Set<VariantId> findNotForSale(Collection<VariantId> variantIds);
}
