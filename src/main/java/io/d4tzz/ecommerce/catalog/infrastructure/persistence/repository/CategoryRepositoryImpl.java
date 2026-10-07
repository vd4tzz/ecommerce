package io.d4tzz.ecommerce.catalog.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.catalog.domain.category.Attribute;
import io.d4tzz.ecommerce.catalog.domain.category.AttributeType;
import io.d4tzz.ecommerce.catalog.domain.category.AttributeValue;
import io.d4tzz.ecommerce.catalog.domain.category.Category;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryId;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryName;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryRepository;
import io.d4tzz.ecommerce.catalog.domain.category.CategorySlug;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.entity.JpaCategory;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.entity.JpaCategoryAttribute;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.entity.JpaCategoryAttribute.JpaAttributeValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
interface SpringCategoryRepository extends JpaRepository<JpaCategory, Long> {
    boolean existsBySlug(String slug);

    boolean existsByParentIdAndName(Long parentId, String name);

    boolean existsByParentId(Long parentId);

    @Query(
        """
        SELECT CASE WHEN EXISTS (
            SELECT 1 FROM Product p WHERE p.categoryId = :categoryId
        ) THEN true ELSE false END
        """
    )
    boolean isCategoryReferenced(@Param("categoryId") Long categoryId);

    @Modifying
    @Query(
        """
        UPDATE Category c
        SET c.path = REPLACE(c.path, :oldPrefix, :newPrefix)
        WHERE c.path LIKE CONCAT(:oldPrefix, '%')
        """
    )
    void updatePrefixPath(@Param("oldPrefix") String oldPrefix, @Param("newPrefix") String newPrefix);

    List<JpaCategory> findByPathStartingWith(String prefix);
}

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    private final SpringCategoryRepository springRepo;

    public CategoryRepositoryImpl(SpringCategoryRepository repo) {
        this.springRepo = repo;
    }

    @Override
    public boolean exists(CategoryId id) {
        if (id == null || id.value() == null) return false;
        return springRepo.existsById(id.value());
    }

    @Override
    public boolean existsBySlug(CategorySlug slug) {
        if (slug == null || slug.value() == null) return false;
        return springRepo.existsBySlug(slug.value());
    }

    @Override
    public Optional<Category> find(CategoryId id) {
        return springRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Attribute> findAttributes(CategoryId categoryId) {
        JpaCategory current = springRepo.findById(categoryId.value()).orElse(null);
        if (current == null) {
            return Collections.emptyList();
        }

        List<Long> ancestorIds = parseAncestorIds(current.getPath());
        List<Attribute> attributes = new ArrayList<>();

        if (!ancestorIds.isEmpty()) {
            for (JpaCategory a : springRepo.findAllById(ancestorIds)) {
                attributes.addAll(toDomain(a).getAttributes());
            }
        }
        attributes.addAll(toDomain(current).getAttributes());

        return Collections.unmodifiableList(attributes);
    }

    @Override
    public List<Attribute> findDescendantAttributes(CategoryId categoryId) {
        JpaCategory current = springRepo.findById(categoryId.value()).orElse(null);
        if (current == null) {
            return Collections.emptyList();
        }

        String prefix =
            (current.getPath() == null ? "" : current.getPath()) + current.getId() + Category.PATH_DELIMITER;

        List<JpaCategory> descendants = springRepo.findByPathStartingWith(prefix);
        if (descendants.isEmpty()) {
            return Collections.emptyList();
        }

        List<Attribute> attributes = new ArrayList<>();
        for (JpaCategory d : descendants) {
            attributes.addAll(toDomain(d).getAttributes());
        }
        return Collections.unmodifiableList(attributes);
    }

    private List<Long> parseAncestorIds(String path) {
        List<Long> ids = new ArrayList<>();
        if (path != null && !path.isBlank()) {
            for (String part : path.split("\\.")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        ids.add(Long.parseLong(trimmed));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return ids;
    }

    @Override
    @Transactional
    public Category save(Category category) {
        JpaCategory jpaCategory;
        Long parentId = null;

        if (category.getParentId() != null) {
            parentId = springRepo.findById(category.getParentId().value()).orElseThrow().getId();
        }

        if (category.getId() == null) {
            jpaCategory = new JpaCategory(
                null,
                category.getName().value(),
                category.getSlug().value(),
                category.getPath(),
                parentId
            );
        } else {
            jpaCategory = springRepo.findById(category.getId().value()).orElseThrow();

            jpaCategory.setName(category.getName().value());
            jpaCategory.setSlug(category.getSlug().value());
            jpaCategory.setPath(category.getPath());
            jpaCategory.setParentId(parentId);
        }

        jpaCategory.setAttributes(toJpaAttributes(category.getAttributes()));
        jpaCategory = springRepo.save(jpaCategory);

        return toDomain(jpaCategory);
    }

    private List<JpaCategoryAttribute> toJpaAttributes(List<Attribute> domainAttributes) {
        if (domainAttributes == null) return Collections.emptyList();
        return domainAttributes
            .stream()
            .map(a ->
                new JpaCategoryAttribute(
                    a.getCode(),
                    a.getLabel(),
                    a.getType().name(),
                    a
                        .getValues()
                        .stream()
                        .map(v -> new JpaAttributeValue(v.code(), v.label()))
                        .toList()
                )
            )
            .toList();
    }

    private Category toDomain(JpaCategory jpaCategory) {
        Long parentId = jpaCategory.getParentId();
        List<Attribute> definitions =
            jpaCategory.getAttributes() != null
                ? jpaCategory
                      .getAttributes()
                      .stream()
                      .map(d -> {
                          List<AttributeValue> values =
                              d.values() != null
                                  ? d
                                        .values()
                                        .stream()
                                        .map(o -> AttributeValue.of(o.code(), o.label()))
                                        .toList()
                                  : Collections.emptyList();

                          return Attribute.restore(d.code(), d.label(), AttributeType.valueOf(d.type()), values);
                      })
                      .toList()
                : Collections.emptyList();

        return new Category(
            CategoryId.of(jpaCategory.getId()),
            CategoryName.of(jpaCategory.getName()),
            CategorySlug.of(jpaCategory.getSlug()),
            jpaCategory.getPath(),
            parentId == null ? null : CategoryId.of(parentId),
            definitions
        );
    }

    @Override
    public void delete(CategoryId id) {
        springRepo.deleteById(id.value());
    }

    @Override
    public boolean parentHasChildByName(CategoryId parentId, CategoryName name) {
        return springRepo.existsByParentIdAndName(parentId == null ? null : parentId.value(), name.value());
    }

    @Override
    public boolean hasChildren(CategoryId id) {
        if (id == null || id.value() == null) return false;
        return springRepo.existsByParentId(id.value());
    }

    @Override
    public boolean isCategoryReferenced(CategoryId id) {
        if (id == null || id.value() == null) return false;
        return springRepo.isCategoryReferenced(id.value());
    }

    @Override
    public void updateChildrenPrefix(String oldPrefix, String newPrefix) {
        springRepo.updatePrefixPath(oldPrefix, newPrefix);
    }
}
