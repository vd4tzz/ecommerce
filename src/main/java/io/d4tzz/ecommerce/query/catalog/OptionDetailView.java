package io.d4tzz.ecommerce.query.catalog;

import java.util.List;

public record OptionDetailView(
    Long id,
    String name,
    Boolean inUse,
    List<OptionValueView> values
) {
    public record OptionValueView(
        Long id,
        String name,
        Boolean inUse
    ) {}
}
