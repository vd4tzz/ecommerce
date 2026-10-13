package io.d4tzz.ecommerce.catalog.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.category.Attribute;
import io.d4tzz.ecommerce.catalog.domain.category.AttributeType;
import io.d4tzz.ecommerce.catalog.domain.category.AttributeValue;
import io.d4tzz.ecommerce.catalog.domain.category.Category;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryId;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryName;
import io.d4tzz.ecommerce.catalog.domain.category.CategorySlug;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.repository.CategoryRepositoryImpl;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.repository.OptionRepositoryImpl;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.repository.ProductRepositoryImpl;
import io.d4tzz.ecommerce.support.PostgresContainerTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
// same @Import in every catalog repository test, so Spring builds one context and one connection pool for all of them
@Import({ ProductRepositoryImpl.class, OptionRepositoryImpl.class, CategoryRepositoryImpl.class })
class CategoryRepositoryImplTest extends PostgresContainerTest {

    @Autowired CategoryRepositoryImpl repository;
    @Autowired EntityManager em;

    private static Attribute attribute(String code, AttributeType type, String... valueCodes) {
        List<AttributeValue> values = java.util.Arrays.stream(valueCodes)
            .map(c -> AttributeValue.of(c, c.toUpperCase()))
            .toList();
        return Attribute.create(code, code.toUpperCase(), type, values);
    }

    /** Saves a new category under {@code parent} (null for a root) and returns it with its generated id. */
    private Category save(String slug, Category parent, Attribute... attributes) {
        Category category = Category.create(
            CategoryName.of("Name of " + slug),
            CategorySlug.of(slug),
            repository::existsBySlug,
            parent,
            List.of(attributes),
            parent == null ? List.of() : repository.findAttributes(parent.getId())
        );
        return repository.save(category);
    }

    /** Writes pending changes and forgets them, so the next read comes from the database. */
    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private static Set<String> codes(List<Attribute> attributes) {
        return attributes.stream().map(Attribute::getCode).collect(Collectors.toSet());
    }

    // ---- save / find ----

    @Test
    void save_newRootCategoryGetsIdAndRoundTripsEveryField() {
        Category saved = save("shirts", null, attribute("material", AttributeType.MULTI, "cotton", "wool"));
        flushAndClear();

        Category loaded = repository.find(saved.getId()).orElseThrow();

        assertNotNull(saved.getId());
        assertEquals("Name of shirts", loaded.getName().value());
        assertEquals("shirts", loaded.getSlug().value());
        assertNull(loaded.getPath());
        assertNull(loaded.getParentId());
        Attribute material = loaded.getAttribute("material").orElseThrow();
        assertEquals("MATERIAL", material.getLabel());
        assertEquals(AttributeType.MULTI, material.getType());
        assertEquals(Set.of("cotton", "wool"), material.getCodes());
    }

    @Test
    void save_childCategoryKeepsItsParentAndPath() {
        Category parent = save("clothes", null);
        Category child = save("shirts", parent);
        flushAndClear();

        Category loaded = repository.find(child.getId()).orElseThrow();

        assertEquals(parent.getId(), loaded.getParentId());
        assertEquals(parent.prefix(), loaded.getPath());
    }

    @Test
    void save_existingCategoryUpdatesItsFields() {
        Category saved = save("shirts", null, attribute("material", AttributeType.SINGLE, "cotton"));
        flushAndClear();

        Category loaded = repository.find(saved.getId()).orElseThrow();
        loaded.rename(CategoryName.of("Tops"));
        loaded.changeSlug(CategorySlug.of("tops"), repository::existsBySlug);
        loaded.updateAttributes(
            List.of(attribute("material", AttributeType.SINGLE, "cotton", "wool")),
            List.of(),
            List.of()
        );
        repository.save(loaded);
        flushAndClear();

        Category reloaded = repository.find(saved.getId()).orElseThrow();
        assertEquals("Tops", reloaded.getName().value());
        assertEquals("tops", reloaded.getSlug().value());
        assertEquals(Set.of("cotton", "wool"), reloaded.getAttribute("material").orElseThrow().getCodes());
    }

    @Test
    void find_unknownIdIsEmpty() {
        assertTrue(repository.find(CategoryId.of(-1L)).isEmpty());
    }

    @Test
    void save_duplicateSlugViolatesUniqueConstraint() {
        save("shirts", null);
        em.flush();

        assertThrows(ConstraintViolationException.class, () -> {
            repository.save(
                Category.create(
                    CategoryName.of("Other"),
                    CategorySlug.of("shirts"),
                    slug -> false, // skip the domain check to reach the database constraint
                    null,
                    List.of(),
                    List.of()
                )
            );
            em.flush();
        });
    }

