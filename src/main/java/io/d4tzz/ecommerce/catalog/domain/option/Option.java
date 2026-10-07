package io.d4tzz.ecommerce.catalog.domain.option;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Option {

    private OptionId id;
    private OptionName name;
    private List<Value> values;

    public Option(OptionId id, OptionName name, List<Value> values) {
        this.id = id;
        this.name = name;
        this.values = values;
    }

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

    public void rename(OptionName newName, OptionNameUniqueChecker checker) {
        if (this.name.equals(newName)) {
            return;
        }
        if (checker.exists(newName)) {
            throw new ConflictException("Option name '" + newName.value() + "' already exists");
        }
        this.name = newName;
    }

    public Value addValue(ValueName valueName) {
        if (hasValueWithName(valueName)) {
            throw new ConflictException("Value '" + valueName.value() + "' already exists in option");
        }
        Value value = Value.create(valueName);
        this.values.add(value);
        return value;
    }

    public void removeValue(ValueId valueId, OptionValueUsageChecker checker) {
        if (checker.isReferenced(valueId)) {
            throw new InvalidOperationException("Cannot remove option value because it is currently in use");
        }
        this.values.removeIf(val -> valueId.equals(val.getId()));
    }

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

        valueToRename.rename(newName);
        return valueToRename;
    }

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
