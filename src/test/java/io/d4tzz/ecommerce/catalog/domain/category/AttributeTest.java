package io.d4tzz.ecommerce.catalog.domain.category;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AttributeTest {

    private static Attribute material(AttributeType type) {
        return Attribute.create(
            "material",
            "Material",
            type,
            List.of(AttributeValue.of("cotton", "Cotton"), AttributeValue.of("wool", "Wool"))
        );
    }

    // ---- creation ----

    @Test
    void create_trimsAndLowercasesTheCodeAndTrimsTheLabel() {
        Attribute attribute = Attribute.create(
            "  Material ",
            "  Material type ",
            AttributeType.SINGLE,
            List.of(AttributeValue.of("cotton", "Cotton"))
        );

        assertEquals("material", attribute.getCode());
        assertEquals("Material type", attribute.getLabel());
    }

    @Test
    void create_rejectsInvalidCodes() {
        for (String code : new String[] { "a", "has space", "with-dash", "a".repeat(51), "" }) {
            assertThrows(
                InvalidArgumentException.class,
                () -> Attribute.create(code, "Label", AttributeType.SINGLE, List.of(AttributeValue.of("x1", "X"))),
                code
            );
        }
    }

    @Test
    void create_acceptsDigitsAndUnderscoresInTheCode() {
        Attribute attribute = Attribute.create(
            "size_2xl",
            "Size",
            AttributeType.SINGLE,
            List.of(AttributeValue.of("big", "Big"))
        );

        assertEquals("size_2xl", attribute.getCode());
    }

    @Test
    void create_rejectsBlankLabel() {
        assertThrows(InvalidArgumentException.class, () ->
            Attribute.create("material", "   ", AttributeType.SINGLE, List.of(AttributeValue.of("cotton", "Cotton")))
        );
    }

    @Test
    void create_rejectsNoValues() {
        assertThrows(InvalidArgumentException.class, () ->
            Attribute.create("material", "Material", AttributeType.SINGLE, List.of())
        );
    }

    @Test
    void create_rejectsNullValueInTheList() {
        assertThrows(InvalidArgumentException.class, () ->
            Attribute.create(
                "material",
                "Material",
                AttributeType.SINGLE,
                Arrays.asList(AttributeValue.of("cotton", "Cotton"), null)
            )
        );
    }

    @Test
    void create_rejectsDuplicateValueCodes() {
        // value codes are lowercased, so "Cotton" and "cotton" are the same code
        assertThrows(ConflictException.class, () ->
            Attribute.create(
                "material",
                "Material",
                AttributeType.SINGLE,
                List.of(AttributeValue.of("cotton", "Cotton"), AttributeValue.of("Cotton", "Cotton again"))
            )
        );
    }

    @Test
    void restore_appliesTheSameValidationAsCreate() {
        assertThrows(InvalidArgumentException.class, () ->
            Attribute.restore("material", "Material", AttributeType.SINGLE, List.of())
        );
    }

    // ---- validateValue: SINGLE ----

    @Test
    void validateValue_singleAcceptsExactlyOneAllowedValue() {
        assertDoesNotThrow(() -> material(AttributeType.SINGLE).validateValue(List.of("cotton")));
    }

    @Test
    void validateValue_singleRejectsMoreThanOneValue() {
        assertThrows(InvalidArgumentException.class, () ->
            material(AttributeType.SINGLE).validateValue(List.of("cotton", "wool"))
        );
    }

    @Test
    void validateValue_singleRejectsUnknownOrBlankValue() {
        Attribute attribute = material(AttributeType.SINGLE);

        assertThrows(InvalidArgumentException.class, () -> attribute.validateValue(List.of("silk")));
        assertThrows(InvalidArgumentException.class, () -> attribute.validateValue(List.of(" ")));
        assertThrows(InvalidArgumentException.class, () -> attribute.validateValue(Arrays.asList((String) null)));
    }

    // ---- validateValue: MULTI ----

    @Test
    void validateValue_multiAcceptsSeveralAllowedValues() {
        assertDoesNotThrow(() -> material(AttributeType.MULTI).validateValue(List.of("cotton", "wool")));
    }

    @Test
    void validateValue_multiRejectsIfAnyValueIsUnknown() {
        assertThrows(InvalidArgumentException.class, () ->
            material(AttributeType.MULTI).validateValue(List.of("cotton", "silk"))
        );
    }

    // ---- validateValue: both types ----

    @Test
    void validateValue_rejectsNullOrEmptyList() {
        for (AttributeType type : AttributeType.values()) {
            Attribute attribute = material(type);

            assertThrows(InvalidArgumentException.class, () -> attribute.validateValue(null), type.name());
            assertThrows(InvalidArgumentException.class, () -> attribute.validateValue(List.of()), type.name());
        }
    }

    // ---- getters ----

    @Test
    void getCodes_returnsTheValueCodes() {
        assertEquals(Set.of("cotton", "wool"), material(AttributeType.SINGLE).getCodes());
    }

    @Test
    void getValues_isUnmodifiable() {
        Attribute attribute = material(AttributeType.SINGLE);

        assertThrows(UnsupportedOperationException.class, () -> attribute.getValues().clear());
    }

    @Test
    void create_copiesTheValueListSoLaterChangesToItAreIgnored() {
        List<AttributeValue> values = new java.util.ArrayList<>(List.of(AttributeValue.of("cotton", "Cotton")));
        Attribute attribute = Attribute.create("material", "Material", AttributeType.SINGLE, values);

        values.add(AttributeValue.of("wool", "Wool"));

        assertEquals(Set.of("cotton"), attribute.getCodes());
    }
}
