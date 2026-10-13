package io.d4tzz.ecommerce.catalog.domain.option;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OptionTest {

    private static final ValueId RED_ID = ValueId.of(1L);
    private static final ValueId BLUE_ID = ValueId.of(2L);

    private static final OptionNameUniqueChecker NAME_FREE = name -> false;
    private static final OptionNameUniqueChecker NAME_TAKEN = name -> true;

    /** A persisted "Color" option with the values Red (id 1) and Blue (id 2). */
    private static Option color() {
        return new Option(
            OptionId.of(1L),
            OptionName.of("Color"),
            new ArrayList<>(
                List.of(Value.restore(RED_ID, ValueName.of("Red")), Value.restore(BLUE_ID, ValueName.of("Blue")))
            )
        );
    }

    private static List<String> valueNames(Option option) {
        return option
            .getValues()
            .stream()
            .map(v -> v.getName().value())
            .toList();
    }

    // ---- creation ----

    @Test
    void create_addsTheInitialValuesInOrder() {
        Option option = Option.create(
            OptionName.of("Size"),
            List.of(ValueName.of("S"), ValueName.of("M")),
            NAME_FREE
        );

        assertEquals("Size", option.getName().value());
        assertEquals(List.of("S", "M"), valueNames(option));
        assertNull(option.getId());
    }

    @Test
    void create_rejectsNameAlreadyTaken() {
        assertThrows(ConflictException.class, () ->
            Option.create(OptionName.of("Size"), List.of(), NAME_TAKEN)
        );
    }

    @Test
    void create_rejectsDuplicateValueNames() {
        assertThrows(ConflictException.class, () ->
            Option.create(OptionName.of("Size"), List.of(ValueName.of("S"), ValueName.of("S")), NAME_FREE)
        );
    }

    // ---- rename ----

    @Test
    void rename_changesTheName() {
        Option option = color();

        option.rename(OptionName.of("Colour"), NAME_FREE);

        assertEquals("Colour", option.getName().value());
    }

    @Test
    void rename_toTheSameNameDoesNotCheckUniqueness() {
        Option option = color();
        // the option's own name is "taken" by itself, so the checker must not even be asked
        OptionNameUniqueChecker mustNotBeCalled = name -> {
            throw new AssertionError("checker should not be called");
        };

        option.rename(OptionName.of("Color"), mustNotBeCalled);

        assertEquals("Color", option.getName().value());
    }

    @Test
    void rename_rejectsNameUsedByAnotherOption() {
        Option option = color();

        assertThrows(ConflictException.class, () -> option.rename(OptionName.of("Size"), NAME_TAKEN));
        assertEquals("Color", option.getName().value());
    }

    // ---- addValue ----

    @Test
    void addValue_appendsTheValue() {
        Option option = color();

        Value added = option.addValue(ValueName.of("Green"));

        assertEquals("Green", added.getName().value());
        assertEquals(List.of("Red", "Blue", "Green"), valueNames(option));
    }

    @Test
    void addValue_rejectsDuplicateName() {
        Option option = color();

        assertThrows(ConflictException.class, () -> option.addValue(ValueName.of("Red")));
        assertEquals(List.of("Red", "Blue"), valueNames(option));
    }

    // ---- removeValue ----

    @Test
    void removeValue_removesAValueThatIsNotInUse() {
        Option option = color();

        option.removeValue(RED_ID, id -> false);

        assertEquals(List.of("Blue"), valueNames(option));
    }

    @Test
    void removeValue_rejectsAValueInUse() {
        Option option = color();

        assertThrows(InvalidOperationException.class, () -> option.removeValue(RED_ID, id -> true));
        assertEquals(List.of("Red", "Blue"), valueNames(option));
    }

    @Test
    void removeValue_ignoresAnUnknownValue() {
        Option option = color();

        option.removeValue(ValueId.of(99L), id -> false);

        assertEquals(List.of("Red", "Blue"), valueNames(option));
    }

    // ---- renameValue ----

    @Test
    void renameValue_changesTheName() {
        Option option = color();

        Value renamed = option.renameValue(RED_ID, ValueName.of("Crimson"));

        assertEquals(RED_ID, renamed.getId());
        assertEquals(List.of("Crimson", "Blue"), valueNames(option));
    }

    @Test
    void renameValue_toTheSameNameChangesNothing() {
        Option option = color();

        Value result = option.renameValue(RED_ID, ValueName.of("Red"));

        assertEquals("Red", result.getName().value());
        assertEquals(List.of("Red", "Blue"), valueNames(option));
    }

    @Test
    void renameValue_rejectsNameUsedByAnotherValue() {
        Option option = color();

        assertThrows(ConflictException.class, () -> option.renameValue(RED_ID, ValueName.of("Blue")));
        assertEquals(List.of("Red", "Blue"), valueNames(option));
    }

    @Test
    void renameValue_rejectsAnUnknownValue() {
        Option option = color();

        assertThrows(InvalidArgumentException.class, () -> option.renameValue(ValueId.of(99L), ValueName.of("X")));
    }
}
