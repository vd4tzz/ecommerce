package io.d4tzz.ecommerce.query.catalog.option;

import java.util.List;

public record OptionTableRowView(
    Long id,
    String name,
    int valueCount,
    List<String> previewValues
) {}
