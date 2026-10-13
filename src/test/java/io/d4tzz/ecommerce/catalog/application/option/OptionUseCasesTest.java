package io.d4tzz.ecommerce.catalog.application.option;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.catalog.application.option.command.AddOptionValueCommand;
import io.d4tzz.ecommerce.catalog.application.option.command.CreateOptionCommand;
import io.d4tzz.ecommerce.catalog.application.option.command.DeleteOptionValueCommand;
import io.d4tzz.ecommerce.catalog.application.option.command.UpdateOptionCommand;
import io.d4tzz.ecommerce.catalog.application.option.command.UpdateOptionValueCommand;
import io.d4tzz.ecommerce.catalog.domain.option.Option;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.OptionName;
import io.d4tzz.ecommerce.catalog.domain.option.OptionRepository;
import io.d4tzz.ecommerce.catalog.domain.option.Value;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import io.d4tzz.ecommerce.catalog.domain.option.ValueName;
import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OptionUseCasesTest {

    private static final long OPTION_ID = 3L;
    private static final long RED_ID = 10L;
    private static final long BLUE_ID = 11L;

    private OptionRepository repository;
    private CreateOptionUseCase create;
    private UpdateOptionUseCase update;
    private DeleteOptionUseCase delete;
    private AddOptionValueUseCase addValue;
    private UpdateOptionValueUseCase updateValue;
    private DeleteOptionValueUseCase deleteValue;

    @BeforeEach
    void setUp() {
        repository = mock(OptionRepository.class);
        create = new CreateOptionUseCase(repository);
        update = new UpdateOptionUseCase(repository);
        delete = new DeleteOptionUseCase(repository);
        addValue = new AddOptionValueUseCase(repository);
        updateValue = new UpdateOptionValueUseCase(repository);
        deleteValue = new DeleteOptionValueUseCase(repository);
    }

    /** Option "Color" with the values Red and Blue, already stored. */
    private Option storedColor() {
        Option option = new Option(
            OptionId.of(OPTION_ID),
            OptionName.of("Color"),
            new ArrayList<>(
                List.of(
                    Value.restore(ValueId.of(RED_ID), ValueName.of("Red")),
                    Value.restore(ValueId.of(BLUE_ID), ValueName.of("Blue"))
                )
            )
        );
        when(repository.find(OptionId.of(OPTION_ID))).thenReturn(Optional.of(option));
        return option;
    }

    private void optionDoesNotExist() {
        when(repository.find(OptionId.of(OPTION_ID))).thenReturn(Optional.empty());
    }

    private static List<String> valueNames(Option option) {
        return option.getValues().stream().map(v -> v.getName().value()).toList();
    }

    // ---- create ----

    @Test
    void create_savesTheOptionWithItsValuesAndReturnsTheId() {
        when(repository.save(any(Option.class))).thenAnswer(inv -> {
            Option o = inv.getArgument(0);
            return new Option(OptionId.of(OPTION_ID), o.getName(), new ArrayList<>(o.getValues()));
        });

        Long id = create.execute(new CreateOptionCommand("Size", List.of("S", "M")));

        assertEquals(OPTION_ID, id);
        ArgumentCaptor<Option> captor = ArgumentCaptor.forClass(Option.class);
        verify(repository).save(captor.capture());
        assertEquals("Size", captor.getValue().getName().value());
        assertEquals(List.of("S", "M"), valueNames(captor.getValue()));
    }

    @Test
    void create_rejectsNameAlreadyTaken() {
        when(repository.existsByName(OptionName.of("Size"))).thenReturn(true);

        assertThrows(ConflictException.class, () -> create.execute(new CreateOptionCommand("Size", List.of())));
        verify(repository, never()).save(any());
    }

    @Test
    void create_rejectsDuplicateValueNames() {
        assertThrows(ConflictException.class, () -> create.execute(new CreateOptionCommand("Size", List.of("S", "S"))));
        verify(repository, never()).save(any());
    }

    // ---- update ----

    @Test
    void update_renamesTheOption() {
        Option option = storedColor();

        update.execute(new UpdateOptionCommand(OPTION_ID, "Colour"));

        assertEquals("Colour", option.getName().value());
        verify(repository).save(option);
    }

    @Test
    void update_rejectsUnknownOption() {
        optionDoesNotExist();

        assertThrows(NotFoundException.class, () -> update.execute(new UpdateOptionCommand(OPTION_ID, "Colour")));
        verify(repository, never()).save(any());
    }

    @Test
    void update_rejectsNameUsedByAnotherOption() {
        Option option = storedColor();
        when(repository.existsByName(OptionName.of("Size"))).thenReturn(true);

        assertThrows(ConflictException.class, () -> update.execute(new UpdateOptionCommand(OPTION_ID, "Size")));
        assertEquals("Color", option.getName().value());
        verify(repository, never()).save(any());
    }

    // ---- delete ----

    @Test
    void delete_removesAnOptionNoProductUses() {
        when(repository.existsById(OptionId.of(OPTION_ID))).thenReturn(true);

        delete.execute(OPTION_ID);

        verify(repository).delete(OptionId.of(OPTION_ID));
    }

    @Test
    void delete_rejectsUnknownOption() {
        when(repository.existsById(OptionId.of(OPTION_ID))).thenReturn(false);

        assertThrows(NotFoundException.class, () -> delete.execute(OPTION_ID));
        verify(repository, never()).delete(any());
    }

    @Test
    void delete_rejectsAnOptionInUse() {
        when(repository.existsById(OptionId.of(OPTION_ID))).thenReturn(true);
        when(repository.isOptionReferenced(OptionId.of(OPTION_ID))).thenReturn(true);

        assertThrows(InvalidOperationException.class, () -> delete.execute(OPTION_ID));
        verify(repository, never()).delete(any());
    }

    // ---- add value ----

    @Test
    void addValue_appendsTheValueAndSaves() {
        Option option = storedColor();

        addValue.execute(new AddOptionValueCommand(OPTION_ID, "Green"));

        assertEquals(List.of("Red", "Blue", "Green"), valueNames(option));
        verify(repository).save(option);
    }

    @Test
    void addValue_rejectsDuplicateName() {
        storedColor();

        assertThrows(ConflictException.class, () -> addValue.execute(new AddOptionValueCommand(OPTION_ID, "Red")));
        verify(repository, never()).save(any());
    }

    @Test
    void addValue_rejectsUnknownOption() {
        optionDoesNotExist();

        assertThrows(NotFoundException.class, () -> addValue.execute(new AddOptionValueCommand(OPTION_ID, "Green")));
    }

    // ---- update value ----

    @Test
    void updateValue_renamesTheValueAndSaves() {
        Option option = storedColor();

        updateValue.execute(new UpdateOptionValueCommand(OPTION_ID, RED_ID, "Crimson"));

        assertEquals(List.of("Crimson", "Blue"), valueNames(option));
        verify(repository).save(option);
    }

    @Test
    void updateValue_rejectsNameUsedByAnotherValue() {
        storedColor();

        assertThrows(ConflictException.class, () ->
            updateValue.execute(new UpdateOptionValueCommand(OPTION_ID, RED_ID, "Blue"))
        );
        verify(repository, never()).save(any());
    }

    @Test
    void updateValue_rejectsUnknownValueAndUnknownOption() {
        storedColor();
        assertThrows(InvalidArgumentException.class, () ->
            updateValue.execute(new UpdateOptionValueCommand(OPTION_ID, 99L, "X"))
        );

        optionDoesNotExist();
        assertThrows(NotFoundException.class, () ->
            updateValue.execute(new UpdateOptionValueCommand(OPTION_ID, RED_ID, "X"))
        );
        verify(repository, never()).save(any());
    }

    // ---- delete value ----

    @Test
    void deleteValue_removesAValueNoProductUses() {
        Option option = storedColor();

        deleteValue.execute(new DeleteOptionValueCommand(OPTION_ID, RED_ID));

        assertEquals(List.of("Blue"), valueNames(option));
        verify(repository).save(option);
    }

    @Test
    void deleteValue_rejectsAValueInUse() {
        Option option = storedColor();
        when(repository.isValueReferenced(ValueId.of(RED_ID))).thenReturn(true);

        assertThrows(InvalidOperationException.class, () ->
            deleteValue.execute(new DeleteOptionValueCommand(OPTION_ID, RED_ID))
        );
        assertEquals(List.of("Red", "Blue"), valueNames(option));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteValue_rejectsUnknownOption() {
        optionDoesNotExist();

        assertThrows(NotFoundException.class, () ->
            deleteValue.execute(new DeleteOptionValueCommand(OPTION_ID, RED_ID))
        );
    }
}
