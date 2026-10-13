package io.d4tzz.ecommerce.catalog.domain.product;

import static io.d4tzz.ecommerce.catalog.domain.product.ProductFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.category.Attribute;
import io.d4tzz.ecommerce.catalog.domain.category.AttributeType;
import io.d4tzz.ecommerce.catalog.domain.category.AttributeValue;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryId;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProductTest {

    private static final OptionId MATERIAL = OptionId.of(3L);
    private static final ValueId COTTON = ValueId.of(30L);

    private static final Attribute MATERIAL_ATTRIBUTE = Attribute.create(
        "material",
        "Material",
        AttributeType.SINGLE,
        List.of(AttributeValue.of("cotton", "Cotton"), AttributeValue.of("wool", "Wool"))
    );

    private static Variant addBlueS(Product product, String sku, long price) {
        return product.addVariant(
            Sku.of(sku),
            Money.of(price),
            null,
            Dimensions.of(10, 10, 10),
            Weight.ofGrams(500),
            Map.of(COLOR, BLUE, SIZE, S),
            SKU_FREE
        );
    }

    private static Variant addVariant(Product product, String sku, Map<OptionId, ValueId> combination) {
        return product.addVariant(
            Sku.of(sku),
            Money.of(100_000),
            null,
            Dimensions.of(10, 10, 10),
            Weight.ofGrams(500),
            combination,
            SKU_FREE
        );
    }

    // ---- creation ----

    @Test
    void create_startsAsDraftWithoutOptionsMediaOrVariants() {
        Product product = emptyDraft();

        assertNull(product.getId());
        assertEquals(ProductStatus.DRAFT, product.getStatus());
        assertTrue(product.getOptions().isEmpty());
        assertTrue(product.getMedia().isEmpty());
        assertTrue(product.getVariants().isEmpty());
    }

    @Test
    void create_rejectsSlugAlreadyTaken() {
        assertThrows(ConflictException.class, () ->
            Product.create(
                ProductName.of("Shirt"),
                ProductSlug.of("shirt"),
                slug -> true,
                Description.of(""),
                CategoryId.of(1L),
                Map.of(),
                List.of()
            )
        );
    }

    @Test
    void restore_copiesTheVariantListSoTheProductCanChangeIt() {
        // shirt() hands an immutable List.of(...) to Product.restore
        Product product = shirt();

        addBlueS(product, "SHIRT-BLUE-S", 100_000);

        assertEquals(2, product.getVariants().size());
    }

    @Test
    void getOptions_keepsTheOrderItWasRestoredWith() {
        Product product = shirt();

        assertEquals(List.of(COLOR, SIZE), List.copyOf(product.getOptions().keySet()));
        assertEquals(List.of(RED, BLUE), List.copyOf(product.getOptions().get(COLOR)));
    }

    @Test
    void getOptions_isUnmodifiable() {
        Product product = shirt();

        assertThrows(UnsupportedOperationException.class, () -> product.getOptions().clear());
        assertThrows(UnsupportedOperationException.class, () -> product.getOptions().get(COLOR).add(ValueId.of(99L)));
    }

    // ---- name, slug, description ----

    @Test
    void rename_changesTheName() {
        Product product = shirt();

        product.rename(ProductName.of("Polo"));

        assertEquals("Polo", product.getName().value());
    }

    @Test
    void changeSlug_changesTheSlug() {
        Product product = shirt();

        product.changeSlug(ProductSlug.of("polo"), SLUG_FREE);

        assertEquals("polo", product.getSlug().value());
    }

    @Test
    void changeSlug_toTheSameSlugDoesNotCheckUniqueness() {
        Product product = shirt();

        product.changeSlug(ProductSlug.of("shirt"), slug -> true);

        assertEquals("shirt", product.getSlug().value());
    }

    @Test
    void changeSlug_rejectsSlugUsedByAnotherProduct() {
        Product product = shirt();

        assertThrows(ConflictException.class, () -> product.changeSlug(ProductSlug.of("polo"), slug -> true));
        assertEquals("shirt", product.getSlug().value());
    }

    @Test
    void changeDescription_treatsNullAsEmpty() {
        Product product = shirt();

        product.changeDescription(null);

        assertEquals("", product.getDescription().value());
    }

    // ---- attributes and category ----

    @Test
    void create_acceptsAttributesThatMatchTheSchema() {
        Product product = Product.create(
            ProductName.of("Shirt"),
            ProductSlug.of("shirt"),
            SLUG_FREE,
            Description.of(""),
            CategoryId.of(1L),
            Map.of("material", List.of("cotton")),
            List.of(MATERIAL_ATTRIBUTE)
        );

        assertEquals(Map.of("material", List.of("cotton")), product.getAttributes());
    }

    @Test
    void create_rejectsAttributeNotInTheSchema() {
        assertThrows(InvalidArgumentException.class, () ->
            Product.create(
                ProductName.of("Shirt"),
                ProductSlug.of("shirt"),
                SLUG_FREE,
                Description.of(""),
                CategoryId.of(1L),
                Map.of("fit", List.of("slim")),
                List.of(MATERIAL_ATTRIBUTE)
            )
        );
    }

    @Test
    void create_rejectsAttributesWhenTheCategoryHasNone() {
        assertThrows(InvalidArgumentException.class, () ->
            Product.create(
                ProductName.of("Shirt"),
                ProductSlug.of("shirt"),
                SLUG_FREE,
                Description.of(""),
                CategoryId.of(1L),
                Map.of("material", List.of("cotton")),
                List.of()
            )
        );
    }

    @Test
    void create_rejectsInvalidAttributeValue() {
        assertThrows(InvalidArgumentException.class, () ->
            Product.create(
                ProductName.of("Shirt"),
                ProductSlug.of("shirt"),
                SLUG_FREE,
                Description.of(""),
                CategoryId.of(1L),
                Map.of("material", List.of("silk")),
                List.of(MATERIAL_ATTRIBUTE)
            )
        );
    }

    @Test
    void create_treatsNullAttributesAsEmpty() {
        Product product = Product.create(
            ProductName.of("Shirt"),
            ProductSlug.of("shirt"),
            SLUG_FREE,
            Description.of(""),
            CategoryId.of(1L),
            null,
            List.of(MATERIAL_ATTRIBUTE)
        );

        assertTrue(product.getAttributes().isEmpty());
    }

    @Test
    void updateAttributes_replacesTheAttributes() {
        Product product = shirt();

        product.updateAttributes(Map.of("material", List.of("wool")), List.of(MATERIAL_ATTRIBUTE));

        assertEquals(Map.of("material", List.of("wool")), product.getAttributes());
    }

    @Test
    void changeCategory_setsTheCategoryAndReplacesTheAttributes() {
        Product product = shirt();

        product.changeCategory(CategoryId.of(2L), Map.of("material", List.of("wool")), List.of(MATERIAL_ATTRIBUTE));

        assertEquals(CategoryId.of(2L), product.getCategoryId());
        assertEquals(Map.of("material", List.of("wool")), product.getAttributes());
    }

    @Test
    void changeCategory_keepsTheOldCategoryWhenAttributesAreInvalid() {
        Product product = shirt();

        assertThrows(InvalidArgumentException.class, () ->
            product.changeCategory(CategoryId.of(2L), Map.of("material", List.of("silk")), List.of(MATERIAL_ATTRIBUTE))
        );
        assertEquals(CategoryId.of(1L), product.getCategoryId());
    }

    // ---- media ----

    @Test
    void addMedia_appendsToTheGalleryAndIgnoresDuplicates() {
        Product product = emptyDraft();

        product.addMedia(IMG_1);
        product.addMedia(IMG_2);
        product.addMedia(IMG_1);

        assertEquals(List.of(IMG_1, IMG_2), List.copyOf(product.getMedia()));
    }

    @Test
    void removeMedia_removesMediaNoVariantUses() {
        Product product = shirt();

        product.removeMedia(IMG_2);

        assertEquals(List.of(IMG_1), List.copyOf(product.getMedia()));
    }

    @Test
    void removeMedia_rejectsMediaUsedByAVariant() {
        Product product = shirt();

        assertThrows(InvalidArgumentException.class, () -> product.removeMedia(IMG_1));
        assertTrue(product.getMedia().contains(IMG_1));
    }

    @Test
    void reorderMedia_setsTheNewOrder() {
        Product product = shirt();

        product.reorderMedia(List.of(IMG_2, IMG_1));

        assertEquals(List.of(IMG_2, IMG_1), List.copyOf(product.getMedia()));
    }

    @Test
    void reorderMedia_rejectsAnOrderThatIsNotExactlyTheGallery() {
        Product product = shirt();
        MediaId other = MediaId.of("other");

        assertThrows(InvalidArgumentException.class, () -> product.reorderMedia(List.of(IMG_1)));
        assertThrows(InvalidArgumentException.class, () -> product.reorderMedia(List.of(IMG_1, IMG_1)));
        assertThrows(InvalidArgumentException.class, () -> product.reorderMedia(List.of(IMG_1, other)));
        assertThrows(InvalidArgumentException.class, () -> product.reorderMedia(Arrays.asList(IMG_1, null)));
        assertEquals(List.of(IMG_1, IMG_2), List.copyOf(product.getMedia()));
    }

    @Test
    void updateVariantMedia_setsAndClearsTheMedia() {
        Product product = shirt();

        Variant updated = product.updateVariantMedia(RED_S_ID, IMG_2);
        assertEquals(IMG_2, updated.getMediaId());

        updated = product.updateVariantMedia(RED_S_ID, null);
        assertNull(updated.getMediaId());
    }

    @Test
    void updateVariantMedia_rejectsMediaNotInTheGallery() {
        Product product = shirt();

        assertThrows(NotFoundException.class, () -> product.updateVariantMedia(RED_S_ID, MediaId.of("other")));
        assertEquals(IMG_1, product.findVariantById(RED_S_ID).getMediaId());
    }

    @Test
    void updateVariantMedia_rejectsUnknownVariant() {
        Product product = shirt();

        assertThrows(NotFoundException.class, () -> product.updateVariantMedia(VariantId.of(99L), IMG_2));
    }

    // ---- options ----

    @Test
    void addOption_addsTheDefaultValueToEveryExistingVariant() {
        Product product = shirt();

        product.addOption(MATERIAL, COTTON);

        assertEquals(List.of(COTTON), List.copyOf(product.getOptions().get(MATERIAL)));
        assertEquals(Map.of(COLOR, RED, SIZE, S, MATERIAL, COTTON), product.findVariantById(RED_S_ID).getCombination());
    }

    @Test
    void addOption_rejectsAnOptionAlreadyOnTheProduct() {
        Product product = shirt();

        assertThrows(ConflictException.class, () -> product.addOption(COLOR, BLUE));
    }

    @Test
    void addValue_appendsToTheOption() {
        Product product = shirt();
        ValueId green = ValueId.of(12L);

        product.addValue(COLOR, green);

        assertEquals(List.of(RED, BLUE, green), List.copyOf(product.getOptions().get(COLOR)));
    }

    @Test
    void addValue_rejectsUnknownOptionAndDuplicateValue() {
        Product product = shirt();

        assertThrows(NotFoundException.class, () -> product.addValue(MATERIAL, COTTON));
        assertThrows(ConflictException.class, () -> product.addValue(COLOR, RED));
    }

    @Test
    void removeValue_removesAValueNoVariantUses() {
        Product product = shirt();

        product.removeValue(COLOR, BLUE);

        assertEquals(List.of(RED), List.copyOf(product.getOptions().get(COLOR)));
    }

    @Test
    void removeValue_rejectsAValueUsedByAVariant() {
        Product product = shirt();

        assertThrows(InvalidOperationException.class, () -> product.removeValue(COLOR, RED));
        assertTrue(product.getOptions().get(COLOR).contains(RED));
    }

    @Test
    void removeValue_removesTheOptionToo_whenItWasTheLastValue() {
        Product product = emptyDraft();
        product.addOption(COLOR, RED);

        product.removeValue(COLOR, RED);

        assertFalse(product.getOptions().containsKey(COLOR));
    }

    @Test
    void removeValue_rejectsUnknownOptionAndUnknownValue() {
        Product product = shirt();

        assertThrows(NotFoundException.class, () -> product.removeValue(MATERIAL, COTTON));
        assertThrows(NotFoundException.class, () -> product.removeValue(COLOR, ValueId.of(99L)));
    }

    @Test
    void reorderOptionValues_setsTheNewOrder() {
        Product product = shirt();

        product.reorderOptionValues(COLOR, List.of(BLUE, RED));

        assertEquals(List.of(BLUE, RED), List.copyOf(product.getOptions().get(COLOR)));
    }

    @Test
    void reorderOptionValues_rejectsAnOrderThatIsNotExactlyTheValues() {
        Product product = shirt();

        assertThrows(InvalidArgumentException.class, () -> product.reorderOptionValues(COLOR, List.of(RED)));
        assertThrows(InvalidArgumentException.class, () -> product.reorderOptionValues(COLOR, List.of(RED, RED)));
        assertThrows(InvalidArgumentException.class, () -> product.reorderOptionValues(COLOR, List.of(RED, S)));
        assertThrows(InvalidArgumentException.class, () ->
            product.reorderOptionValues(COLOR, Arrays.asList(RED, null))
        );
        assertThrows(NotFoundException.class, () -> product.reorderOptionValues(MATERIAL, List.of(COTTON)));
        assertEquals(List.of(RED, BLUE), List.copyOf(product.getOptions().get(COLOR)));
    }

    @Test
    void removeOption_removesItFromTheProductAndFromEveryVariant() {
        Product product = shirt();
        product.removeValue(SIZE, M);

        product.removeOption(SIZE);

        assertFalse(product.getOptions().containsKey(SIZE));
        assertEquals(Map.of(COLOR, RED), product.findVariantById(RED_S_ID).getCombination());
    }

    @Test
    void removeOption_rejectsAnOptionWithSeveralValues() {
        Product product = shirt();

        assertThrows(InvalidOperationException.class, () -> product.removeOption(COLOR));
        assertTrue(product.getOptions().containsKey(COLOR));
    }

    @Test
    void removeOption_rejectsUnknownOption() {
        Product product = shirt();

        assertThrows(NotFoundException.class, () -> product.removeOption(MATERIAL));
    }

    @Test
    void reorderOptions_setsTheNewOrder() {
        Product product = shirt();

        product.reorderOptions(List.of(SIZE, COLOR));

        assertEquals(List.of(SIZE, COLOR), List.copyOf(product.getOptions().keySet()));
    }

    @Test
    void reorderOptions_rejectsAnOrderThatIsNotExactlyTheOptions() {
        Product product = shirt();

        assertThrows(InvalidArgumentException.class, () -> product.reorderOptions(List.of(COLOR)));
        assertThrows(InvalidArgumentException.class, () -> product.reorderOptions(List.of(COLOR, COLOR)));
        assertThrows(InvalidArgumentException.class, () -> product.reorderOptions(List.of(COLOR, MATERIAL)));
        assertThrows(InvalidArgumentException.class, () -> product.reorderOptions(Arrays.asList(COLOR, null)));
        assertEquals(List.of(COLOR, SIZE), List.copyOf(product.getOptions().keySet()));
    }

    // ---- addVariant ----

    @Test
    void addVariant_addsTheVariantWithoutAnId() {
        Product product = shirt();

        Variant variant = product.addVariant(
            Sku.of("SHIRT-BLUE-S"),
            Money.of(90_000),
            IMG_2,
            Dimensions.of(10, 10, 10),
            Weight.ofGrams(500),
            Map.of(COLOR, BLUE, SIZE, S),
            SKU_FREE
        );

        assertNull(variant.getId());
        assertEquals(IMG_2, variant.getMediaId());
        assertEquals(2, product.getVariants().size());
        assertTrue(product.hasVariantWithSku(Sku.of("SHIRT-BLUE-S")));
        assertTrue(product.hasVariantWithCombination(Map.of(COLOR, BLUE, SIZE, S)));
    }

    @Test
    void addVariant_withoutOptionsAcceptsNullCombination() {
        Product product = emptyDraft();

        Variant variant = addVariant(product, "SHIRT-ONE", null);

        assertTrue(variant.getCombination().isEmpty());
    }

    @Test
    void addVariant_rejectsMediaNotInTheGallery() {
        Product product = shirt();

        assertThrows(NotFoundException.class, () ->
            product.addVariant(
                Sku.of("SHIRT-BLUE-S"),
                Money.of(90_000),
                MediaId.of("other"),
                Dimensions.of(10, 10, 10),
                Weight.ofGrams(500),
                Map.of(COLOR, BLUE, SIZE, S),
                SKU_FREE
            )
        );
    }

    @Test
    void addVariant_rejectsSkuUsedByAnotherVariantOfTheProduct() {
        Product product = shirt();

        assertThrows(ConflictException.class, () ->
            addVariant(product, RED_S_SKU.value(), Map.of(COLOR, BLUE, SIZE, S))
        );
    }

    @Test
    void addVariant_rejectsSkuUsedByAnotherProduct() {
        Product product = shirt();

        assertThrows(ConflictException.class, () ->
            product.addVariant(
                Sku.of("SHIRT-BLUE-S"),
                Money.of(90_000),
                null,
                Dimensions.of(10, 10, 10),
                Weight.ofGrams(500),
                Map.of(COLOR, BLUE, SIZE, S),
                sku -> true
            )
        );
    }

    @Test
    void addVariant_rejectsACombinationThatDoesNotCoverEveryOption() {
        Product product = shirt();

        assertThrows(InvalidArgumentException.class, () -> addVariant(product, "X-1", Map.of(COLOR, BLUE)));
        assertThrows(InvalidArgumentException.class, () -> addVariant(product, "X-2", null));
    }

    @Test
    void addVariant_rejectsAnOptionOrValueTheProductDoesNotHave() {
        Product product = shirt();

        assertThrows(InvalidArgumentException.class, () ->
            addVariant(product, "X-1", Map.of(COLOR, ValueId.of(99L), SIZE, S))
        );
        assertThrows(InvalidArgumentException.class, () ->
            addVariant(product, "X-2", Map.of(COLOR, BLUE, MATERIAL, COTTON))
        );
    }

    @Test
    void addVariant_rejectsACombinationAnotherVariantAlreadyHas() {
        Product product = shirt();

        assertThrows(ConflictException.class, () -> addVariant(product, "X-1", Map.of(COLOR, RED, SIZE, S)));
        assertEquals(1, product.getVariants().size());
    }

    // ---- variant updates ----

    @Test
    void updateVariantPrice_changesPriceAndCompareAt() {
        Product product = shirt();

        Variant updated = product.updateVariantPrice(RED_S_ID, Money.of(80_000), Money.of(120_000));

        assertEquals(Money.of(80_000), updated.getPrice());
        assertEquals(Money.of(120_000), updated.getCompareAt());
    }

    @Test
    void updateVariantPrice_rejectsCompareAtLowerThanPrice() {
        Product product = shirt();

        assertThrows(InvalidArgumentException.class, () ->
            product.updateVariantPrice(RED_S_ID, Money.of(80_000), Money.of(50_000))
        );
    }

    @Test
    void updateVariantDimensionsAndWeight_changeTheVariant() {
        Product product = shirt();

        product.updateVariantDimensions(RED_S_ID, Dimensions.of(1, 2, 3));
        product.updateVariantWeight(RED_S_ID, Weight.ofGrams(900));

        Variant variant = product.findVariantById(RED_S_ID);
        assertEquals(Dimensions.of(1, 2, 3), variant.getDimensions());
        assertEquals(Weight.ofGrams(900), variant.getWeight());
    }

    @Test
    void updateVariant_rejectsUnknownVariant() {
        Product product = shirt();
        VariantId unknown = VariantId.of(99L);

        assertThrows(NotFoundException.class, () -> product.updateVariantPrice(unknown, Money.of(1), null));
        assertThrows(NotFoundException.class, () -> product.updateVariantWeight(unknown, Weight.ofGrams(1)));
        assertThrows(NotFoundException.class, () -> product.updateVariantDimensions(unknown, Dimensions.of(1, 1, 1)));
        assertThrows(NotFoundException.class, () -> product.updateVariantSku(unknown, Sku.of("X"), SKU_FREE));
    }

    @Test
    void updateVariantSku_changesTheSku() {
        Product product = shirt();

        product.updateVariantSku(RED_S_ID, Sku.of("NEW-SKU"), SKU_FREE);

        assertEquals(Sku.of("NEW-SKU"), product.findVariantById(RED_S_ID).getSku());
    }

    @Test
    void updateVariantSku_toTheSameSkuDoesNotCheckUniqueness() {
        Product product = shirt();

        product.updateVariantSku(RED_S_ID, RED_S_SKU, sku -> true);

        assertEquals(RED_S_SKU, product.findVariantById(RED_S_ID).getSku());
    }

    @Test
    void updateVariantSku_rejectsSkuUsedByAnotherProduct() {
        Product product = shirt();

        assertThrows(ConflictException.class, () ->
            product.updateVariantSku(RED_S_ID, Sku.of("NEW-SKU"), sku -> true)
        );
        assertEquals(RED_S_SKU, product.findVariantById(RED_S_ID).getSku());
    }

    @Test
    void updateVariantSku_rejectsSkuUsedByAnotherVariantOfTheProduct() {
        Product product = shirt();
        addBlueS(product, "SHIRT-BLUE-S", 100_000);

        assertThrows(ConflictException.class, () ->
            product.updateVariantSku(RED_S_ID, Sku.of("SHIRT-BLUE-S"), SKU_FREE)
        );
    }

    @Test
    void findVariantById_rejectsUnknownVariant() {
        assertThrows(NotFoundException.class, () -> shirt().findVariantById(VariantId.of(99L)));
    }

    // ---- removeVariant ----

    @Test
    void removeVariant_removesTheVariantOfADraft() {
        Product product = shirt(ProductStatus.DRAFT);

        product.removeVariant(RED_S_ID);

        assertTrue(product.getVariants().isEmpty());
    }

    @Test
    void removeVariant_ignoresAnUnknownVariant() {
        Product product = shirt(ProductStatus.DRAFT);

        product.removeVariant(VariantId.of(99L));

        assertEquals(1, product.getVariants().size());
    }

    @Test
    void removeVariant_isOnlyAllowedInDraft() {
        for (ProductStatus status : new ProductStatus[] { ProductStatus.PUBLISHED, ProductStatus.ARCHIVED }) {
            Product product = shirt(status);

            assertThrows(InvalidOperationException.class, () -> product.removeVariant(RED_S_ID), status.name());
            assertEquals(1, product.getVariants().size());
        }
    }

    // ---- status ----

    @Test
    void publish_andArchive_changeTheStatusAndAreIdempotent() {
        Product product = shirt();

        product.publish();
        product.publish();
        assertEquals(ProductStatus.PUBLISHED, product.getStatus());

        product.archive();
        product.archive();
        assertEquals(ProductStatus.ARCHIVED, product.getStatus());
    }

    @Test
    void draft_isOnlyValidWhenAlreadyDraft() {
        assertDoesNotThrow(() -> shirt(ProductStatus.DRAFT).draft());
        assertThrows(InvalidOperationException.class, () -> shirt(ProductStatus.PUBLISHED).draft());
        assertThrows(InvalidOperationException.class, () -> shirt(ProductStatus.ARCHIVED).draft());
    }

    @Test
    void ensureCanDelete_isOnlyAllowedInDraft() {
        assertDoesNotThrow(() -> shirt(ProductStatus.DRAFT).ensureCanDelete());
        assertThrows(InvalidOperationException.class, () -> shirt(ProductStatus.PUBLISHED).ensureCanDelete());
        assertThrows(InvalidOperationException.class, () -> shirt(ProductStatus.ARCHIVED).ensureCanDelete());
    }

    // ---- prices ----

    @Test
    void minAndMaxPrice_comeFromTheVariants() {
        Product product = shirt();
        addBlueS(product, "SHIRT-BLUE-S", 60_000);

        assertEquals(Money.of(60_000), product.getMinPrice().orElseThrow());
        assertEquals(Money.of(100_000), product.getMaxPrice().orElseThrow());
    }

    @Test
    void minAndMaxPrice_areEmptyWithoutVariants() {
        Product product = emptyDraft();

        assertTrue(product.getMinPrice().isEmpty());
        assertTrue(product.getMaxPrice().isEmpty());
    }
}
