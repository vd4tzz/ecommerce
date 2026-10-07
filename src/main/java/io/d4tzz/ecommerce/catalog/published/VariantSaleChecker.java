package io.d4tzz.ecommerce.catalog.published;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import java.util.Collection;
import java.util.Set;

/** What catalog tells other modules about whether variants can be bought. */
public interface VariantSaleChecker {
    /** Returns the given variants that cannot be bought: they do not exist or their product is not published. */
    Set<VariantId> checkNotForSale(Collection<VariantId> variantIds);
}
