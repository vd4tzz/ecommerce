package io.d4tzz.ecommerce.shipping.application.port;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import java.util.Collection;
import java.util.Map;

/** Where shipping looks up product information from the catalog. */
public interface VariantLookup {
    /** Weight and dimensions of one unit of a variant. */
    record Measurements(int weightGrams, int lengthCm, int widthCm, int heightCm) {}

    /** The measurements of each variant, by id. Variants that do not exist or have none are left out. */
    Map<VariantId, Measurements> measurementsOf(Collection<VariantId> variantIds);
}
