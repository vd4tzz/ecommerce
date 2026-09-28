package io.d4tzz.ecommerce.query.catalog.option;

import java.util.List;

public record OptionDetailView(Long id, String name, Boolean inUse, List<Value> values) {
    public record Value(Long id, String name, Boolean inUse) {}
}
