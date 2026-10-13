package io.d4tzz.ecommerce.catalog.domain.product;

import io.d4tzz.ecommerce.catalog.domain.category.Attribute;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryId;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;

/**
 * A sellable product: the aggregate root of the catalog.
 *
 * <p>A product belongs to one {@code Category} and has:
 * <ul>
 *   <li>a lifecycle {@link ProductStatus} (new products start as {@code DRAFT});</li>
 *   <li>category-defined attributes, as attribute code to selected value codes;</li>
 *   <li>options (such as "Color") with the values the product offers for each, in display order;</li>
 *   <li>an ordered media gallery;</li>
 *   <li>variants, each with its own SKU and price, identified by one value per product option.</li>
 * </ul>
 */
public class Product {

    private ProductId id;
    private ProductName name;
    private ProductSlug slug;
    private Description description;
    private CategoryId categoryId;
    private ProductStatus status;
    /** Option to its values. Insertion order is the display order of both the options and each option's values. */
    private LinkedHashMap<OptionId, LinkedHashSet<ValueId>> options;
    /** Gallery media. Insertion order is the display order. */
    private LinkedHashSet<MediaId> media;
    private List<Variant> variants;
    /** Attribute code to the codes of its values. Codes come from the category's attribute schema. */
    private Map<String, List<String>> attributes;

    /**
     * Rebuilds a product from persisted data without any validation.
     *
     * <p>To create a new product use {@link #create}.
     *
     * @param id       product id, {@code null} if not yet persisted
     * @param options  option to its values, in display order
     * @param media    gallery media, in display order
     * @param variants the product's variants; the list must be mutable
     */
    public Product(
        ProductId id,
        ProductName name,
        ProductSlug slug,
        Description description,
        CategoryId categoryId,
        ProductStatus status,
        SequencedMap<OptionId, ? extends SequencedSet<ValueId>> options,
        SequencedSet<MediaId> media,
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
        options.forEach((optId, valIds) -> this.options.put(optId, new LinkedHashSet<>(valIds)));
        this.media = new LinkedHashSet<>(media);
        this.variants = new ArrayList<>(variants);
        this.attributes = new LinkedHashMap<>(attributes);
    }

    /**
     * Creates a new, not yet persisted product in {@code DRAFT} status, with no options, media or variants.
     *
     * @param name        product name
     * @param slug        URL slug, must be unused
     * @param checker     tells whether a slug is already taken
     * @param description product description
     * @param categoryId  category the product belongs to
     * @param attributes  selected values per attribute code, may be {@code null} or empty
     * @param schema      the category's full attribute schema, including inherited attributes
     * @return the new product, without an id
     * @throws ConflictException        if the slug already exists
     * @throws InvalidArgumentException if the attributes do not match the schema
     */
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

    /** Rebuilds a product from persisted data without any validation. See the constructor. */
    public static Product restore(
        ProductId id,
        ProductName name,
        ProductSlug slug,
        Description description,
        CategoryId categoryId,
        ProductStatus status,
        SequencedMap<OptionId, ? extends SequencedSet<ValueId>> options,
        SequencedSet<MediaId> media,
        List<Variant> variants,
        Map<String, List<String>> attributes
    ) {
        return new Product(id, name, slug, description, categoryId, status, options, media, variants, attributes);
    }

    /** Changes the name of this product. */
    public void rename(ProductName name) {
        this.name = name;
    }

    /**
     * Changes the slug of this product. Does nothing if the slug is unchanged.
     *
     * @param newSlug the new slug
     * @param checker tells whether a slug is already taken
     * @throws ConflictException if another product already uses the new slug
     */
    public void changeSlug(ProductSlug newSlug, SlugUniqueChecker checker) {
        if (this.slug.equals(newSlug)) {
            return;
        }

        if (checker.exists(newSlug)) {
            throw new ConflictException("Product slug '" + newSlug.value() + "' already exists");
        }

        this.slug = newSlug;
    }

    /** Changes the description. A {@code null} description is stored as an empty one. */
    public void changeDescription(Description description) {
        this.description = description != null ? description : Description.of("");
    }

    /**
     * Moves the product to another category and replaces its attributes.
     *
     * @param categoryId the new category
     * @param attributes selected values per attribute code for the new category
     * @param schema     the new category's full attribute schema, including inherited attributes
     * @throws InvalidArgumentException if the attributes do not match the schema
     */
    public void changeCategory(CategoryId categoryId, Map<String, List<String>> attributes, List<Attribute> schema) {
        this.attributes = validateAttributes(attributes, schema);
        this.categoryId = categoryId;
    }

