package io.d4tzz.ecommerce.catalog.domain.product;

import static io.d4tzz.ecommerce.catalog.domain.product.ProductFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VariantTest {

    private static Variant redS() {
        return variant(1, "SHIRT-RED-S", 100_000, null, Map.of(COLOR, RED, SIZE, S));
    }

    // ---- creation ----

    @Test
    void create_startsWithoutIdAndCompareAtEqualToPrice() {
        Variant variant = Variant.create(
            Sku.of("SKU-1"),
            Money.of(100_000),
            null,
            Dimensions.of(10, 10, 10),
            Weight.ofGrams(500),
            Map.of(COLOR, RED)
        );

        assertNull(variant.getId());
        assertEquals(Money.of(100_000), variant.getPrice());
        assertEquals(Money.of(100_000), variant.getCompareAt());
        assertEquals(Map.of(COLOR, RED), variant.getCombination());
    }

    @Test
    void restore_acceptsCompareAtEqualToOrHigherThanPrice() {
        assertDoesNotThrow(() -> restoreWithCompareAt(100_000, 100_000L));
        assertDoesNotThrow(() -> restoreWithCompareAt(100_000, 150_000L));
    }

    @Test
    void restore_acceptsNoCompareAt() {
        Variant variant = restoreWithCompareAt(100_000, null);

        assertNull(variant.getCompareAt());
    }

    @Test
    void restore_rejectsCompareAtLowerThanPrice() {
        assertThrows(InvalidArgumentException.class, () -> restoreWithCompareAt(100_000, 50_000L));
    }

    private static Variant restoreWithCompareAt(long price, Long compareAt) {
        return Variant.restore(
            VariantId.of(1L),
            Sku.of("SKU-1"),
            Money.of(price),
            compareAt == null ? null : Money.of(compareAt),
            null,
            Dimensions.of(10, 10, 10),
            Weight.ofGrams(500),
            Map.of()
        );
    }

    @Test
    void combination_isCopiedSoLaterChangesToTheSourceAreIgnored() {
        Map<OptionId, ValueId> source = new HashMap<>(Map.of(COLOR, RED));
        Variant variant = variant(1, "SKU-1", 100_000, null, source);

        source.put(SIZE, S);

        assertEquals(Map.of(COLOR, RED), variant.getCombination());
    }

    @Test
    void getCombination_isUnmodifiable() {
        Variant variant = redS();

        assertThrows(UnsupportedOperationException.class, () -> variant.getCombination().clear());
    }

    // ---- price ----

    @Test
    void changePrice_updatesPriceAndCompareAt() {
        Variant variant = redS();

        variant.changePrice(Money.of(80_000), Money.of(100_000));

        assertEquals(Money.of(80_000), variant.getPrice());
        assertEquals(Money.of(100_000), variant.getCompareAt());
    }

    @Test
    void changePrice_rejectsCompareAtLowerThanPriceAndKeepsTheOldValues() {
        Variant variant = redS();

        assertThrows(InvalidArgumentException.class, () ->
            variant.changePrice(Money.of(80_000), Money.of(50_000))
        );
        assertEquals(Money.of(100_000), variant.getPrice());
        assertEquals(Money.of(100_000), variant.getCompareAt());
    }

    @Test
    void changePrice_withoutCompareAtClearsIt() {
        Variant variant = redS();

        variant.changePrice(Money.of(80_000));

        assertEquals(Money.of(80_000), variant.getPrice());
        assertNull(variant.getCompareAt());
    }

    // ---- other mutators ----

    @Test
    void changeSku_changeMedia_changeDimensions_changeWeight() {
        Variant variant = redS();

        variant.changeSku(Sku.of("NEW-SKU"));
        variant.changeMedia(IMG_2);
        variant.changeDimensions(Dimensions.of(1, 2, 3));
        variant.changeWeight(Weight.ofGrams(900));

        assertEquals(Sku.of("NEW-SKU"), variant.getSku());
        assertEquals(IMG_2, variant.getMediaId());
        assertEquals(Dimensions.of(1, 2, 3), variant.getDimensions());
        assertEquals(Weight.ofGrams(900), variant.getWeight());
    }

    @Test
    void changeMedia_canClearTheMedia() {
        Variant variant = variant(1, "SKU-1", 100_000, IMG_1, Map.of());

        variant.changeMedia(null);

        assertNull(variant.getMediaId());
    }

    // ---- combination ----

    @Test
    void addToCombination_andRemoveFromCombination() {
        Variant variant = variant(1, "SKU-1", 100_000, null, Map.of(COLOR, RED));

        variant.addToCombination(SIZE, M);
        assertEquals(Map.of(COLOR, RED, SIZE, M), variant.getCombination());

        variant.removeFromCombination(COLOR);
        assertEquals(Map.of(SIZE, M), variant.getCombination());
    }

    @Test
    void addToCombination_replacesTheValueOfAnExistingOption() {
        Variant variant = variant(1, "SKU-1", 100_000, null, Map.of(COLOR, RED));

        variant.addToCombination(COLOR, BLUE);

        assertEquals(Map.of(COLOR, BLUE), variant.getCombination());
    }

    @Test
    void hasValue_isTrueOnlyForTheVariantsOwnValueOfThatOption() {
        Variant variant = redS();

        assertTrue(variant.hasValue(COLOR, RED));
        assertFalse(variant.hasValue(COLOR, BLUE));
        assertFalse(variant.hasValue(OptionId.of(99L), RED));
    }

    @Test
    void matchesCombination_requiresTheExactSameCombination() {
        Variant variant = redS();

        assertTrue(variant.matchesCombination(Map.of(COLOR, RED, SIZE, S)));
        assertFalse(variant.matchesCombination(Map.of(COLOR, RED, SIZE, M)));
        assertFalse(variant.matchesCombination(Map.of(COLOR, RED)));
        assertFalse(variant.matchesCombination(Map.of(COLOR, RED, SIZE, S, OptionId.of(3L), ValueId.of(30L))));
        assertFalse(variant.matchesCombination(null));
    }
}
