package io.d4tzz.ecommerce.inventory.application.port;

import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import java.util.Collection;
import java.util.Set;

/** What inventory needs to know about the catalog. */
public interface VariantCatalog {
    /** Returns the given variant ids that do not exist in the catalog. */
    Set<VariantId> findMissing(Collection<VariantId> variantIds);
}
