package io.d4tzz.ecommerce.catalog.domain.product;

import io.d4tzz.ecommerce.catalog.domain.category.CategoryId;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import io.d4tzz.ecommerce.common.Money;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class ProductFixtures {

    public static final OptionId COLOR = OptionId.of(1L);
    public static final OptionId SIZE = OptionId.of(2L);
    public static final ValueId RED = ValueId.of(10L);
    public static final ValueId BLUE = ValueId.of(11L);
    public static final ValueId S = ValueId.of(20L);
    public static final ValueId M = ValueId.of(21L);

    public static final MediaId IMG_1 = MediaId.of("img-1");
    public static final MediaId IMG_2 = MediaId.of("img-2");

    /** The id of the only variant of {@link #shirt}. It is Red / S, uses {@link #IMG_1} and costs 100000. */
    public static final VariantId RED_S_ID = VariantId.of(1L);
    public static final Sku RED_S_SKU = Sku.of("SHIRT-RED-S");

    public static final SlugUniqueChecker SLUG_FREE = slug -> false;
    public static final SkuUniqueChecker SKU_FREE = sku -> false;

    private ProductFixtures() {}

    public static Variant variant(long id, String sku, long price, MediaId mediaId, Map<OptionId, ValueId> combination) {
        return Variant.restore(
            VariantId.of(id),
            Sku.of(sku),
            Money.of(price),
            Money.of(price),
            mediaId,
            Dimensions.of(10, 10, 10),
            Weight.ofGrams(500),
            combination
        );
    }

    /** A new product in DRAFT status without options, media or variants. */
    public static Product emptyDraft() {
        return Product.create(
            ProductName.of("Shirt"),
            ProductSlug.of("shirt"),
            SLUG_FREE,
            Description.of("A shirt"),
            CategoryId.of(1L),
            Map.of(),
            List.of()
        );
    }

    /**
     * A persisted shirt in the given status with options Color [Red, Blue] and Size [S, M],
     * a gallery of [img-1, img-2], and one variant Red / S.
     * The variants list given to the product is immutable on purpose: the product must copy it.
     */
    public static Product shirt(ProductStatus status) {
        LinkedHashMap<OptionId, LinkedHashSet<ValueId>> options = new LinkedHashMap<>();
        options.put(COLOR, new LinkedHashSet<>(List.of(RED, BLUE)));
        options.put(SIZE, new LinkedHashSet<>(List.of(S, M)));

        Variant redS = Variant.restore(
            RED_S_ID,
            RED_S_SKU,
            Money.of(100_000),
            Money.of(100_000),
            IMG_1,
            Dimensions.of(10, 10, 10),
            Weight.ofGrams(500),
            Map.of(COLOR, RED, SIZE, S)
        );

        return Product.restore(
            ProductId.of(1L),
            ProductName.of("Shirt"),
            ProductSlug.of("shirt"),
            Description.of("A shirt"),
            CategoryId.of(1L),
            status,
            options,
            new LinkedHashSet<>(List.of(IMG_1, IMG_2)),
            List.of(redS),
            Map.of()
        );
    }

    public static Product shirt() {
        return shirt(ProductStatus.DRAFT);
    }
}
