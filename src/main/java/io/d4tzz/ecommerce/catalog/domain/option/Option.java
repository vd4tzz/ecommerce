package io.d4tzz.ecommerce.catalog.domain.option;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A product option such as "Color" or "Size", with the list of {@link Value}s it can take
 * (for example "Red", "Blue").
 *
 * <p>Option names are unique across the catalog, and value names are unique within an option.
 */
public class Option {

    private OptionId id;
    private OptionName name;
    private List<Value> values;

    /**
     * Rebuilds an option from persisted data without any validation.
     *
     * <p>To create a new option use {@link #create}.
     *
     * @param id     option id, {@code null} if not yet persisted
     * @param values the option's values; the list must be mutable
     */
    public Option(OptionId id, OptionName name, List<Value> values) {
        this.id = id;
        this.name = name;
        this.values = new ArrayList<>(values);
    }

    /**
     * Creates a new, not yet persisted option.
     *
     * @param name       option name, must be unused
     * @param valueNames names of the initial values, may be empty
     * @param checker    tells whether an option name is already taken
     * @return the new option, without an id
     * @throws ConflictException if the name already exists or {@code valueNames} contains duplicates
     */
    public static Option create(OptionName name, List<ValueName> valueNames, OptionNameUniqueChecker checker) {
        if (checker.exists(name)) {
            throw new ConflictException("Option name '" + name.value() + "' already exists");
        }
        Option option = new Option(null, name, new ArrayList<>());
        for (ValueName valueName : valueNames) {
            option.addValue(valueName);
        }
        return option;
    }

    /**
     * Renames this option. Does nothing if the name is unchanged.
     *
     * @param newName the new name
     * @param checker tells whether an option name is already taken
     * @throws ConflictException if another option already uses the new name
     */
    public void rename(OptionName newName, OptionNameUniqueChecker checker) {
        if (this.name.equals(newName)) {
            return;
        }
        if (checker.exists(newName)) {
            throw new ConflictException("Option name '" + newName.value() + "' already exists");
        }
        this.name = newName;
    }

    /**
     * Adds a new value to this option.
     *
     * @param valueName name of the new value
     * @return the added value, without an id until the option is saved
     * @throws ConflictException if this option already has a value with that name
     */
    public Value addValue(ValueName valueName) {
        if (hasValueWithName(valueName)) {
            throw new ConflictException("Value '" + valueName.value() + "' already exists in option");
        }
        Value value = Value.create(valueName);
        this.values.add(value);
        return value;
    }

    /**
     * Removes a value from this option. Does nothing if the option has no such value.
     *
     * @param valueId id of the value to remove
     * @param checker tells whether a value is in use, for example by a product
     * @throws InvalidOperationException if the value is in use
     */
    public void removeValue(ValueId valueId, OptionValueUsageChecker checker) {
        if (checker.isReferenced(valueId)) {
            throw new InvalidOperationException("Cannot remove option value because it is currently in use");
        }
        this.values.removeIf(val -> valueId.equals(val.getId()));
    }

    /**
     * Renames a value of this option. Does nothing if the name is unchanged.
     *
     * @param valueId id of the value to rename
     * @param newName the new name
     * @return the renamed value
     * @throws InvalidArgumentException if this option has no value with that id
     * @throws ConflictException        if another value of this option already has the new name
     */
    public Value renameValue(ValueId valueId, ValueName newName) {
        Value valueToRename = null;
        for (Value value : values) {
            if (value.getId().equals(valueId)) {
                valueToRename = value;
            } else if (value.getName().equals(newName)) {
                throw new ConflictException("Value '" + newName.value() + "' already exists in option");
            }
        }

        if (valueToRename == null) {
            throw new InvalidArgumentException("Value not found");
        }

        if (valueToRename.getName().equals(newName)) {
            return valueToRename;
        }

        valueToRename.rename(newName);
        return valueToRename;
    }

    /** Tells whether this option already has a value with the given name. */
    public boolean hasValueWithName(ValueName valueName) {
        return this.values.stream().anyMatch(val -> val.getName().equals(valueName));
    }

    public OptionId getId() {
        return id;
    }

    public OptionName getName() {
        return name;
    }

    public List<Value> getValues() {
        return Collections.unmodifiableList(values);
    }
}
