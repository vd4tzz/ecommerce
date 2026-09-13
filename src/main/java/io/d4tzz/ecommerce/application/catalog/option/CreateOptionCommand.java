package io.d4tzz.ecommerce.application.catalog.option;

import java.util.List;

public record CreateOptionCommand(String name, List<String> values) {
    public CreateOptionCommand {
        values = values == null ? List.of() : values;
    }
}