    @Test
    void delete_removesTheCategory() {
        Category saved = save("shirts", null);
        flushAndClear();

        repository.delete(saved.getId());
        flushAndClear();

        assertTrue(repository.find(saved.getId()).isEmpty());
        assertFalse(repository.exists(saved.getId()));
    }

    // ---- existence checks ----

    @Test
    void exists_andExistsBySlug() {
        Category saved = save("shirts", null);
        flushAndClear();

        assertTrue(repository.exists(saved.getId()));
        assertFalse(repository.exists(CategoryId.of(-1L)));
        assertTrue(repository.existsBySlug(CategorySlug.of("shirts")));
        assertFalse(repository.existsBySlug(CategorySlug.of("hats")));
    }

    @Test
    void hasChildren_isTrueOnlyForACategoryWithADirectChild() {
        Category parent = save("clothes", null);
        Category child = save("shirts", parent);
        flushAndClear();

        assertTrue(repository.hasChildren(parent.getId()));
        assertFalse(repository.hasChildren(child.getId()));
    }

    @Test
    void parentHasChildByName_looksOnlyAtDirectChildrenOfThatParent() {
        Category parent = save("clothes", null);
        save("shirts", parent);
        flushAndClear();

        assertTrue(repository.parentHasChildByName(parent.getId(), CategoryName.of("Name of shirts")));
        assertFalse(repository.parentHasChildByName(parent.getId(), CategoryName.of("Name of hats")));
        // the root level has "clothes", not "shirts"
        assertTrue(repository.parentHasChildByName(null, CategoryName.of("Name of clothes")));
        assertFalse(repository.parentHasChildByName(null, CategoryName.of("Name of shirts")));
    }

    // ---- attribute schema ----

    @Test
    void findAttributes_returnsAncestorsThenOwnAttributes() {
        Category root = save("clothes", null, attribute("brand", AttributeType.SINGLE, "acme"));
        Category middle = save("tops", root, attribute("fit", AttributeType.SINGLE, "slim"));
        Category leaf = save("shirts", middle, attribute("material", AttributeType.SINGLE, "cotton"));
        flushAndClear();

        assertEquals(Set.of("brand", "fit", "material"), codes(repository.findAttributes(leaf.getId())));
        assertEquals(Set.of("brand"), codes(repository.findAttributes(root.getId())));
    }

    @Test
    void findAttributes_unknownCategoryIsEmpty() {
        assertTrue(repository.findAttributes(CategoryId.of(-1L)).isEmpty());
    }

    @Test
    void findDescendantAttributes_returnsEveryDescendantAtAnyDepthButNotItself() {
        Category root = save("clothes", null, attribute("brand", AttributeType.SINGLE, "acme"));
        Category middle = save("tops", root, attribute("fit", AttributeType.SINGLE, "slim"));
        save("shirts", middle, attribute("material", AttributeType.SINGLE, "cotton"));
        save("shoes", null, attribute("size_eu", AttributeType.SINGLE, "42"));
        flushAndClear();

        assertEquals(Set.of("fit", "material"), codes(repository.findDescendantAttributes(root.getId())));
        assertEquals(Set.of("material"), codes(repository.findDescendantAttributes(middle.getId())));
    }

    @Test
    void findDescendantAttributes_isEmptyForALeafAndForAnUnknownCategory() {
        Category root = save("clothes", null);
        Category leaf = save("shirts", root, attribute("material", AttributeType.SINGLE, "cotton"));
        flushAndClear();

        assertTrue(repository.findDescendantAttributes(leaf.getId()).isEmpty());
        assertTrue(repository.findDescendantAttributes(CategoryId.of(-1L)).isEmpty());
    }

    // ---- paths ----

    @Test
    void updateChildrenPrefix_rewritesThePathOfEveryDescendant() {
        Category oldParent = save("clothes", null);
        Category newParent = save("fashion", null);
        Category middle = save("tops", oldParent);
        Category leaf = save("shirts", middle);
        flushAndClear();

        repository.updateChildrenPrefix(oldParent.prefix(), newParent.prefix());
        flushAndClear();

        assertEquals(newParent.prefix() + middle.getId() + ".", repository.find(leaf.getId()).orElseThrow().getPath());
        assertEquals(newParent.prefix(), repository.find(middle.getId()).orElseThrow().getPath());
    }
}
