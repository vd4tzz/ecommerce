package io.d4tzz.ecommerce.domain.catalog.option;

import io.d4tzz.ecommerce.common.Repository;
import java.util.Collection;

public interface OptionRepository extends Repository<Option, OptionId> {
    boolean existsOptionHasName(OptionName name);

    default boolean existsByName(OptionName name) {
        return existsOptionHasName(name);
    }

    boolean isValueReferenced(ValueId id);

    boolean isOptionReferenced(OptionId id);

    boolean existsById(OptionId id);

    boolean areValuesBelongToOption(
        OptionId optionId,
        Collection<ValueId> valueIds
    );
}
