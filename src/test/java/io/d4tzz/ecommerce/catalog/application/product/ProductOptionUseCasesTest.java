package io.d4tzz.ecommerce.catalog.application.product;

import static io.d4tzz.ecommerce.catalog.domain.product.ProductFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.catalog.application.product.command.AddProductOptionCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.AddProductOptionValueCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.ReorderProductOptionValuesCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.ReorderProductOptionsCommand;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.OptionRepository;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import io.d4tzz.ecommerce.catalog.domain.product.ProductStatus;
import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductOptionUseCasesTest {

    private static final long PRODUCT_ID = 1L;
    private static final long MATERIAL = 3L;
    private static final long COTTON = 30L;
    private static final long WOOL = 31L;

    private ProductRepository productRepository;
    private OptionRepository optionRepository;
    private AddProductOptionUseCase addOption;
    private AddProductOptionValueUseCase addValue;
    private RemoveProductOptionUseCase removeOption;
    private RemoveProductOptionValueUseCase removeValue;
    private ReorderProductOptionsUseCase reorderOptions;
    private ReorderProductOptionValuesUseCase reorderValues;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        optionRepository = mock(OptionRepository.class);
        addOption = new AddProductOptionUseCase(productRepository, optionRepository);
        addValue = new AddProductOptionValueUseCase(productRepository, optionRepository);
        removeOption = new RemoveProductOptionUseCase(productRepository);
        removeValue = new RemoveProductOptionValueUseCase(productRepository);
        reorderOptions = new ReorderProductOptionsUseCase(productRepository);
        reorderValues = new ReorderProductOptionValuesUseCase(productRepository);
    }

    private Product stored(Product product) {
        when(productRepository.find(ProductId.of(PRODUCT_ID))).thenReturn(Optional.of(product));
        return product;
    }

    private Product storedShirt() {
        return stored(shirt(ProductStatus.DRAFT));
    }

    private void productDoesNotExist() {
        when(productRepository.find(ProductId.of(PRODUCT_ID))).thenReturn(Optional.empty());
    }

    private void valuesBelongTo(long optionId, boolean belong) {
        when(optionRepository.areValuesBelongToOption(eq(OptionId.of(optionId)), any())).thenReturn(belong);
    }

    // ---- add option ----

    @Test
    void addOption_addsTheOptionWithTheFirstValueAsDefaultThenTheRest() {
        Product product = storedShirt();
        when(optionRepository.existsById(OptionId.of(MATERIAL))).thenReturn(true);
        valuesBelongTo(MATERIAL, true);

        addOption.execute(new AddProductOptionCommand(PRODUCT_ID, MATERIAL, List.of(COTTON, WOOL)));

        assertEquals(
            List.of(ValueId.of(COTTON), ValueId.of(WOOL)),
            List.copyOf(product.getOptions().get(OptionId.of(MATERIAL)))
        );
        // the existing variant receives the default (first) value
        assertEquals(
            ValueId.of(COTTON),
            product.findVariantById(RED_S_ID).getCombination().get(OptionId.of(MATERIAL))
        );
        verify(productRepository).save(product);
    }

    @Test
    void addOption_rejectsUnknownOption() {
        storedShirt();
        when(optionRepository.existsById(OptionId.of(MATERIAL))).thenReturn(false);

        assertThrows(NotFoundException.class, () ->
            addOption.execute(new AddProductOptionCommand(PRODUCT_ID, MATERIAL, List.of(COTTON)))
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void addOption_rejectsValuesThatDoNotBelongToTheOption() {
        storedShirt();
        when(optionRepository.existsById(OptionId.of(MATERIAL))).thenReturn(true);
        valuesBelongTo(MATERIAL, false);

        assertThrows(InvalidArgumentException.class, () ->
            addOption.execute(new AddProductOptionCommand(PRODUCT_ID, MATERIAL, List.of(COTTON)))
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void addOption_rejectsAnOptionAlreadyOnTheProduct() {
        storedShirt();
        when(optionRepository.existsById(COLOR)).thenReturn(true);
        valuesBelongTo(COLOR.value(), true);

        assertThrows(ConflictException.class, () ->
            addOption.execute(new AddProductOptionCommand(PRODUCT_ID, COLOR.value(), List.of(BLUE.value())))
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void addOption_rejectsUnknownProduct() {
        productDoesNotExist();

        assertThrows(NotFoundException.class, () ->
            addOption.execute(new AddProductOptionCommand(PRODUCT_ID, MATERIAL, List.of(COTTON)))
        );
    }

    // ---- add value ----

    @Test
    void addValue_addsTheValuesToAnOptionOfTheProduct() {
        Product product = storedShirt();
        valuesBelongTo(COLOR.value(), true);

        addValue.execute(new AddProductOptionValueCommand(PRODUCT_ID, COLOR.value(), List.of(40L, 41L)));

        assertEquals(
            List.of(RED, BLUE, ValueId.of(40L), ValueId.of(41L)),
            List.copyOf(product.getOptions().get(COLOR))
        );
        verify(productRepository).save(product);
    }

    @Test
    void addValue_rejectsValuesThatDoNotBelongToTheOption() {
        storedShirt();
        valuesBelongTo(COLOR.value(), false);

        assertThrows(InvalidArgumentException.class, () ->
            addValue.execute(new AddProductOptionValueCommand(PRODUCT_ID, COLOR.value(), List.of(40L)))
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void addValue_rejectsAnOptionTheProductDoesNotHave() {
        storedShirt();
        valuesBelongTo(MATERIAL, true);

        assertThrows(NotFoundException.class, () ->
            addValue.execute(new AddProductOptionValueCommand(PRODUCT_ID, MATERIAL, List.of(COTTON)))
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void addValue_rejectsAValueAlreadyInTheOption() {
        storedShirt();
        valuesBelongTo(COLOR.value(), true);

        assertThrows(ConflictException.class, () ->
            addValue.execute(new AddProductOptionValueCommand(PRODUCT_ID, COLOR.value(), List.of(RED.value())))
        );
    }

    // ---- remove option ----

    @Test
    void removeOption_removesAnOptionWithASingleValue() {
        Product product = stored(emptyDraft());
        product.addOption(COLOR, RED);

        removeOption.execute(PRODUCT_ID, COLOR.value());

        assertTrue(product.getOptions().isEmpty());
        verify(productRepository).save(product);
    }

    @Test
    void removeOption_rejectsAnOptionWithSeveralValues() {
        storedShirt();

        assertThrows(InvalidOperationException.class, () -> removeOption.execute(PRODUCT_ID, COLOR.value()));
        verify(productRepository, never()).save(any());
    }

    @Test
    void removeOption_rejectsUnknownProductAndUnknownOption() {
        productDoesNotExist();
        assertThrows(NotFoundException.class, () -> removeOption.execute(PRODUCT_ID, COLOR.value()));

        storedShirt();
        assertThrows(NotFoundException.class, () -> removeOption.execute(PRODUCT_ID, MATERIAL));
    }

    // ---- remove value ----

    @Test
    void removeValue_removesAValueNoVariantUses() {
        Product product = storedShirt();

        removeValue.execute(PRODUCT_ID, COLOR.value(), BLUE.value());

        assertEquals(List.of(RED), List.copyOf(product.getOptions().get(COLOR)));
        verify(productRepository).save(product);
    }

    @Test
    void removeValue_rejectsAValueUsedByAVariant() {
        storedShirt();

        assertThrows(InvalidOperationException.class, () ->
            removeValue.execute(PRODUCT_ID, COLOR.value(), RED.value())
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void removeValue_rejectsUnknownProduct() {
        productDoesNotExist();

        assertThrows(NotFoundException.class, () -> removeValue.execute(PRODUCT_ID, COLOR.value(), BLUE.value()));
    }

    // ---- reorder options ----

    @Test
    void reorderOptions_setsTheNewOrder() {
        Product product = storedShirt();

        reorderOptions.execute(new ReorderProductOptionsCommand(PRODUCT_ID, List.of(SIZE.value(), COLOR.value())));

        assertEquals(List.of(SIZE, COLOR), List.copyOf(product.getOptions().keySet()));
        verify(productRepository).save(product);
    }

    @Test
    void reorderOptions_rejectsAnOrderThatIsNotExactlyTheOptions() {
        storedShirt();

        assertThrows(InvalidArgumentException.class, () ->
            reorderOptions.execute(new ReorderProductOptionsCommand(PRODUCT_ID, List.of(COLOR.value())))
        );
        verify(productRepository, never()).save(any());
    }

    // ---- reorder values ----

    @Test
    void reorderValues_setsTheNewOrder() {
        Product product = storedShirt();

        reorderValues.execute(
            new ReorderProductOptionValuesCommand(PRODUCT_ID, COLOR.value(), List.of(BLUE.value(), RED.value()))
        );

        assertEquals(List.of(BLUE, RED), List.copyOf(product.getOptions().get(COLOR)));
        verify(productRepository).save(product);
    }

    @Test
    void reorderValues_rejectsAnOrderThatIsNotExactlyTheValues() {
        storedShirt();

        assertThrows(InvalidArgumentException.class, () ->
            reorderValues.execute(new ReorderProductOptionValuesCommand(PRODUCT_ID, COLOR.value(), List.of(RED.value())))
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void reorderValues_rejectsUnknownProduct() {
        productDoesNotExist();

        assertThrows(NotFoundException.class, () ->
            reorderValues.execute(new ReorderProductOptionValuesCommand(PRODUCT_ID, COLOR.value(), List.of()))
        );
    }

    @Test
    void reorderOptions_rejectsUnknownProduct() {
        productDoesNotExist();

        assertThrows(NotFoundException.class, () ->
            reorderOptions.execute(new ReorderProductOptionsCommand(PRODUCT_ID, List.of()))
        );
    }
}
