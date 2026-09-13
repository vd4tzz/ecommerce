package io.d4tzz.ecommerce.domain.catalog.category;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
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

public class Category {

    public static String PATH_DELIMITER = ".";

    private CategoryId id;
    private CategoryName name;
    private CategorySlug slug;
    private String path;
    private CategoryId parentId;
    private List<Attribute> attributes;

    private Category(
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

    public static Category restore(
        CategoryId id,
        CategoryName name,
        CategorySlug slug,
        String path,
        CategoryId parentId,
        List<Attribute> attributes
    ) {
        return new Category(id, name, slug, path, parentId, attributes);
    }

    public String prefix() {
        if (id == null) {
            throw new InvalidOperationException("Cannot generate prefix for category without ID");
        }
        return (path == null ? "" : path) + id.toString() + PATH_DELIMITER;
    }

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

    public void rename(CategoryName name) {
        this.name = name;
    }

    public void changeSlug(CategorySlug newSlug, CategorySlugChecker checker) {
        if (this.slug.equals(newSlug)) {
            return;
        }

        if (checker.exists(newSlug)) {
            throw new ConflictException("Category slug '" + newSlug.value() + "' already exists");
        }

        this.slug = newSlug;
    }

    public void updateAttributes(
        List<Attribute> newAttributes,
        List<Attribute> ancestorSchema,
        List<Attribute> descendantSchema
    ) {
        List<Attribute> validated = validateAttributes(newAttributes, ancestorSchema, descendantSchema);

        Map<String, Attribute> existingByCode = new HashMap<>(this.attributes.size());
        for (Attribute attr : this.attributes) {
            existingByCode.put(attr.getCode(), attr);
        }

        for (Attribute attr : validated) {
            Attribute existing = existingByCode.remove(attr.getCode());
            if (existing != null) {
                if (existing.getType() != attr.getType()) {
                    throw new InvalidOperationException("Cannot change type of existing attribute: " + attr.getCode());
                }

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

        Set<String> seenCodes = new HashSet<>(attributes.size());
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

    public void reorderAttributes(List<String> orderedCodes) {
        if (orderedCodes.size() != this.attributes.size()) {
            throw new InvalidArgumentException("Order list must match current attributes size");
        }

        Map<String, Attribute> attributeByCode = HashMap.newHashMap(this.attributes.size());
        for (Attribute attr : this.attributes) {
            attributeByCode.put(attr.getCode(), attr);
        }

        List<Attribute> reordered = new ArrayList<>(this.attributes.size());
        for (String code : orderedCodes) {
            Attribute attr = attributeByCode.remove(code.toLowerCase());
            if (attr == null) {
                throw new InvalidArgumentException(
                    "Attribute code '" + code + "' not found or duplicate in reorder list"
                );
            }

            reordered.add(attr);
        }

        this.attributes = reordered;
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
