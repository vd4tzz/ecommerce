package io.d4tzz.ecommerce.catalog.published;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import java.util.Collection;
import java.util.Set;

/** What catalog tells other modules about whether variants exist, whatever their product's status. */
public interface VariantExistenceChecker {
    /** Returns the given variant ids that do not exist in the catalog. */
    Set<VariantId> findMissing(Collection<VariantId> variantIds);
}
