package io.d4tzz.ecommerce.catalog.domain.product;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Variant {

    private VariantId id;
    private Sku sku;
    private Money price;
    private Money compareAt;
    private MediaId mediaId;
    private Dimensions dimensions;
    private Weight weight;
    private Map<OptionId, ValueId> combination;

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
        this.sku = Objects.requireNonNull(sku, "Sku cannot be null");
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        if (compareAt != null && compareAt.compareTo(price) < 0) {
            throw new InvalidArgumentException("Compare at price cannot be less than price");
        }
        this.compareAt = compareAt;
        this.dimensions = Objects.requireNonNull(dimensions, "Dimensions cannot be null");
        this.weight = Objects.requireNonNull(weight, "Weight cannot be null");
        this.mediaId = mediaId;
        this.combination = combination != null ? new HashMap<>(combination) : new HashMap<>();
    }

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

    void changePrice(Money newPrice, Money newCompareAt) {
        if (newCompareAt != null && newCompareAt.compareTo(newPrice) < 0) {
            throw new InvalidArgumentException("Compare at price cannot be less than price");
        }
        this.price = newPrice;
        this.compareAt = newCompareAt;
    }

    void changePrice(Money newPrice) {
        changePrice(newPrice, null);
    }

    void changeSku(Sku newSku) {
        this.sku = Objects.requireNonNull(newSku, "Sku cannot be null");
    }

    void changeMedia(MediaId newMediaId) {
        this.mediaId = newMediaId;
    }

    void changeDimensions(Dimensions newDimensions) {
        this.dimensions = newDimensions;
    }

    void changeWeight(Weight newWeight) {
        this.weight = Objects.requireNonNull(newWeight, "Weight cannot be null");
    }

    void addToCombination(OptionId optionId, ValueId valueId) {
        this.combination.put(optionId, valueId);
    }

    void removeFromCombination(OptionId optionId) {
        this.combination.remove(optionId);
    }

    public boolean hasValue(OptionId optionId, ValueId valueId) {
        return valueId.equals(this.combination.get(optionId));
    }

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

    public Map<OptionId, ValueId> getCombination() {
        return Collections.unmodifiableMap(combination);
    }
}
