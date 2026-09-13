package io.d4tzz.ecommerce.domain.catalog.category;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Attribute {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z0-9_]{2,50}$");

    private String code;
    private String label;
    private AttributeType type;
    private List<AttributeValue> values;

    private Attribute(String code, String label, AttributeType type, List<AttributeValue> values) {
        this.code = validateCode(code);
        this.label = validateLabel(label);
        this.type = type;
        this.values = validateOptions(values);
    }

    public static Attribute create(String code, String label, AttributeType type, List<AttributeValue> values) {
        return new Attribute(code, label, type, values);
    }

    public static Attribute restore(String code, String label, AttributeType type, List<AttributeValue> values) {
        return new Attribute(code, label, type, values);
    }

    public Set<String> getCodes() {
        return this.values.stream().map(AttributeValue::code).collect(Collectors.toUnmodifiableSet());
    }

    public void validateValue(Object value) {
        if (value == null) {
            throw new InvalidArgumentException("Attribute '" + this.label + "' is required");
        }
        if (value instanceof String s && s.isBlank()) {
            throw new InvalidArgumentException("Attribute '" + this.label + "' cannot be blank");
        }
        if (value instanceof Collection<?> c && c.isEmpty()) {
            throw new InvalidArgumentException("Attribute '" + this.label + "' must contain at least one value");
        }

        Set<String> validCodes = getCodes();
        switch (this.type) {
            case SELECT -> {
                if (!(value instanceof String strVal)) {
                    throw new InvalidArgumentException("Attribute '" + this.label + "' must be a string identifier");
                }
                if (!validCodes.contains(strVal)) {
                    throw new InvalidArgumentException(
                        "Invalid value '" + strVal + "' for attribute '" + this.label + "'"
                    );
                }
            }
            case MULTI_SELECT -> {
                if (!(value instanceof Collection<?> list)) {
                    throw new InvalidArgumentException("Attribute '" + this.label + "' must be an array of values");
                }
                for (Object val : list) {
                    if (!(val instanceof String strVal)) {
                        throw new InvalidArgumentException("Elements in '" + this.label + "' must be strings");
                    }
                    if (!validCodes.contains(strVal)) {
                        throw new InvalidArgumentException(
                            "Invalid value '" + strVal + "' in attribute '" + this.label + "'"
                        );
                    }
                }
            }
        }
    }

    private static String validateCode(String code) {
        String trimmed = code.trim().toLowerCase();
        if (!CODE_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidArgumentException(
                "Attribute code '" +
                    code +
                    "' must contain only lowercase letters, digits, and underscores, between 2 and 50 characters"
            );
        }
        return trimmed;
    }

    private static String validateLabel(String label) {
        String trimmed = label.trim();
        if (trimmed.isBlank()) {
            throw new InvalidArgumentException("Attribute label cannot be blank");
        }
        return trimmed;
    }

    private static List<AttributeValue> validateOptions(List<AttributeValue> values) {
        if (values == null || values.isEmpty()) {
            throw new InvalidArgumentException("Attribute must have at least one value");
        }

        Set<String> seenIds = new HashSet<>();
        for (AttributeValue opt : values) {
            if (opt == null) {
                throw new InvalidArgumentException("Attribute value cannot be null");
            }
            if (!seenIds.add(opt.code())) {
                throw new ConflictException("Duplicate value: " + opt.code());
            }
        }
        return new ArrayList<>(values);
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public AttributeType getType() {
        return type;
    }

    public List<AttributeValue> getValues() {
        return Collections.unmodifiableList(values);
    }
}
