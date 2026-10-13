package io.d4tzz.ecommerce.catalog.domain.option;

import io.d4tzz.ecommerce.common.Repository;
import java.util.Collection;

/**
 * Persistence port for {@link Option}.
 *
 * <p>Besides the basic {@code find}, {@code save} and {@code delete}, it exposes the existence and
 * usage checks the option use cases need.
 */
public interface OptionRepository extends Repository<Option, OptionId> {
    /** Tells whether any option already uses the given name. */
    boolean existsByName(OptionName name);

    /** Tells whether the option value is selected by any product. */
    boolean isValueReferenced(ValueId id);

    /** Tells whether any product uses the option. */
    boolean isOptionReferenced(OptionId id);

    /** Tells whether an option with the given id exists. */
    boolean existsById(OptionId id);

    /**
     * Tells whether every given value belongs to the given option. Duplicate ids are counted once.
     *
     * @return {@code true} if all values belong to the option, or if {@code valueIds} is empty
     */
    boolean areValuesBelongToOption(OptionId optionId, Collection<ValueId> valueIds);
}
