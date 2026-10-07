package io.d4tzz.ecommerce.catalog.domain.product;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.category.Attribute;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryId;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Product {

    private ProductId id;
    private ProductName name;
    private ProductSlug slug;
    private Description description;
    private CategoryId categoryId;
    private ProductStatus status;
    private LinkedHashMap<OptionId, LinkedHashSet<ValueId>> options;
    private LinkedHashSet<MediaId> media;
    private List<Variant> variants;
    private Map<String, List<String>> attributes;

    public Product(
        ProductId id,
        ProductName name,
        ProductSlug slug,
        Description description,
        CategoryId categoryId,
        ProductStatus status,
        Map<OptionId, Set<ValueId>> options,
        Set<MediaId> media,
        List<Variant> variants,
        Map<String, List<String>> attributes
    ) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.categoryId = categoryId;
        this.status = status;

        this.options = new LinkedHashMap<>();
        if (options != null) {
            options.forEach((optId, valIds) ->
                this.options.put(optId, valIds != null ? new LinkedHashSet<>(valIds) : new LinkedHashSet<>())
            );
        }

        this.media = media != null ? new LinkedHashSet<>(media) : new LinkedHashSet<>();
        this.variants = variants;
        this.attributes = new LinkedHashMap<>(attributes);
    }

    public static Product create(
        ProductName name,
        ProductSlug slug,
        SlugUniqueChecker checker,
        Description description,
        CategoryId categoryId,
        Map<String, List<String>> attributes,
        List<Attribute> schema
    ) {
        if (checker.exists(slug)) {
            throw new ConflictException("Product slug '" + slug.value() + "' already exists");
        }

        Map<String, List<String>> validatedAttributes = validateAttributes(attributes, schema);

        return new Product(
            null,
            name,
            slug,
            description,
            categoryId,
            ProductStatus.DRAFT,
            new LinkedHashMap<>(),
            new LinkedHashSet<>(),
            new ArrayList<>(),
            validatedAttributes
        );
    }

    public static Product restore(
        ProductId id,
        ProductName name,
        ProductSlug slug,
        Description description,
        CategoryId categoryId,
        ProductStatus status,
        Map<OptionId, Set<ValueId>> options,
        Set<MediaId> media,
        List<Variant> variants,
        Map<String, List<String>> attributes
    ) {
        return new Product(id, name, slug, description, categoryId, status, options, media, variants, attributes);
    }

    public void rename(ProductName name) {
        this.name = name;
    }

    public void changeSlug(ProductSlug newSlug, SlugUniqueChecker checker) {
        if (this.slug.equals(newSlug)) {
            return;
        }

        if (checker.exists(newSlug)) {
            throw new ConflictException("Product slug '" + newSlug.value() + "' already exists");
        }

        this.slug = newSlug;
    }

    public void changeDescription(Description description) {
        this.description = description != null ? description : Description.of("");
    }

    public void changeCategory(CategoryId categoryId, Map<String, List<String>> attributes, List<Attribute> schema) {
        this.attributes = validateAttributes(attributes, schema);
        this.categoryId = categoryId;
    }

    public void addMedia(MediaId mediaId) {
        this.media.add(mediaId);
    }

    public void removeMedia(MediaId mediaId) {
        boolean inUse = this.variants.stream().anyMatch(v -> mediaId.equals(v.getMediaId()));
        if (inUse) {
            throw new InvalidArgumentException("Cannot remove media because it is in use by existing variants");
        }
        this.media.remove(mediaId);
    }

    public void reorderMedia(List<MediaId> newOrder) {
        if (newOrder.size() != this.media.size()) {
            throw new InvalidArgumentException("New order must contain exactly the existing media without duplicates");
        }

        LinkedHashSet<MediaId> newOrderSet = LinkedHashSet.newLinkedHashSet(newOrder.size());
        for (MediaId m : newOrder) {
            if (m == null || !this.media.contains(m) || !newOrderSet.add(m)) {
                throw new InvalidArgumentException(
                    "New order must contain exactly the existing media without duplicates"
                );
            }
        }
        this.media = newOrderSet;
    }

    public Variant updateVariantMedia(VariantId variantId, MediaId mediaId) {
        if (mediaId != null && !this.media.contains(mediaId)) {
            throw new NotFoundException("Media '" + mediaId.value() + "' is not in product gallery");
        }
        Variant variant = findVariantById(variantId);
        variant.changeMedia(mediaId);
        return variant;
    }

    public void addOption(OptionId optionId, ValueId defaultValueId) {
        if (options.containsKey(optionId)) {
            throw new ConflictException("Option already added to product");
        }

        this.options.put(optionId, new LinkedHashSet<>(List.of(defaultValueId)));

        for (Variant variant : variants) {
            variant.addToCombination(optionId, defaultValueId);
        }
    }

    public void addValue(OptionId optionId, ValueId valueId) {
        Set<ValueId> values = this.options.get(optionId);
        if (values == null) {
            throw new NotFoundException("Option not found on product");
        }
        if (!values.add(valueId)) {
            throw new ConflictException("Value already existed before");
        }
    }

    public void removeValue(OptionId optionId, ValueId valueId) {
        Set<ValueId> values = this.options.get(optionId);
        if (values == null) {
            throw new NotFoundException("Option not found on product");
        }
        if (!values.contains(valueId)) {
            throw new NotFoundException("Value not found in option");
        }

        boolean inUse = this.variants.stream().anyMatch(v -> v.hasValue(optionId, valueId));
        if (inUse) {
            throw new InvalidOperationException("Cannot remove value because it is in use by existing variants");
        }
        values.remove(valueId);

        if (values.isEmpty()) {
            this.options.remove(optionId);
        }
    }

    public void reorderOptionValues(OptionId optionId, List<ValueId> newOrder) {
        Set<ValueId> values = this.options.get(optionId);
        if (values == null) {
            throw new NotFoundException("Option not found on product");
        }
        if (newOrder.size() != values.size()) {
            throw new InvalidArgumentException(
                "New order must contain exactly the existing values of the option without duplicates"
            );
        }

        LinkedHashSet<ValueId> newOrderSet = LinkedHashSet.newLinkedHashSet(newOrder.size());
        for (ValueId val : newOrder) {
            if (val == null || !values.contains(val) || !newOrderSet.add(val)) {
                throw new InvalidArgumentException(
                    "New order must contain exactly the existing values of the option without duplicates"
                );
            }
        }
        this.options.put(optionId, newOrderSet);
    }

    public void removeOption(OptionId optionId) {
        Set<ValueId> values = this.options.get(optionId);
        if (values == null) {
            throw new NotFoundException("Option not found on product");
        }
        if (values.size() > 1) {
            throw new InvalidOperationException("Cannot remove option with multiple values. Remove values first.");
        }

        this.options.remove(optionId);
        for (Variant variant : variants) {
            variant.removeFromCombination(optionId);
        }
    }

    public void reorderOptions(List<OptionId> newOrder) {
        if (newOrder.size() != this.options.size()) {
            throw new InvalidArgumentException(
                "New order must contain exactly the existing options without duplicates"
            );
        }

        LinkedHashMap<OptionId, LinkedHashSet<ValueId>> reorder = LinkedHashMap.newLinkedHashMap(newOrder.size());
        for (OptionId optId : newOrder) {
            if (optId == null) {
                throw new InvalidArgumentException("New order cannot contain null elements");
            }

            LinkedHashSet<ValueId> values = this.options.get(optId);
            if (values == null || reorder.put(optId, values) != null) {
                throw new InvalidArgumentException(
                    "New order must contain exactly the existing options without duplicates"
                );
            }
        }
        this.options = reorder;
    }

    public Variant addVariant(
        Sku sku,
        Money price,
        MediaId mediaId,
        Dimensions dimensions,
        Weight weight,
        Map<OptionId, ValueId> combination,
        SkuUniqueChecker checker
    ) {
        if (mediaId != null && !this.media.contains(mediaId)) {
            throw new NotFoundException("Media '" + mediaId.value() + "' is not in product gallery");
        }

        combination = combination != null ? combination : Collections.emptyMap();

        if (hasVariantWithSku(sku) || checker.exists(sku)) {
            throw new ConflictException("Variant with sku '" + sku.value() + "' already exists");
        }

        if (combination.size() != this.options.size()) {
            throw new InvalidArgumentException("Invalid variant combination");
        }
        for (Map.Entry<OptionId, ValueId> entry : combination.entrySet()) {
            Set<ValueId> availableValues = this.options.get(entry.getKey());
            if (availableValues == null || !availableValues.contains(entry.getValue())) {
                throw new InvalidArgumentException("Option and value must exist in product options");
            }
        }

        if (hasVariantWithCombination(combination)) {
            throw new ConflictException("Variant with the same combination already exists");
        }

        Variant variant = Variant.create(sku, price, mediaId, dimensions, weight, combination);
        this.variants.add(variant);

        return variant;
    }

    public Variant updateVariantDimensions(VariantId variantId, Dimensions dimensions) {
        Variant variant = findVariantById(variantId);
        variant.changeDimensions(dimensions);
        return variant;
    }

    public Variant updateVariantWeight(VariantId variantId, Weight weight) {
        Variant variant = findVariantById(variantId);
        variant.changeWeight(weight);
        return variant;
    }

    public void removeVariant(VariantId variantId) {
        if (this.status != ProductStatus.DRAFT) {
            throw new InvalidOperationException(
                "Cannot remove variant when product is in " +
                    this.status +
                    " status. Only DRAFT products allow variant removal."
            );
        }
        this.variants.removeIf(v -> variantId.equals(v.getId()));
    }

    public Variant updateVariantPrice(VariantId variantId, Money newPrice, Money newCompareAt) {
        Variant variant = findVariantById(variantId);
        variant.changePrice(newPrice, newCompareAt);
        return variant;
    }

    public void updateVariantSku(VariantId variantId, Sku newSku, SkuUniqueChecker checker) {
        Variant variant = findVariantById(variantId);
        if (variant.getSku().equals(newSku)) {
            return;
        }

        if (hasVariantWithSku(newSku) || checker.exists(newSku)) {
            throw new ConflictException("Variant with sku '" + newSku.value() + "' already exists");
        }

        variant.changeSku(newSku);
    }

    public void updateAttributes(Map<String, List<String>> attributes, List<Attribute> schema) {
        this.attributes = validateAttributes(attributes, schema);
    }

    private static Map<String, List<String>> validateAttributes(
        Map<String, List<String>> attributes,
        List<Attribute> schema
    ) {
        if (attributes == null) {
            attributes = Collections.emptyMap();
        }

        if (schema.isEmpty()) {
            if (!attributes.isEmpty()) {
                throw new InvalidArgumentException("Category has no attributes, cannot provide attributes");
            }
            return Collections.emptyMap();
        }

        Map<String, Attribute> schemaMap = new HashMap<>(schema.size());
        for (Attribute attr : schema) {
            schemaMap.put(attr.getCode(), attr);
        }

        for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {
            Attribute attr = schemaMap.get(entry.getKey());
            if (attr == null) {
                throw new InvalidArgumentException("Unknown attribute: " + entry.getKey());
            }
            attr.validateValue(entry.getValue());
        }

        return new LinkedHashMap<>(attributes);
    }

    public void publish() {
        if (this.status == ProductStatus.PUBLISHED) {
            return;
        }
        this.status = ProductStatus.PUBLISHED;
    }

    public void archive() {
        if (this.status == ProductStatus.ARCHIVED) {
            return;
        }
        this.status = ProductStatus.ARCHIVED;
    }

    public void draft() {
        if (this.status == ProductStatus.DRAFT) {
            return;
        }
        throw new InvalidOperationException("Cannot transition product back to DRAFT from " + this.status);
    }

    public void ensureCanDelete() {
        if (this.status != ProductStatus.DRAFT) {
            throw new InvalidOperationException(
                "Cannot delete product in " + this.status + " status. Only DRAFT products can be deleted."
            );
        }
    }

    public boolean hasVariantWithSku(Sku sku) {
        return this.variants.stream().anyMatch(v -> v.getSku().equals(sku));
    }

    public boolean hasVariantWithCombination(Map<OptionId, ValueId> combination) {
        return this.variants.stream().anyMatch(v -> v.matchesCombination(combination));
    }

    public Variant findVariantById(VariantId variantId) {
        return this.variants
            .stream()
            .filter(v -> variantId.equals(v.getId()))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Variant not found: " + variantId.value()));
    }

    public ProductId getId() {
        return id;
    }

    public ProductName getName() {
        return name;
    }

    public ProductSlug getSlug() {
        return slug;
    }

    public Description getDescription() {
        return description;
    }

    public CategoryId getCategoryId() {
        return categoryId;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public Map<OptionId, Set<ValueId>> getOptions() {
        Map<OptionId, Set<ValueId>> unmodifiable = new LinkedHashMap<>();
        options.forEach((optId, valIds) -> unmodifiable.put(optId, Collections.unmodifiableSet(valIds)));
        return Collections.unmodifiableMap(unmodifiable);
    }

    public Set<MediaId> getMedia() {
        return Collections.unmodifiableSet(media);
    }

    public List<Variant> getVariants() {
        return Collections.unmodifiableList(variants);
    }

    public Optional<Money> getMinPrice() {
        return this.variants.stream().map(Variant::getPrice).min(Comparator.naturalOrder());
    }

    public Optional<Money> getMaxPrice() {
        return this.variants.stream().map(Variant::getPrice).max(Comparator.naturalOrder());
    }

    public Map<String, List<String>> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
