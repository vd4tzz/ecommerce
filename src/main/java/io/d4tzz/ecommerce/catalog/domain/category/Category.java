package io.d4tzz.ecommerce.catalog.domain.category;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A node in the product category tree.
 *
 * <p>The tree is stored as a materialized path: {@code path} holds the ids of all ancestors, each
 * followed by {@link #PATH_DELIMITER} (for example {@code "1.5."}), and is {@code null} for a root
 * category. Each category owns a schema of {@link Attribute}s; the full schema of a category is
 * its own attributes plus those inherited from its ancestors.
 */
public class Category {

    /** Separator between ids in {@code path}. */
    public static String PATH_DELIMITER = ".";

    private CategoryId id;
    private CategoryName name;
    private CategorySlug slug;
    private String path;
    private CategoryId parentId;
    private List<Attribute> attributes;

    /**
     * Rebuilds a category from persisted data without any validation.
     *
     * <p>To create a new category use {@link #create}.
     *
     * @param id         category id, {@code null} if not yet persisted
     * @param path       ancestors' ids joined by {@link #PATH_DELIMITER}, {@code null} for a root category
     * @param parentId   id of the parent category, {@code null} for a root category
     * @param attributes attributes defined by this category itself, excluding inherited ones
     */
    public Category(
        CategoryId id,
        CategoryName name,
        CategorySlug slug,
        String path,
        CategoryId parentId,
        List<Attribute> attributes
    ) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.path = path;
        this.parentId = parentId;
        this.attributes = attributes;
    }

    /**
     * Creates a new, not yet persisted category.
     *
     * @param name           category name
     * @param slug           URL slug, must be unused
     * @param checker        tells whether a slug is already taken
     * @param parent         parent category, {@code null} to create a root category; must already be persisted
     * @param attributes     attributes defined by the new category
     * @param ancestorSchema all attributes inherited from the parent and its ancestors, empty for a root category
     * @return the new category, without an id
     * @throws ConflictException         if the slug already exists, an attribute code duplicates another
     *                                   or is already inherited from an ancestor
     * @throws InvalidOperationException if {@code parent} has no id
     */
    public static Category create(
        CategoryName name,
        CategorySlug slug,
        CategorySlugChecker checker,
        Category parent,
        List<Attribute> attributes,
        List<Attribute> ancestorSchema
    ) {
        if (checker.exists(slug)) {
            throw new ConflictException("Category slug '" + slug.value() + "' already exists");
        }
        List<Attribute> validatedAttributes = validateAttributes(attributes, ancestorSchema, Collections.emptyList());
        return new Category(
            null,
            name,
            slug,
            parent == null ? null : parent.prefix(),
            parent == null ? null : parent.getId(),
            validatedAttributes
        );
    }

    /**
     * Returns the path that this category's children have: this category's path followed by its own id
     * and {@link #PATH_DELIMITER}, for example {@code "1.5."}.
     *
     * @throws InvalidOperationException if this category has no id yet
     */
    public String prefix() {
        if (id == null) {
            throw new InvalidOperationException("Cannot generate prefix for category without ID");
        }
        return (path == null ? "" : path) + id.toString() + PATH_DELIMITER;
    }

    /**
     * Moves this category under another parent.
     *
     * <p>Only this category's {@code path} and {@code parentId} change. The paths of its descendants
     * are not updated here.
     *
     * @param parent the new parent category
     * @throws InvalidOperationException if the parent is this category or one of its descendants
     */
    public void move(Category parent) {
        if (this.id != null) {
            if (this.id.equals(parent.getId())) {
                throw new InvalidOperationException("Cannot move category into itself");
            }
            if (parent.prefix().startsWith(this.prefix())) {
                throw new InvalidOperationException("Cannot move category into its own descendant");
            }
        }
        this.path = parent.prefix();
        this.parentId = parent.getId();
    }

    /** Changes the name of this category. */
    public void rename(CategoryName name) {
        this.name = name;
    }

    /**
     * Changes the slug of this category. Does nothing if the slug is unchanged.
     *
     * @param newSlug the new slug
     * @param checker tells whether a slug is already taken
     * @throws ConflictException if another category already uses the new slug
     */
    public void changeSlug(CategorySlug newSlug, CategorySlugChecker checker) {
        if (this.slug.equals(newSlug)) {
            return;
        }

        if (checker.exists(newSlug)) {
            throw new ConflictException("Category slug '" + newSlug.value() + "' already exists");
        }

        this.slug = newSlug;
    }

    /**
     * Updates the attribute schema of this category.
     *
     * <p>Rules enforced:
     * <ul>
     *   <li>Attribute codes must not conflict with those inherited from ancestors or propagated to descendants.</li>
     *   <li>An existing attribute's type cannot be changed.</li>
     *   <li>Existing value codes of an attribute cannot be removed — only new ones may be added.</li>
     *   <li>Existing attributes cannot be deleted.</li>
     * </ul>
     *
     * @param newAttributes   the complete new attribute list of this category, including existing attributes
     * @param ancestorSchema  all attributes inherited from the parent and its ancestors
     * @param descendantSchema all attributes defined by descendant categories
     * @throws ConflictException         if an attribute code is duplicated, inherited from an ancestor,
     *                                   or already used by a descendant
     * @throws InvalidOperationException if an existing attribute is removed, changes type,
     *                                   or loses an existing value
     */
    public void updateAttributes(
        List<Attribute> newAttributes,
        List<Attribute> ancestorSchema,
        List<Attribute> descendantSchema
    ) {
        // ensure newAttributes are different to ancestor and descendant
        List<Attribute> validated = validateAttributes(newAttributes, ancestorSchema, descendantSchema);

        Map<String, Attribute> existingByCode = HashMap.newHashMap(this.attributes.size());
        for (Attribute attr : this.attributes) {
            existingByCode.put(attr.getCode(), attr);
        }

        for (Attribute attr : validated) {
            Attribute existing = existingByCode.remove(attr.getCode());
            if (existing != null) {
                if (existing.getType() != attr.getType()) {
                    throw new InvalidOperationException("Cannot change type of existing attribute: " + attr.getCode());
                }

                // ensure the codes of an attribute only add new and cannot change existings.
                Set<String> existingValueCodes = existing.getCodes();
                Set<String> newValueCodes = attr.getCodes();
                if (!newValueCodes.containsAll(existingValueCodes)) {
                    Set<String> missingValues = new HashSet<>(existingValueCodes);
                    missingValues.removeAll(newValueCodes);
                    throw new InvalidOperationException(
                        "Values of attribute '" +
                            attr.getCode() +
                            "' cannot be deleted once created. Missing: " +
                            String.join(", ", missingValues)
                    );
                }
            }
        }

        // the new attributes do not contain all exsiting attributes, existingByCode will not empty
        if (!existingByCode.isEmpty()) {
            String missingCodes = String.join(", ", existingByCode.keySet());
            throw new InvalidOperationException("Attributes cannot be deleted once created. Missing: " + missingCodes);
        }

        this.attributes = validated;
    }

    private static List<Attribute> validateAttributes(
        List<Attribute> attributes,
        List<Attribute> ancestorSchema,
        List<Attribute> descendantSchema
    ) {
        if (attributes.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> ancestorCodes = ancestorSchema.isEmpty()
            ? Collections.emptySet()
            : ancestorSchema.stream().map(Attribute::getCode).collect(Collectors.toSet());

        Set<String> descendantCodes = descendantSchema.isEmpty()
            ? Collections.emptySet()
            : descendantSchema.stream().map(Attribute::getCode).collect(Collectors.toSet());

        Set<String> seenCodes = HashSet.newHashSet(attributes.size());
        for (Attribute attr : attributes) {
            String code = attr.getCode();
            if (ancestorCodes.contains(code)) {
                throw new ConflictException(
                    "Attribute code '" + code + "' is already inherited from an ancestor category"
                );
            }
            if (descendantCodes.contains(code)) {
                throw new ConflictException("Attribute code '" + code + "' is already used by a descendant category");
            }
            if (!seenCodes.add(code)) {
                throw new ConflictException("Duplicate attribute code: " + code);
            }
        }

        return new ArrayList<>(attributes);
    }

    public Optional<Attribute> getAttribute(String code) {
        return this.attributes
            .stream()
            .filter(a -> a.getCode().equalsIgnoreCase(code))
            .findFirst();
    }

    public boolean hasAttributeWithCode(String code) {
        return this.attributes.stream().anyMatch(a -> a.getCode().equalsIgnoreCase(code));
    }

    public CategoryId getId() {
        return id;
    }

    public CategoryName getName() {
        return name;
    }

    public CategorySlug getSlug() {
        return slug;
    }

    public String getPath() {
        return path;
    }

    public CategoryId getParentId() {
        return parentId;
    }

    public List<Attribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }
}
