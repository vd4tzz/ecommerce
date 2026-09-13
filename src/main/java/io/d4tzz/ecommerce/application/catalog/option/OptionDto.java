package io.d4tzz.ecommerce.application.catalog.option;

import io.d4tzz.ecommerce.domain.catalog.option.Option;
import io.d4tzz.ecommerce.domain.catalog.option.Value;
import java.util.List;

public record OptionDto(
    Long id,
    String name,
    List<ValueDto> values
) {
    public record ValueDto(Long id, String name) {
        public static ValueDto from(Value value) {
            return new ValueDto(
                value.getId() != null ? value.getId().value() : null,
                value.getName().value()
            );
        }

        public static List<ValueDto> fromList(List<Value> values) {
            if (values == null) return List.of();
            return values.stream().map(ValueDto::from).toList();
        }
    }

    public static OptionDto from(Option option) {
        return new OptionDto(
            option.getId() != null ? option.getId().value() : null,
            option.getName().value(),
            ValueDto.fromList(option.getValues())
        );
    }
}
