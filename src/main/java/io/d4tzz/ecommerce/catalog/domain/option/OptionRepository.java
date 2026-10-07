package io.d4tzz.ecommerce.catalog.domain.option;

import io.d4tzz.ecommerce.common.Repository;
import java.util.Collection;

public interface OptionRepository extends Repository<Option, OptionId> {
    boolean existsByName(OptionName name);

    boolean isValueReferenced(ValueId id);

    boolean isOptionReferenced(OptionId id);

    boolean existsById(OptionId id);

    boolean areValuesBelongToOption(OptionId optionId, Collection<ValueId> valueIds);
}
