package io.d4tzz.ecommerce.catalog.domain.category;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategoryTest {

    private static final CategorySlugChecker SLUG_FREE = slug -> false;
    private static final CategorySlugChecker SLUG_TAKEN = slug -> true;

    private static Attribute attribute(String code, AttributeType type, String... valueCodes) {
        List<AttributeValue> values = java.util.Arrays.stream(valueCodes)
            .map(c -> AttributeValue.of(c, c))
            .toList();
        return Attribute.create(code, code, type, values);
    }

    /** A persisted category. {@code path} is the ids of its ancestors, for example "1." */
    private static Category persisted(long id, String path, Long parentId, List<Attribute> attributes) {
        return new Category(
            CategoryId.of(id),
            CategoryName.of("Category " + id),
            CategorySlug.of("category-" + id),
            path,
            parentId == null ? null : CategoryId.of(parentId),
            attributes
        );
    }

    private static Category root(long id) {
        return persisted(id, null, null, List.of());
    }

    // ---- creation ----

    @Test
    void create_rootCategoryHasNoPathAndNoParent() {
        Category category = Category.create(
            CategoryName.of("Shirts"),
            CategorySlug.of("shirts"),
            SLUG_FREE,
            null,
            List.of(),
            List.of()
        );

        assertNull(category.getId());
        assertNull(category.getPath());
        assertNull(category.getParentId());
        assertEquals("shirts", category.getSlug().value());
    }

    @Test
    void create_childPathIsTheParentPrefix() {
        Category grandParent = root(1);
        Category parent = persisted(5, grandParent.prefix(), 1L, List.of());

        Category child = Category.create(
            CategoryName.of("T-shirts"),
            CategorySlug.of("t-shirts"),
            SLUG_FREE,
            parent,
            List.of(),
            List.of()
        );

        assertEquals("1.5.", child.getPath());
        assertEquals(CategoryId.of(5L), child.getParentId());
    }

    @Test
    void create_rejectsSlugAlreadyTaken() {
        assertThrows(ConflictException.class, () ->
            Category.create(CategoryName.of("Shirts"), CategorySlug.of("shirts"), SLUG_TAKEN, null, List.of(), List.of())
        );
    }

    @Test
    void create_rejectsAttributeAlreadyInheritedFromAnAncestor() {
        Attribute inherited = attribute("material", AttributeType.SINGLE, "cotton");

        assertThrows(ConflictException.class, () ->
            Category.create(
                CategoryName.of("Shirts"),
                CategorySlug.of("shirts"),
                SLUG_FREE,
                root(1),
                List.of(attribute("material", AttributeType.MULTI, "wool")),
                List.of(inherited)
            )
        );
    }

    @Test
    void create_rejectsDuplicateAttributeCodes() {
        assertThrows(ConflictException.class, () ->
            Category.create(
                CategoryName.of("Shirts"),
                CategorySlug.of("shirts"),
                SLUG_FREE,
                null,
                List.of(
                    attribute("material", AttributeType.SINGLE, "cotton"),
                    attribute("material", AttributeType.MULTI, "wool")
                ),
                List.of()
            )
        );
    }

    @Test
    void create_rejectsAParentThatIsNotPersisted() {
        Category unsavedParent = Category.create(
            CategoryName.of("Clothes"),
            CategorySlug.of("clothes"),
            SLUG_FREE,
            null,
            List.of(),
            List.of()
        );

        assertThrows(InvalidOperationException.class, () ->
            Category.create(
                CategoryName.of("Shirts"),
                CategorySlug.of("shirts"),
                SLUG_FREE,
                unsavedParent,
                List.of(),
                List.of()
            )
        );
    }

    // ---- prefix ----

    @Test
    void prefix_ofARootIsItsIdFollowedByTheDelimiter() {
        assertEquals("5.", root(5).prefix());
    }

    @Test
    void prefix_ofAChildIsItsPathPlusItsId() {
        assertEquals("1.5.", persisted(5, "1.", 1L, List.of()).prefix());
    }

    @Test
    void prefix_requiresAnId() {
        Category unsaved = new Category(
            null,
            CategoryName.of("Shirts"),
            CategorySlug.of("shirts"),
            null,
            null,
            List.of()
        );

        assertThrows(InvalidOperationException.class, unsaved::prefix);
    }

    // ---- rename / slug ----

    @Test
    void rename_changesTheName() {
        Category category = root(1);

        category.rename(CategoryName.of("Tops"));

        assertEquals("Tops", category.getName().value());
    }

    @Test
    void changeSlug_changesTheSlug() {
        Category category = root(1);

        category.changeSlug(CategorySlug.of("tops"), SLUG_FREE);

        assertEquals("tops", category.getSlug().value());
    }

    @Test
    void changeSlug_toTheSameSlugDoesNotCheckUniqueness() {
        Category category = root(1);

        category.changeSlug(CategorySlug.of("category-1"), SLUG_TAKEN);

        assertEquals("category-1", category.getSlug().value());
    }

    @Test
    void changeSlug_rejectsSlugUsedByAnotherCategory() {
        Category category = root(1);

        assertThrows(ConflictException.class, () -> category.changeSlug(CategorySlug.of("tops"), SLUG_TAKEN));
        assertEquals("category-1", category.getSlug().value());
    }

    // ---- updateAttributes ----

    @Test
    void updateAttributes_canAddANewAttribute() {
        Category category = persisted(1, null, null, List.of(attribute("material", AttributeType.SINGLE, "cotton")));

        category.updateAttributes(
            List.of(
                attribute("material", AttributeType.SINGLE, "cotton"),
                attribute("fit", AttributeType.SINGLE, "slim")
            ),
            List.of(),
            List.of()
        );

        assertTrue(category.hasAttributeWithCode("fit"));
        assertEquals(2, category.getAttributes().size());
    }

    @Test
    void updateAttributes_canAddANewValueToAnExistingAttribute() {
        Category category = persisted(1, null, null, List.of(attribute("material", AttributeType.SINGLE, "cotton")));

        category.updateAttributes(
            List.of(attribute("material", AttributeType.SINGLE, "cotton", "wool")),
            List.of(),
            List.of()
        );

        assertEquals(java.util.Set.of("cotton", "wool"), category.getAttribute("material").orElseThrow().getCodes());
    }

    @Test
    void updateAttributes_cannotRemoveAnAttribute() {
        Category category = persisted(
            1,
            null,
            null,
            List.of(
                attribute("material", AttributeType.SINGLE, "cotton"),
                attribute("fit", AttributeType.SINGLE, "slim")
            )
        );

        assertThrows(InvalidOperationException.class, () ->
            category.updateAttributes(
                List.of(attribute("material", AttributeType.SINGLE, "cotton")),
                List.of(),
                List.of()
            )
        );
        assertEquals(2, category.getAttributes().size());
    }

    @Test
    void updateAttributes_cannotChangeTheTypeOfAnAttribute() {
        Category category = persisted(1, null, null, List.of(attribute("material", AttributeType.SINGLE, "cotton")));

        assertThrows(InvalidOperationException.class, () ->
            category.updateAttributes(
                List.of(attribute("material", AttributeType.MULTI, "cotton")),
                List.of(),
                List.of()
            )
        );
    }

    @Test
    void updateAttributes_cannotRemoveAnExistingValue() {
        Category category = persisted(
            1,
            null,
            null,
            List.of(attribute("material", AttributeType.SINGLE, "cotton", "wool"))
        );

        assertThrows(InvalidOperationException.class, () ->
            category.updateAttributes(
                List.of(attribute("material", AttributeType.SINGLE, "cotton")),
                List.of(),
                List.of()
            )
        );
        assertEquals(java.util.Set.of("cotton", "wool"), category.getAttribute("material").orElseThrow().getCodes());
    }

    @Test
    void updateAttributes_rejectsCodeInheritedFromAnAncestor() {
        Category category = persisted(5, "1.", 1L, List.of());

        assertThrows(ConflictException.class, () ->
            category.updateAttributes(
                List.of(attribute("material", AttributeType.SINGLE, "cotton")),
                List.of(attribute("material", AttributeType.SINGLE, "wool")),
                List.of()
            )
        );
    }

    @Test
    void updateAttributes_rejectsCodeAlreadyUsedByADescendant() {
        Category category = root(1);

        assertThrows(ConflictException.class, () ->
            category.updateAttributes(
                List.of(attribute("material", AttributeType.SINGLE, "cotton")),
                List.of(),
                List.of(attribute("material", AttributeType.SINGLE, "wool"))
            )
        );
    }

    @Test
    void updateAttributes_rejectsDuplicateCodesInTheNewList() {
        Category category = root(1);

        assertThrows(ConflictException.class, () ->
            category.updateAttributes(
                List.of(
                    attribute("material", AttributeType.SINGLE, "cotton"),
                    attribute("material", AttributeType.SINGLE, "wool")
                ),
                List.of(),
                List.of()
            )
        );
    }

    // ---- attribute lookup ----

    @Test
    void getAttribute_ignoresCase() {
        Category category = persisted(1, null, null, List.of(attribute("material", AttributeType.SINGLE, "cotton")));

        assertTrue(category.getAttribute("MATERIAL").isPresent());
        assertTrue(category.hasAttributeWithCode("Material"));
        assertTrue(category.getAttribute("fit").isEmpty());
        assertFalse(category.hasAttributeWithCode("fit"));
    }

    @Test
    void getAttributes_isUnmodifiable() {
        Category category = persisted(1, null, null, List.of(attribute("material", AttributeType.SINGLE, "cotton")));

        assertThrows(UnsupportedOperationException.class, () -> category.getAttributes().clear());
    }

    // ---- move ----

    @Test
    void move_changesPathAndParent() {
        Category category = persisted(5, "1.", 1L, List.of());
        Category newParent = root(2);

        category.move(newParent);

        assertEquals("2.", category.getPath());
        assertEquals(CategoryId.of(2L), category.getParentId());
    }

    @Test
    void move_rejectsMovingIntoItself() {
        Category category = persisted(5, "1.", 1L, List.of());

        assertThrows(InvalidOperationException.class, () -> category.move(category));
    }

    @Test
    void move_rejectsMovingIntoItsOwnDescendant() {
        Category category = persisted(5, "1.", 1L, List.of());
        Category descendant = persisted(7, "1.5.", 5L, List.of());

        assertThrows(InvalidOperationException.class, () -> category.move(descendant));
        assertEquals("1.", category.getPath());
    }
}
