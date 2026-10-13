package io.d4tzz.ecommerce.catalog.application.product;

import static io.d4tzz.ecommerce.catalog.domain.product.ProductFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.catalog.application.product.command.AddProductMediaCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.AddProductVariantCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.ReorderProductMediaCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.UpdateProductVariantCommand;
import io.d4tzz.ecommerce.catalog.domain.product.Dimensions;
import io.d4tzz.ecommerce.catalog.domain.product.MediaId;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import io.d4tzz.ecommerce.catalog.domain.product.ProductStatus;
import io.d4tzz.ecommerce.catalog.domain.product.Sku;
import io.d4tzz.ecommerce.catalog.domain.product.Variant;
import io.d4tzz.ecommerce.catalog.domain.product.Weight;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductMediaAndVariantUseCasesTest {

    private static final long PRODUCT_ID = 1L;

    private ProductRepository productRepository;
    private AddProductMediaUseCase addMedia;
    private RemoveProductMediaUseCase removeMedia;
    private ReorderProductMediaUseCase reorderMedia;
    private AddProductVariantUseCase addVariant;
    private UpdateProductVariantUseCase updateVariant;
    private RemoveProductVariantUseCase removeVariant;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        addMedia = new AddProductMediaUseCase(productRepository);
        removeMedia = new RemoveProductMediaUseCase(productRepository);
        reorderMedia = new ReorderProductMediaUseCase(productRepository);
        addVariant = new AddProductVariantUseCase(productRepository);
        updateVariant = new UpdateProductVariantUseCase(productRepository);
        removeVariant = new RemoveProductVariantUseCase(productRepository);
    }

    private Product stored(ProductStatus status) {
        Product product = shirt(status);
        when(productRepository.find(ProductId.of(PRODUCT_ID))).thenReturn(Optional.of(product));
        return product;
    }

    private Product storedShirt() {
        return stored(ProductStatus.DRAFT);
    }

    private void productDoesNotExist() {
        when(productRepository.find(ProductId.of(PRODUCT_ID))).thenReturn(Optional.empty());
    }

    private static AddProductVariantCommand.Variant newVariant(String sku, String mediaId, Map<Long, Long> combination) {
        return new AddProductVariantCommand.Variant(sku, BigDecimal.valueOf(90_000), mediaId, 10, 20, 30, 700, combination);
    }

    private static UpdateProductVariantCommand updateCommand(BigDecimal price, BigDecimal compareAt, String mediaId) {
        return new UpdateProductVariantCommand(PRODUCT_ID, RED_S_ID.value(), price, compareAt, mediaId, 1, 2, 3, 900);
    }

    // ---- add media ----

    @Test
    void addMedia_appendsEveryMediaInOrder() {
        Product product = storedShirt();

        addMedia.execute(new AddProductMediaCommand(PRODUCT_ID, List.of("img-3", "img-4")));

        assertEquals(
            List.of(IMG_1, IMG_2, MediaId.of("img-3"), MediaId.of("img-4")),
            List.copyOf(product.getMedia())
        );
        verify(productRepository).save(product);
    }

    @Test
    void addMedia_rejectsUnknownProduct() {
        productDoesNotExist();

        assertThrows(NotFoundException.class, () ->
            addMedia.execute(new AddProductMediaCommand(PRODUCT_ID, List.of("img-3")))
        );
        verify(productRepository, never()).save(any());
    }

    // ---- remove media ----

    @Test
    void removeMedia_removesMediaNoVariantUses() {
        Product product = storedShirt();

        removeMedia.execute(PRODUCT_ID, IMG_2.value());

        assertEquals(List.of(IMG_1), List.copyOf(product.getMedia()));
        verify(productRepository).save(product);
    }

    @Test
    void removeMedia_rejectsMediaUsedByAVariant() {
        storedShirt();

        assertThrows(InvalidArgumentException.class, () -> removeMedia.execute(PRODUCT_ID, IMG_1.value()));
        verify(productRepository, never()).save(any());
    }

    // ---- reorder media ----

    @Test
    void reorderMedia_setsTheNewOrder() {
        Product product = storedShirt();

        reorderMedia.execute(new ReorderProductMediaCommand(PRODUCT_ID, List.of(IMG_2.value(), IMG_1.value())));

        assertEquals(List.of(IMG_2, IMG_1), List.copyOf(product.getMedia()));
        verify(productRepository).save(product);
    }

    @Test
    void reorderMedia_rejectsAnOrderThatIsNotExactlyTheGallery() {
        storedShirt();

        assertThrows(InvalidArgumentException.class, () ->
            reorderMedia.execute(new ReorderProductMediaCommand(PRODUCT_ID, List.of(IMG_1.value())))
        );
        verify(productRepository, never()).save(any());
    }

    // ---- add variant ----

    @Test
    void addVariant_mapsTheCommandToAVariantOfTheProduct() {
        Product product = storedShirt();

        addVariant.execute(
            new AddProductVariantCommand(
                PRODUCT_ID,
                List.of(newVariant("SHIRT-BLUE-S", IMG_2.value(), Map.of(COLOR.value(), BLUE.value(), SIZE.value(), S.value())))
            )
        );

        assertEquals(2, product.getVariants().size());
        Variant added = product.getVariants().get(1);
        assertEquals(Sku.of("SHIRT-BLUE-S"), added.getSku());
        assertEquals(Money.of(90_000), added.getPrice());
        assertEquals(IMG_2, added.getMediaId());
        assertEquals(Dimensions.of(10, 20, 30), added.getDimensions());
        assertEquals(Weight.ofGrams(700), added.getWeight());
        assertEquals(Map.of(COLOR, BLUE, SIZE, S), added.getCombination());
        verify(productRepository).save(product);
    }

    @Test
    void addVariant_addsEveryVariantOfTheCommand() {
        Product product = storedShirt();

        addVariant.execute(
            new AddProductVariantCommand(
                PRODUCT_ID,
                List.of(
                    newVariant("SHIRT-BLUE-S", null, Map.of(COLOR.value(), BLUE.value(), SIZE.value(), S.value())),
                    newVariant("SHIRT-RED-M", null, Map.of(COLOR.value(), RED.value(), SIZE.value(), M.value()))
                )
            )
        );

        assertEquals(3, product.getVariants().size());
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void addVariant_treatsABlankMediaIdAsNoMedia() {
        Product product = storedShirt();

        addVariant.execute(
            new AddProductVariantCommand(
                PRODUCT_ID,
                List.of(newVariant("SHIRT-BLUE-S", "  ", Map.of(COLOR.value(), BLUE.value(), SIZE.value(), S.value())))
            )
        );

        assertNull(product.getVariants().get(1).getMediaId());
    }

    @Test
    void addVariant_rejectsSkuUsedByAnotherProduct() {
        storedShirt();
        when(productRepository.existsBySku(Sku.of("SHIRT-BLUE-S"))).thenReturn(true);

        assertThrows(ConflictException.class, () ->
            addVariant.execute(
                new AddProductVariantCommand(
                    PRODUCT_ID,
                    List.of(newVariant("SHIRT-BLUE-S", null, Map.of(COLOR.value(), BLUE.value(), SIZE.value(), S.value())))
                )
            )
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void addVariant_rejectsACombinationThatIsNotComplete() {
        storedShirt();

        assertThrows(InvalidArgumentException.class, () ->
            addVariant.execute(
                new AddProductVariantCommand(PRODUCT_ID, List.of(newVariant("SHIRT-X", null, Map.of(COLOR.value(), BLUE.value()))))
            )
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void addVariant_rejectsUnknownProduct() {
        productDoesNotExist();

        assertThrows(NotFoundException.class, () ->
            addVariant.execute(new AddProductVariantCommand(PRODUCT_ID, List.of(newVariant("X", null, Map.of()))))
        );
    }

    // ---- update variant ----

    @Test
    void updateVariant_changesPriceCompareAtDimensionsAndMedia() {
        Product product = storedShirt();

        updateVariant.execute(updateCommand(BigDecimal.valueOf(80_000), BigDecimal.valueOf(120_000), IMG_2.value()));

        Variant variant = product.findVariantById(RED_S_ID);
        assertEquals(Money.of(80_000), variant.getPrice());
        assertEquals(Money.of(120_000), variant.getCompareAt());
        assertEquals(Dimensions.of(1, 2, 3), variant.getDimensions());
        assertEquals(IMG_2, variant.getMediaId());
        verify(productRepository).save(product);
    }

    @Test
    void updateVariant_changesTheWeight() {
        Product product = storedShirt();

        updateVariant.execute(updateCommand(BigDecimal.valueOf(80_000), null, IMG_1.value()));

        assertEquals(Weight.ofGrams(900), product.findVariantById(RED_S_ID).getWeight());
    }

    @Test
    void updateVariant_withoutCompareAtOrMediaClearsThem() {
        Product product = storedShirt();

        updateVariant.execute(updateCommand(BigDecimal.valueOf(80_000), null, " "));

        Variant variant = product.findVariantById(RED_S_ID);
        assertNull(variant.getCompareAt());
        assertNull(variant.getMediaId());
    }

    @Test
    void updateVariant_rejectsCompareAtLowerThanPrice() {
        storedShirt();

        assertThrows(InvalidArgumentException.class, () ->
            updateVariant.execute(updateCommand(BigDecimal.valueOf(80_000), BigDecimal.valueOf(50_000), null))
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateVariant_rejectsMediaNotInTheGallery() {
        storedShirt();

        assertThrows(NotFoundException.class, () ->
            updateVariant.execute(updateCommand(BigDecimal.valueOf(80_000), null, "other"))
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateVariant_rejectsUnknownProductAndUnknownVariant() {
        productDoesNotExist();
        assertThrows(NotFoundException.class, () -> updateVariant.execute(updateCommand(BigDecimal.TEN, null, null)));

        storedShirt();
        assertThrows(NotFoundException.class, () ->
            updateVariant.execute(
                new UpdateProductVariantCommand(PRODUCT_ID, 99L, BigDecimal.TEN, null, null, 1, 1, 1, 1)
            )
        );
    }

    // ---- remove variant ----

    @Test
    void removeVariant_removesTheVariantOfADraft() {
        Product product = storedShirt();

        removeVariant.execute(PRODUCT_ID, RED_S_ID.value());

        assertTrue(product.getVariants().isEmpty());
        verify(productRepository).save(product);
    }

    @Test
    void removeVariant_rejectsAProductThatIsNotADraft() {
        Product product = stored(ProductStatus.PUBLISHED);

        assertThrows(InvalidOperationException.class, () -> removeVariant.execute(PRODUCT_ID, RED_S_ID.value()));
        assertEquals(1, product.getVariants().size());
        verify(productRepository, never()).save(any());
    }

    @Test
    void removeVariant_rejectsUnknownProduct() {
        productDoesNotExist();

        assertThrows(NotFoundException.class, () -> removeVariant.execute(PRODUCT_ID, RED_S_ID.value()));
    }
}