    /** Adds a media item to the end of the gallery. Does nothing if it is already there. */
    public void addMedia(MediaId mediaId) {
        this.media.add(mediaId);
    }

    /**
     * Removes a media item from the gallery. Does nothing if it is not in the gallery.
     *
     * @throws InvalidArgumentException if a variant uses the media
     */
    public void removeMedia(MediaId mediaId) {
        boolean inUse = this.variants.stream().anyMatch(v -> mediaId.equals(v.getMediaId()));
        if (inUse) {
            throw new InvalidArgumentException("Cannot remove media because it is in use by existing variants");
        }
        this.media.remove(mediaId);
    }

    /**
     * Sets the gallery order.
     *
     * @param newOrder every gallery media exactly once, in the new order
     * @throws InvalidArgumentException if {@code newOrder} has duplicates, nulls, unknown media,
     *                                  or does not contain all gallery media
     */
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

    /**
     * Sets or clears the media of a variant.
     *
     * @param mediaId a media item from the gallery, or {@code null} to clear
     * @return the updated variant
     * @throws NotFoundException if the media is not in the gallery, or the variant does not exist
     */
    public Variant updateVariantMedia(VariantId variantId, MediaId mediaId) {
        if (mediaId != null && !this.media.contains(mediaId)) {
            throw new NotFoundException("Media '" + mediaId.value() + "' is not in product gallery");
        }
        Variant variant = findVariantById(variantId);
        variant.changeMedia(mediaId);
        return variant;
    }

    /**
     * Adds an option to the product with one initial value, and adds that value to the
     * combination of every existing variant.
     *
     * @param defaultValueId the option's first value, which existing variants get
     * @throws ConflictException if the option is already on the product
     */
    public void addOption(OptionId optionId, ValueId defaultValueId) {
        if (options.containsKey(optionId)) {
            throw new ConflictException("Option already added to product");
        }

        this.options.put(optionId, new LinkedHashSet<>(List.of(defaultValueId)));

        for (Variant variant : variants) {
            variant.addToCombination(optionId, defaultValueId);
        }
    }

    /**
     * Adds a value to an option of the product.
     *
     * @throws NotFoundException if the option is not on the product
     * @throws ConflictException if the value is already in the option
     */
    public void addValue(OptionId optionId, ValueId valueId) {
        Set<ValueId> values = this.options.get(optionId);
        if (values == null) {
            throw new NotFoundException("Option not found on product");
        }
        if (!values.add(valueId)) {
            throw new ConflictException("Value already existed before");
        }
    }

    /**
     * Removes a value from an option of the product. If it was the option's last value,
     * the option is removed from the product too.
     *
     * @throws NotFoundException         if the option is not on the product, or the value is not in the option
     * @throws InvalidOperationException if a variant uses the value
     */
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

    /**
     * Sets the display order of an option's values.
     *
     * @param newOrder every value of the option exactly once, in the new order
     * @throws NotFoundException        if the option is not on the product
     * @throws InvalidArgumentException if {@code newOrder} has duplicates, nulls, unknown values,
     *                                  or does not contain all the option's values
     */
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

    /**
     * Removes an option from the product and from the combination of every variant.
     *
     * @throws NotFoundException         if the option is not on the product
     * @throws InvalidOperationException if the option still has more than one value
     */
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

    /**
     * Sets the display order of the product's options.
     *
     * @param newOrder every option of the product exactly once, in the new order
     * @throws InvalidArgumentException if {@code newOrder} has duplicates, nulls, unknown options,
     *                                  or does not contain all the product's options
     */
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

    /**
     * Adds a variant to the product.
     *
     * @param mediaId     a media item from the gallery, or {@code null} for none
     * @param combination one value for each option of the product; may be {@code null} or empty
     *                    only if the product has no options
     * @param checker     tells whether a SKU is already used by any product
     * @return the new variant, without an id until the product is saved
     * @throws NotFoundException        if the media is not in the gallery
     * @throws ConflictException        if the SKU is taken, or another variant has the same combination
     * @throws InvalidArgumentException if the combination does not cover exactly the product's options
     *                                  and their values
     */
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

    /**
     * Changes the dimensions of a variant.
     *
     * @return the updated variant
     * @throws NotFoundException if the variant does not exist
     */
    public Variant updateVariantDimensions(VariantId variantId, Dimensions dimensions) {
        Variant variant = findVariantById(variantId);
        variant.changeDimensions(dimensions);
        return variant;
    }

