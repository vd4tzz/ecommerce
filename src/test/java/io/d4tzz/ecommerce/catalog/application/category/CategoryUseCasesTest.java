package io.d4tzz.ecommerce.catalog.application.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.catalog.application.category.command.CreateCategoryCommand;
import io.d4tzz.ecommerce.catalog.application.category.command.UpdateCategoryAttributesCommand;
import io.d4tzz.ecommerce.catalog.application.category.command.UpdateCategoryCommand;
import io.d4tzz.ecommerce.catalog.domain.category.Attribute;
import io.d4tzz.ecommerce.catalog.domain.category.AttributeType;
import io.d4tzz.ecommerce.catalog.domain.category.AttributeValue;
import io.d4tzz.ecommerce.catalog.domain.category.Category;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryId;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryName;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryRepository;
import io.d4tzz.ecommerce.catalog.domain.category.CategorySlug;
import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CategoryUseCasesTest {

    private static final long ID = 5L;

    private CategoryRepository repository;
    private CreateCategoryUseCase create;
    private UpdateCategoryUseCase update;
    private UpdateCategoryAttributesUseCase updateAttributes;
    private DeleteCategoryUseCase delete;

    @BeforeEach
    void setUp() {
        repository = mock(CategoryRepository.class);
        create = new CreateCategoryUseCase(repository);
        update = new UpdateCategoryUseCase(repository);
        updateAttributes = new UpdateCategoryAttributesUseCase(repository);
        delete = new DeleteCategoryUseCase(repository);
    }

    private static Attribute attribute(String code, AttributeType type, String... valueCodes) {
        List<AttributeValue> values = java.util.Arrays.stream(valueCodes).map(c -> AttributeValue.of(c, c)).toList();
        return Attribute.create(code, code, type, values);
    }

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

    private static CreateCategoryCommand.Attribute materialCommand(String... valueCodes) {
        List<CreateCategoryCommand.Attribute.Value> values = java.util.Arrays.stream(valueCodes)
            .map(c -> new CreateCategoryCommand.Attribute.Value(c, c.toUpperCase()))
            .toList();
        return new CreateCategoryCommand.Attribute("material", "Material", AttributeType.MULTI, values);
    }

    private static UpdateCategoryAttributesCommand.Attribute updateCommand(String code, AttributeType type, String... valueCodes) {
        List<UpdateCategoryAttributesCommand.Attribute.Value> values = java.util.Arrays.stream(valueCodes)
            .map(c -> new UpdateCategoryAttributesCommand.Attribute.Value(c, c))
            .toList();
        return new UpdateCategoryAttributesCommand.Attribute(code, code, type, values);
    }

    private void saveReturnsTheCategoryWithId() {
        when(repository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            return new Category(CategoryId.of(ID), c.getName(), c.getSlug(), c.getPath(), c.getParentId(), c.getAttributes());
        });
    }

    private Category savedCategory() {
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    // ---- create ----

    @Test
    void create_rootCategoryIsSavedAndItsIdReturned() {
        saveReturnsTheCategoryWithId();

        Long id = create.execute(new CreateCategoryCommand("Shirts", "shirts", null, List.of(materialCommand("cotton", "wool"))));

        assertEquals(ID, id);
        Category saved = savedCategory();
        assertEquals("Shirts", saved.getName().value());
        assertEquals("shirts", saved.getSlug().value());
        assertNull(saved.getParentId());
        Attribute material = saved.getAttribute("material").orElseThrow();
        assertEquals(AttributeType.MULTI, material.getType());
        assertEquals(Set.of("cotton", "wool"), material.getCodes());
        verify(repository, never()).find(any());
        verify(repository, never()).findAttributes(any());
    }

    @Test
    void create_childUsesTheParentForItsPathAndInheritedSchema() {
        Category parent = persisted(1, null, null, List.of());
        when(repository.find(CategoryId.of(1L))).thenReturn(Optional.of(parent));
        when(repository.findAttributes(CategoryId.of(1L))).thenReturn(List.of());
        saveReturnsTheCategoryWithId();

        create.execute(new CreateCategoryCommand("Shirts", "shirts", 1L, List.of()));

        Category saved = savedCategory();
        assertEquals(CategoryId.of(1L), saved.getParentId());
        assertEquals("1.", saved.getPath());
    }

    @Test
    void create_rejectsUnknownParent() {
        when(repository.find(CategoryId.of(1L))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            create.execute(new CreateCategoryCommand("Shirts", "shirts", 1L, List.of()))
        );
        verify(repository, never()).save(any());
    }

    @Test
    void create_rejectsSlugAlreadyTaken() {
        when(repository.existsBySlug(CategorySlug.of("shirts"))).thenReturn(true);

        assertThrows(ConflictException.class, () ->
            create.execute(new CreateCategoryCommand("Shirts", "shirts", null, List.of()))
        );
        verify(repository, never()).save(any());
    }

    @Test
    void create_rejectsAttributeAlreadyInheritedFromTheParent() {
        Category parent = persisted(1, null, null, List.of(attribute("material", AttributeType.SINGLE, "cotton")));
        when(repository.find(CategoryId.of(1L))).thenReturn(Optional.of(parent));
        when(repository.findAttributes(CategoryId.of(1L))).thenReturn(parent.getAttributes());

        assertThrows(ConflictException.class, () ->
            create.execute(new CreateCategoryCommand("Shirts", "shirts", 1L, List.of(materialCommand("wool"))))
        );
        verify(repository, never()).save(any());
    }

    // ---- update ----

    @Test
    void update_renamesAndChangesTheSlug() {
        Category category = persisted(ID, null, null, List.of());
        when(repository.find(CategoryId.of(ID))).thenReturn(Optional.of(category));

        update.execute(new UpdateCategoryCommand(ID, "Tops", "tops"));

        assertEquals("Tops", category.getName().value());
        assertEquals("tops", category.getSlug().value());
        verify(repository).save(category);
    }

    @Test
    void update_rejectsUnknownCategory() {
        when(repository.find(CategoryId.of(ID))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> update.execute(new UpdateCategoryCommand(ID, "Tops", "tops")));
        verify(repository, never()).save(any());
    }

    @Test
    void update_rejectsSlugUsedByAnotherCategory() {
        Category category = persisted(ID, null, null, List.of());
        when(repository.find(CategoryId.of(ID))).thenReturn(Optional.of(category));
        when(repository.existsBySlug(CategorySlug.of("tops"))).thenReturn(true);

        assertThrows(ConflictException.class, () -> update.execute(new UpdateCategoryCommand(ID, "Tops", "tops")));
        verify(repository, never()).save(any());
    }

    // ---- update attributes ----

    @Test
    void updateAttributes_rootCategoryAddsAnAttributeWithoutLookingUpAncestors() {
        Category category = persisted(ID, null, null, List.of(attribute("material", AttributeType.SINGLE, "cotton")));
        when(repository.find(CategoryId.of(ID))).thenReturn(Optional.of(category));
        when(repository.findDescendantAttributes(CategoryId.of(ID))).thenReturn(List.of());

        updateAttributes.execute(
            new UpdateCategoryAttributesCommand(
                ID,
                List.of(updateCommand("material", AttributeType.SINGLE, "cotton"), updateCommand("fit", AttributeType.SINGLE, "slim"))
            )
        );

        assertTrue(category.hasAttributeWithCode("fit"));
        verify(repository).save(category);
        verify(repository, never()).findAttributes(any());
    }

    @Test
    void updateAttributes_childChecksTheParentSchemaAndRejectsAConflict() {
        Category category = persisted(ID, "1.", 1L, List.of());
        when(repository.find(CategoryId.of(ID))).thenReturn(Optional.of(category));
        when(repository.findAttributes(CategoryId.of(1L))).thenReturn(List.of(attribute("material", AttributeType.SINGLE, "cotton")));
        when(repository.findDescendantAttributes(CategoryId.of(ID))).thenReturn(List.of());

        assertThrows(ConflictException.class, () ->
            updateAttributes.execute(
                new UpdateCategoryAttributesCommand(ID, List.of(updateCommand("material", AttributeType.SINGLE, "wool")))
            )
        );
        verify(repository, never()).save(any());
    }

    @Test
    void updateAttributes_rejectsCodeAlreadyUsedByADescendant() {
        Category category = persisted(ID, null, null, List.of());
        when(repository.find(CategoryId.of(ID))).thenReturn(Optional.of(category));
        when(repository.findDescendantAttributes(CategoryId.of(ID))).thenReturn(List.of(attribute("fit", AttributeType.SINGLE, "slim")));

        assertThrows(ConflictException.class, () ->
            updateAttributes.execute(
                new UpdateCategoryAttributesCommand(ID, List.of(updateCommand("fit", AttributeType.SINGLE, "regular")))
            )
        );
        verify(repository, never()).save(any());
    }

    @Test
    void updateAttributes_cannotRemoveAnExistingAttribute() {
        Category category = persisted(ID, null, null, List.of(attribute("material", AttributeType.SINGLE, "cotton")));
        when(repository.find(CategoryId.of(ID))).thenReturn(Optional.of(category));
        when(repository.findDescendantAttributes(CategoryId.of(ID))).thenReturn(List.of());

        assertThrows(InvalidOperationException.class, () ->
            updateAttributes.execute(new UpdateCategoryAttributesCommand(ID, List.of()))
        );
        verify(repository, never()).save(any());
    }

    @Test
    void updateAttributes_rejectsUnknownCategory() {
        when(repository.find(CategoryId.of(ID))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            updateAttributes.execute(new UpdateCategoryAttributesCommand(ID, List.of()))
        );
    }

    // ---- delete ----

    @Test
    void delete_removesACategoryThatIsFree() {
        when(repository.exists(CategoryId.of(ID))).thenReturn(true);

        delete.execute(ID);

        verify(repository).delete(CategoryId.of(ID));
    }

    @Test
    void delete_rejectsUnknownCategory() {
        when(repository.exists(CategoryId.of(ID))).thenReturn(false);

        assertThrows(NotFoundException.class, () -> delete.execute(ID));
        verify(repository, never()).delete(any());
    }

    @Test
    void delete_rejectsACategoryWithChildrenWithoutCheckingProducts() {
        when(repository.exists(CategoryId.of(ID))).thenReturn(true);
        when(repository.hasChildren(CategoryId.of(ID))).thenReturn(true);

        assertThrows(InvalidOperationException.class, () -> delete.execute(ID));
        verify(repository, never()).isCategoryReferenced(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void delete_rejectsACategoryAssignedToProducts() {
        when(repository.exists(CategoryId.of(ID))).thenReturn(true);
        when(repository.isCategoryReferenced(CategoryId.of(ID))).thenReturn(true);

        assertThrows(InvalidOperationException.class, () -> delete.execute(ID));
        verify(repository, never()).delete(any());
    }
}
