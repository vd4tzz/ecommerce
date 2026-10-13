package io.d4tzz.ecommerce.catalog.domain.category;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A product attribute defined by a {@link Category}, such as "material" or "color".
 *
 * <p>An attribute has a code (its identifier within a category tree), a display label, a
 * {@link AttributeType} and a non-empty list of allowed {@link AttributeValue}s. Products in the
 * category must choose their values for this attribute from that list.
 */
public class Attribute {

    /** Valid attribute code: 2 to 50 characters, lowercase letters, digits or underscores only. */
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

    /**
     * Creates a new attribute.
     *
     * <p>The code is trimmed and lowercased, and the label is trimmed.
     *
     * @param code   attribute code, 2 to 50 characters of lowercase letters, digits or underscores
     * @param label  display label, must not be blank
     * @param type   whether a product selects one value or several
     * @param values allowed values, at least one, with distinct codes
     * @return the new attribute
     * @throws InvalidArgumentException if the code has an invalid format, the label is blank,
     *                                  or {@code values} is empty or contains {@code null}
     * @throws ConflictException        if two values share the same code
     */
    public static Attribute create(String code, String label, AttributeType type, List<AttributeValue> values) {
        return new Attribute(code, label, type, values);
    }

    /**
     * Rebuilds an attribute from persisted data. Applies the same validation as
     * {@link #create(String, String, AttributeType, List)}.
     *
     * @throws InvalidArgumentException if the stored data violates the attribute rules
     * @throws ConflictException        if two stored values share the same code
     */
    public static Attribute restore(String code, String label, AttributeType type, List<AttributeValue> values) {
        return new Attribute(code, label, type, values);
    }

    /**
     * Checks that the selected values are allowed for this attribute.
     *
     * <ul>
     *   <li>{@link AttributeType#SINGLE}: exactly one value, and it must be an allowed value code.</li>
     *   <li>{@link AttributeType#MULTI}: at least one value, and every value must be an allowed value code.</li>
     * </ul>
     *
     * @param values the selected value codes
     * @throws InvalidArgumentException if {@code values} is null or empty, the count does not
     *                                  match the attribute type, or any value is null, blank
     *                                  or not an allowed value code
     */
    public void validateValue(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new InvalidArgumentException("Attribute '" + this.label + "' must contain at least one value");
        }

        Set<String> validCodes = getCodes();
        switch (this.type) {
            case SINGLE -> {
                if (values.size() != 1) {
                    throw new InvalidArgumentException("Attribute '" + this.label + "' must contain exactly one value");
                }
                String val = values.get(0);
                if (val == null || val.isBlank() || !validCodes.contains(val)) {
                    throw new InvalidArgumentException(
                        "Invalid value '" + val + "' for attribute '" + this.label + "'"
                    );
                }
            }
            case MULTI -> {
                for (String val : values) {
                    if (val == null || val.isBlank() || !validCodes.contains(val)) {
                        throw new InvalidArgumentException(
                            "Invalid value '" + val + "' in attribute '" + this.label + "'"
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
        if (values.isEmpty()) {
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

    public Set<String> getCodes() {
        return this.values.stream().map(AttributeValue::code).collect(Collectors.toUnmodifiableSet());
    }
}
