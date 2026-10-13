package io.d4tzz.ecommerce.catalog.application.product;

import static io.d4tzz.ecommerce.catalog.domain.product.ProductFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.catalog.application.product.command.ChangeProductStatusCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.CreateProductCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.UpdateProductCommand;
import io.d4tzz.ecommerce.catalog.domain.category.Attribute;
import io.d4tzz.ecommerce.catalog.domain.category.AttributeType;
import io.d4tzz.ecommerce.catalog.domain.category.AttributeValue;
import io.d4tzz.ecommerce.catalog.domain.category.Category;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryId;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryName;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryRepository;
import io.d4tzz.ecommerce.catalog.domain.category.CategorySlug;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import io.d4tzz.ecommerce.catalog.domain.product.ProductSlug;
import io.d4tzz.ecommerce.catalog.domain.product.ProductStatus;
import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProductLifecycleUseCasesTest {

    private static final long PRODUCT_ID = 1L;
    private static final long CATEGORY_ID = 1L;

    private static final Attribute MATERIAL = Attribute.create(
        "material",
        "Material",
        AttributeType.SINGLE,
        List.of(AttributeValue.of("cotton", "Cotton"), AttributeValue.of("wool", "Wool"))
    );

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private CreateProductUseCase create;
    private UpdateProductUseCase update;
    private DeleteProductUseCase delete;
    private ChangeProductStatusUseCase changeStatus;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        create = new CreateProductUseCase(productRepository, categoryRepository);
        update = new UpdateProductUseCase(productRepository, categoryRepository);
        delete = new DeleteProductUseCase(productRepository);
        changeStatus = new ChangeProductStatusUseCase(productRepository);
    }

    private Product stored(ProductStatus status) {
        Product product = shirt(status);
        when(productRepository.find(ProductId.of(PRODUCT_ID))).thenReturn(Optional.of(product));
        return product;
    }

    private void productDoesNotExist() {
        when(productRepository.find(ProductId.of(PRODUCT_ID))).thenReturn(Optional.empty());
    }

    private void categoryExists(long id, List<Attribute> schema) {
        Category category = new Category(
            CategoryId.of(id),
            CategoryName.of("Category " + id),
            CategorySlug.of("category-" + id),
            null,
            null,
            schema
        );
        when(categoryRepository.find(CategoryId.of(id))).thenReturn(Optional.of(category));
        when(categoryRepository.findAttributes(CategoryId.of(id))).thenReturn(schema);
    }

    // ---- create ----

    @Test
    void create_savesADraftInTheCategoryAndReturnsItsId() {
        categoryExists(CATEGORY_ID, List.of(MATERIAL));
        when(productRepository.save(any(Product.class))).thenReturn(shirt());

        Long id = create.execute(
            new CreateProductCommand("Shirt", "shirt", "A shirt", CATEGORY_ID, Map.of("material", List.of("cotton")))
        );

        assertEquals(PRODUCT_ID, id);
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();
        assertEquals("Shirt", saved.getName().value());
        assertEquals("shirt", saved.getSlug().value());
        assertEquals("A shirt", saved.getDescription().value());
        assertEquals(CategoryId.of(CATEGORY_ID), saved.getCategoryId());
        assertEquals(ProductStatus.DRAFT, saved.getStatus());
        assertEquals(Map.of("material", List.of("cotton")), saved.getAttributes());
    }

    @Test
    void create_rejectsUnknownCategory() {
        when(categoryRepository.find(CategoryId.of(CATEGORY_ID))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            create.execute(new CreateProductCommand("Shirt", "shirt", "", CATEGORY_ID, Map.of()))
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_rejectsSlugAlreadyTaken() {
        categoryExists(CATEGORY_ID, List.of());
        when(productRepository.existsBySlug(ProductSlug.of("shirt"))).thenReturn(true);

        assertThrows(ConflictException.class, () ->
            create.execute(new CreateProductCommand("Shirt", "shirt", "", CATEGORY_ID, Map.of()))
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_rejectsAttributesThatDoNotMatchTheCategorySchema() {
        categoryExists(CATEGORY_ID, List.of());

        assertThrows(InvalidArgumentException.class, () ->
            create.execute(
                new CreateProductCommand("Shirt", "shirt", "", CATEGORY_ID, Map.of("material", List.of("cotton")))
            )
        );
        verify(productRepository, never()).save(any());
    }

    // ---- update ----

    @Test
    void update_sameCategoryChangesTheBasicFieldsAndAttributes() {
        Product product = stored(ProductStatus.DRAFT);
        categoryExists(CATEGORY_ID, List.of(MATERIAL));

        update.execute(
            new UpdateProductCommand(PRODUCT_ID, "Polo", "polo", "A polo", CATEGORY_ID, Map.of("material", List.of("wool")))
        );

        assertEquals("Polo", product.getName().value());
        assertEquals("polo", product.getSlug().value());
        assertEquals("A polo", product.getDescription().value());
        assertEquals(CategoryId.of(CATEGORY_ID), product.getCategoryId());
        assertEquals(Map.of("material", List.of("wool")), product.getAttributes());
        verify(productRepository).save(product);
    }

    @Test
    void update_otherCategoryMovesTheProductAndReplacesItsAttributes() {
        Product product = stored(ProductStatus.DRAFT);
        categoryExists(2L, List.of(MATERIAL));

        update.execute(
            new UpdateProductCommand(PRODUCT_ID, "Shirt", "shirt", "A shirt", 2L, Map.of("material", List.of("cotton")))
        );

        assertEquals(CategoryId.of(2L), product.getCategoryId());
        assertEquals(Map.of("material", List.of("cotton")), product.getAttributes());
        verify(productRepository).save(product);
    }

    @Test
    void update_rejectsUnknownProductAndUnknownCategory() {
        productDoesNotExist();
        assertThrows(NotFoundException.class, () ->
            update.execute(new UpdateProductCommand(PRODUCT_ID, "Polo", "polo", "", CATEGORY_ID, Map.of()))
        );

        stored(ProductStatus.DRAFT);
        when(categoryRepository.find(CategoryId.of(2L))).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () ->
            update.execute(new UpdateProductCommand(PRODUCT_ID, "Polo", "polo", "", 2L, Map.of()))
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_rejectsSlugUsedByAnotherProduct() {
        stored(ProductStatus.DRAFT);
        categoryExists(CATEGORY_ID, List.of());
        when(productRepository.existsBySlug(ProductSlug.of("polo"))).thenReturn(true);

        assertThrows(ConflictException.class, () ->
            update.execute(new UpdateProductCommand(PRODUCT_ID, "Polo", "polo", "", CATEGORY_ID, Map.of()))
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_rejectsAttributesThatDoNotMatchTheSchema() {
        stored(ProductStatus.DRAFT);
        categoryExists(CATEGORY_ID, List.of(MATERIAL));

        assertThrows(InvalidArgumentException.class, () ->
            update.execute(
                new UpdateProductCommand(PRODUCT_ID, "Shirt", "shirt", "", CATEGORY_ID, Map.of("material", List.of("silk")))
            )
        );
        verify(productRepository, never()).save(any());
    }

    // ---- delete ----

    @Test
    void delete_removesADraft() {
        stored(ProductStatus.DRAFT);

        delete.execute(PRODUCT_ID);

        verify(productRepository).delete(ProductId.of(PRODUCT_ID));
    }

    @Test
    void delete_rejectsAProductThatIsNotADraft() {
        stored(ProductStatus.PUBLISHED);

        assertThrows(InvalidOperationException.class, () -> delete.execute(PRODUCT_ID));
        verify(productRepository, never()).delete(any());
    }

    @Test
    void delete_rejectsUnknownProduct() {
        productDoesNotExist();

        assertThrows(NotFoundException.class, () -> delete.execute(PRODUCT_ID));
    }

    // ---- change status ----

    @Test
    void changeStatus_publishesAndArchives() {
        Product product = stored(ProductStatus.DRAFT);

        changeStatus.execute(new ChangeProductStatusCommand(PRODUCT_ID, ProductStatus.PUBLISHED));
        assertEquals(ProductStatus.PUBLISHED, product.getStatus());

        changeStatus.execute(new ChangeProductStatusCommand(PRODUCT_ID, ProductStatus.ARCHIVED));
        assertEquals(ProductStatus.ARCHIVED, product.getStatus());

        verify(productRepository, times(2)).save(product);
    }

    @Test
    void changeStatus_cannotGoBackToDraft() {
        Product product = stored(ProductStatus.PUBLISHED);

        assertThrows(InvalidOperationException.class, () ->
            changeStatus.execute(new ChangeProductStatusCommand(PRODUCT_ID, ProductStatus.DRAFT))
        );
        assertEquals(ProductStatus.PUBLISHED, product.getStatus());
        verify(productRepository, never()).save(any());
    }

    @Test
    void changeStatus_rejectsUnknownProduct() {
        productDoesNotExist();

        assertThrows(NotFoundException.class, () ->
            changeStatus.execute(new ChangeProductStatusCommand(PRODUCT_ID, ProductStatus.PUBLISHED))
        );
    }
}