    /**
     * Changes the weight of a variant.
     *
     * @return the updated variant
     * @throws NotFoundException if the variant does not exist
     */
    public Variant updateVariantWeight(VariantId variantId, Weight weight) {
        Variant variant = findVariantById(variantId);
        variant.changeWeight(weight);
        return variant;
    }

    /**
     * Removes a variant. Does nothing if the product has no variant with that id.
     *
     * @throws InvalidOperationException if the product is not in {@code DRAFT} status
     */
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

    /**
     * Changes the price of a variant.
     *
     * @param newPrice     the new price
     * @param newCompareAt the new compare-at price
     * @return the updated variant
     * @throws NotFoundException if the variant does not exist
     */
    public Variant updateVariantPrice(VariantId variantId, Money newPrice, Money newCompareAt) {
        Variant variant = findVariantById(variantId);
        variant.changePrice(newPrice, newCompareAt);
        return variant;
    }

    /**
     * Changes the SKU of a variant. Does nothing if the SKU is unchanged.
     *
     * @param checker tells whether a SKU is already used by any product
     * @throws NotFoundException if the variant does not exist
     * @throws ConflictException if the SKU is already taken
     */
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

    /**
     * Replaces the product's attributes.
     *
     * @param attributes selected values per attribute code
     * @param schema     the product category's full attribute schema, including inherited attributes
     * @throws InvalidArgumentException if the attributes do not match the schema
     */
    public void updateAttributes(Map<String, List<String>> attributes, List<Attribute> schema) {
        this.attributes = validateAttributes(attributes, schema);
    }

    /**
     * Checks the attributes against the category schema: every code must exist in the schema and
     * its values must be valid for the attribute. A {@code null} map is treated as empty.
     * Attributes of the schema that are left out are allowed.
     *
     * @throws InvalidArgumentException if the schema is empty but attributes are given,
     *                                  or an attribute is unknown or has invalid values
     */
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

        Map<String, Attribute> schemaMap = HashMap.newHashMap(schema.size());
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

    /** Sets the status to {@code PUBLISHED}. Does nothing if already published. */
    public void publish() {
        if (this.status == ProductStatus.PUBLISHED) {
            return;
        }
        this.status = ProductStatus.PUBLISHED;
    }

    /** Sets the status to {@code ARCHIVED}. Does nothing if already archived. */
    public void archive() {
        if (this.status == ProductStatus.ARCHIVED) {
            return;
        }
        this.status = ProductStatus.ARCHIVED;
    }

    /**
     * Checks that the product is a draft; a product cannot go back to {@code DRAFT} once it has left it.
     *
     * @throws InvalidOperationException if the product is not in {@code DRAFT} status
     */
    public void draft() {
        if (this.status == ProductStatus.DRAFT) {
            return;
        }
        throw new InvalidOperationException("Cannot transition product back to DRAFT from " + this.status);
    }

    /**
     * Checks that the product may be deleted.
     *
     * @throws InvalidOperationException if the product is not in {@code DRAFT} status
     */
    public void ensureCanDelete() {
        if (this.status != ProductStatus.DRAFT) {
            throw new InvalidOperationException(
                "Cannot delete product in " + this.status + " status. Only DRAFT products can be deleted."
            );
        }
    }

    /** Tells whether one of this product's variants has the given SKU. */
    public boolean hasVariantWithSku(Sku sku) {
        return this.variants.stream().anyMatch(v -> v.getSku().equals(sku));
    }

    /** Tells whether one of this product's variants has exactly the given option-value combination. */
    public boolean hasVariantWithCombination(Map<OptionId, ValueId> combination) {
        return this.variants.stream().anyMatch(v -> v.matchesCombination(combination));
    }

    /**
     * Finds a variant of this product.
     *
     * @throws NotFoundException if the product has no variant with that id
     */
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

    /** Returns each option with its values, in display order, as an unmodifiable view. */
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

    /** Returns the lowest variant price, or empty if the product has no variants. */
    public Optional<Money> getMinPrice() {
        return this.variants.stream().map(Variant::getPrice).min(Comparator.naturalOrder());
    }

    /** Returns the highest variant price, or empty if the product has no variants. */
    public Optional<Money> getMaxPrice() {
        return this.variants.stream().map(Variant::getPrice).max(Comparator.naturalOrder());
    }

    public Map<String, List<String>> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
