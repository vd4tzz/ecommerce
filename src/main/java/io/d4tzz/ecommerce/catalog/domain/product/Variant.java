package io.d4tzz.ecommerce.catalog.domain.product;

import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A purchasable variant of a {@link Product}, such as "Red / Large".
 *
 * <p>A variant has its own SKU, price, dimensions and weight. Its {@code combination} maps each
 * option of the product to one of that option's values. A variant is owned by its product, so it is
 * changed through the product, which is why most mutators here are package-private.
 */
public class Variant {

    private VariantId id;
    private Sku sku;
    private Money price;
    private Money compareAt;
    private MediaId mediaId;
    private Dimensions dimensions;
    private Weight weight;
    private Map<OptionId, ValueId> combination;

    /**
     * Validates and sets all fields.
     *
     * @param compareAt   optional reference price, {@code null} for none; must not be lower than {@code price}
     * @param mediaId     optional gallery media, {@code null} for none
     * @param combination option to value of this variant; {@code null} is treated as empty
     * @throws InvalidArgumentException if {@code compareAt} is lower than {@code price}
     */
    private Variant(
        VariantId id,
        Sku sku,
        Money price,
        Money compareAt,
        MediaId mediaId,
        Dimensions dimensions,
        Weight weight,
        Map<OptionId, ValueId> combination
    ) {
        this.id = id;
        this.sku = sku;
        this.price = price;
        if (compareAt != null && compareAt.compareTo(price) < 0) {
            throw new InvalidArgumentException("Compare at price cannot be less than price");
        }
        this.compareAt = compareAt;
        this.dimensions = dimensions;
        this.weight = weight;
        this.mediaId = mediaId;
        this.combination = combination != null ? new HashMap<>(combination) : new HashMap<>();
    }

    /**
     * Creates a new, not yet persisted variant. Its compare-at price starts equal to {@code price}.
     *
     * @return the new variant, without an id
     */
    public static Variant create(
        Sku sku,
        Money price,
        MediaId mediaId,
        Dimensions dimensions,
        Weight weight,
        Map<OptionId, ValueId> combination
    ) {
        return new Variant(null, sku, price, price, mediaId, dimensions, weight, combination);
    }

    /**
     * Rebuilds a variant from persisted data. Applies the same validation as the constructor.
     *
     * @throws InvalidArgumentException if {@code compareAt} is lower than {@code price}
     */
    public static Variant restore(
        VariantId id,
        Sku sku,
        Money price,
        Money compareAt,
        MediaId mediaId,
        Dimensions dimensions,
        Weight weight,
        Map<OptionId, ValueId> combination
    ) {
        return new Variant(id, sku, price, compareAt, mediaId, dimensions, weight, combination);
    }

    /**
     * Changes the price and the compare-at price.
     *
     * @param newCompareAt the new compare-at price, {@code null} for none; must not be lower than {@code newPrice}
     * @throws InvalidArgumentException if {@code newCompareAt} is lower than {@code newPrice}
     */
    void changePrice(Money newPrice, Money newCompareAt) {
        if (newCompareAt != null && newCompareAt.compareTo(newPrice) < 0) {
            throw new InvalidArgumentException("Compare at price cannot be less than price");
        }
        this.price = newPrice;
        this.compareAt = newCompareAt;
    }

    /** Changes the price and clears the compare-at price. */
    void changePrice(Money newPrice) {
        changePrice(newPrice, null);
    }

    /**
     * Changes the SKU. Uniqueness is not checked here, see {@link Product#updateVariantSku}.
     *
     */
    void changeSku(Sku newSku) {
        this.sku = newSku;
    }

    /** Sets the media of this variant, {@code null} to clear it. Gallery membership is not checked here. */
    void changeMedia(MediaId newMediaId) {
        this.mediaId = newMediaId;
    }

    /** Changes the dimensions. */
    void changeDimensions(Dimensions newDimensions) {
        this.dimensions = newDimensions;
    }

    /**
     * Changes the weight.
     *
     */
    void changeWeight(Weight newWeight) {
        this.weight = newWeight;
    }

    /** Sets the value of an option in the combination, replacing any previous value for that option. */
    void addToCombination(OptionId optionId, ValueId valueId) {
        this.combination.put(optionId, valueId);
    }

    /** Removes an option from the combination. */
    void removeFromCombination(OptionId optionId) {
        this.combination.remove(optionId);
    }

    /** Tells whether this variant uses the given value for the given option. */
    public boolean hasValue(OptionId optionId, ValueId valueId) {
        return valueId.equals(this.combination.get(optionId));
    }

    /** Tells whether this variant's combination is exactly the given one. A {@code null} combination never matches. */
    public boolean matchesCombination(Map<OptionId, ValueId> otherCombination) {
        if (otherCombination == null) return false;
        return this.combination.equals(otherCombination);
    }

    public VariantId getId() {
        return id;
    }

    public Sku getSku() {
        return sku;
    }

    public Money getPrice() {
        return price;
    }

    public Money getCompareAt() {
        return compareAt;
    }

    public MediaId getMediaId() {
        return mediaId;
    }

    public Dimensions getDimensions() {
        return dimensions;
    }

    public Weight getWeight() {
        return weight;
    }

    /** Returns the option to value map of this variant, as an unmodifiable view. */
    public Map<OptionId, ValueId> getCombination() {
        return Collections.unmodifiableMap(combination);
    }
}
