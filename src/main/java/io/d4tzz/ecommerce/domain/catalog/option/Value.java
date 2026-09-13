package io.d4tzz.ecommerce.domain.catalog.option;

public class Value {

    private ValueId id;
    private ValueName name;

    private Value(ValueId id, ValueName name) {
        this.id = id;
        this.name = name;
    }

    public static Value create(ValueName name) {
        return new Value(null, name);
    }

    public static Value restore(ValueId id, ValueName name) {
        return new Value(id, name);
    }

    void rename(ValueName name) {
        this.name = name;
    }

    public ValueId getId() {
        return id;
    }

    public ValueName getName() {
        return name;
    }
}
