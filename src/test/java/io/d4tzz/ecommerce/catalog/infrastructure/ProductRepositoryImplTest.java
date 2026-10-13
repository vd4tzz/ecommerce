package io.d4tzz.ecommerce.catalog.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.category.Attribute;
import io.d4tzz.ecommerce.catalog.domain.category.AttributeType;
import io.d4tzz.ecommerce.catalog.domain.category.AttributeValue;
import io.d4tzz.ecommerce.catalog.domain.category.Category;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryId;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryName;
import io.d4tzz.ecommerce.catalog.domain.category.CategorySlug;
import io.d4tzz.ecommerce.catalog.domain.option.Option;
import io.d4tzz.ecommerce.catalog.domain.option.OptionName;
import io.d4tzz.ecommerce.catalog.domain.option.Value;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import io.d4tzz.ecommerce.catalog.domain.option.ValueName;
import io.d4tzz.ecommerce.catalog.domain.product.Description;
import io.d4tzz.ecommerce.catalog.domain.product.Dimensions;
import io.d4tzz.ecommerce.catalog.domain.product.MediaId;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductName;
import io.d4tzz.ecommerce.catalog.domain.product.ProductSlug;
import io.d4tzz.ecommerce.catalog.domain.product.ProductStatus;
import io.d4tzz.ecommerce.catalog.domain.product.Sku;
import io.d4tzz.ecommerce.catalog.domain.product.Variant;
import io.d4tzz.ecommerce.catalog.domain.product.Weight;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.repository.CategoryRepositoryImpl;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.repository.OptionRepositoryImpl;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.repository.ProductRepositoryImpl;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.support.PostgresContainerTest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
// same @Import in every catalog repository test, so Spring builds one context and one connection pool for all of them
@Import({ ProductRepositoryImpl.class, OptionRepositoryImpl.class, CategoryRepositoryImpl.class })
class ProductRepositoryImplTest extends PostgresContainerTest {

    private static final MediaId IMG_1 = MediaId.of("img-1");
    private static final MediaId IMG_2 = MediaId.of("img-2");

    @Autowired
    ProductRepositoryImpl repository;

    @Autowired
    OptionRepositoryImpl optionRepository;

    @Autowired
    CategoryRepositoryImpl categoryRepository;

    @Autowired
    EntityManager em;

    private Category category;
    private Option color;
    private Option size;
    private ValueId red;
    private ValueId blue;
    private ValueId s;
    private ValueId m;

    /** Products reference a category, options and values by foreign key, so those must exist first. */
    @BeforeEach
    void setUp() {
        Attribute material = Attribute.create(
            "material",
            "Material",
            AttributeType.SINGLE,
            List.of(AttributeValue.of("cotton", "Cotton"), AttributeValue.of("wool", "Wool"))
        );
        category = categoryRepository.save(
            Category.create(
                CategoryName.of("Shirts"),
                CategorySlug.of("shirts"),
                slug -> false,
                null,
                List.of(material),
                List.of()
            )
        );
        color = optionRepository.save(
            Option.create(OptionName.of("Color"), List.of(ValueName.of("Red"), ValueName.of("Blue")), name -> false)
        );
        size = optionRepository.save(
            Option.create(OptionName.of("Size"), List.of(ValueName.of("S"), ValueName.of("M")), name -> false)
        );
        red = valueId(color, "Red");
        blue = valueId(color, "Blue");
        s = valueId(size, "S");
        m = valueId(size, "M");
        em.flush();
        IO.println("=========================");
    }

    private static ValueId valueId(Option option, String name) {
        return option
            .getValues()
            .stream()
            .filter(v -> v.getName().value().equals(name))
            .map(Value::getId)
            .findFirst()
            .orElseThrow();
    }

    private Product newProduct(String slug) {
        return Product.create(
            ProductName.of("Shirt " + slug),
            ProductSlug.of(slug),
            repository::existsBySlug,
            Description.of("A shirt"),
            category.getId(),
            Map.of(),
            List.of()
        );
    }

    /** A draft with options Color [Red, Blue] and Size [S, M], media [img-1, img-2] and one variant Red / S. */
    private Product newShirt(String slug, String sku) {
        Product product = newProduct(slug);
        product.addMedia(IMG_1);
        product.addMedia(IMG_2);
        product.addOption(color.getId(), red);
        product.addValue(color.getId(), blue);
        product.addOption(size.getId(), s);
        product.addValue(size.getId(), m);
        addVariant(product, sku, 100_000, IMG_1, Map.of(color.getId(), red, size.getId(), s));
        return product;
    }

    private Variant addVariant(
        Product product,
        String sku,
        long price,
        MediaId media,
        Map<io.d4tzz.ecommerce.catalog.domain.option.OptionId, ValueId> combination
    ) {
        return product.addVariant(
            Sku.of(sku),
            Money.of(price),
            media,
            Dimensions.of(10, 20, 30),
            Weight.ofGrams(500),
            combination,
            repository::existsBySku
        );
    }

    private Product reload(ProductId id) {
        em.flush();
        em.clear();
        return repository.find(id).orElseThrow();
    }

    private Variant variantBySku(Product product, String sku) {
        return product
            .getVariants()
            .stream()
            .filter(v -> v.getSku().value().equals(sku))
            .findFirst()
            .orElseThrow();
    }

    // ---- save new / find ----

    @Test
    void save_newProductWithoutOptionsRoundTripsItsBasicFields() {
        Product product = Product.create(
            ProductName.of("Shirt"),
            ProductSlug.of("shirt"),
            repository::existsBySlug,
            Description.of("A shirt"),
            category.getId(),
            Map.of("material", List.of("cotton")),
            categoryRepository.findAttributes(category.getId())
        );

        Product loaded = reload(repository.save(product).getId());

        assertEquals("Shirt", loaded.getName().value());
        assertEquals("shirt", loaded.getSlug().value());
        assertEquals("A shirt", loaded.getDescription().value());
        assertEquals(category.getId(), loaded.getCategoryId());
        assertEquals(ProductStatus.DRAFT, loaded.getStatus());
        assertEquals(Map.of("material", List.of("cotton")), loaded.getAttributes());
        assertTrue(loaded.getOptions().isEmpty());
        assertTrue(loaded.getMedia().isEmpty());
        assertTrue(loaded.getVariants().isEmpty());
    }

    @Test
    void save_newProductRoundTripsOptionsMediaAndVariantsInOrder() {
        Product saved = repository.save(newShirt("shirt", "SHIRT-RED-S"));

        Product loaded = reload(saved.getId());

        assertNotNull(saved.getId());
        assertEquals(List.of(color.getId(), size.getId()), List.copyOf(loaded.getOptions().keySet()));
        assertEquals(List.of(red, blue), List.copyOf(loaded.getOptions().get(color.getId())));
        assertEquals(List.of(s, m), List.copyOf(loaded.getOptions().get(size.getId())));
        assertEquals(List.of(IMG_1, IMG_2), List.copyOf(loaded.getMedia()));

        assertEquals(1, loaded.getVariants().size());
        Variant variant = loaded.getVariants().get(0);
        assertNotNull(variant.getId());
        assertEquals(Sku.of("SHIRT-RED-S"), variant.getSku());
        assertEquals(Money.of(100_000), variant.getPrice());
        assertEquals(Money.of(100_000), variant.getCompareAt());
        assertEquals(IMG_1, variant.getMediaId());
        assertEquals(Dimensions.of(10, 20, 30), variant.getDimensions());
        assertEquals(Weight.ofGrams(500), variant.getWeight());
        assertEquals(Map.of(color.getId(), red, size.getId(), s), variant.getCombination());
    }

    @Test
    void save_keepsTheOrderOfOptionsValuesAndMediaThatWasSet() {
        Product product = newShirt("shirt", "SHIRT-RED-S");
        product.reorderOptions(List.of(size.getId(), color.getId()));
        product.reorderOptionValues(color.getId(), List.of(blue, red));
        product.reorderMedia(List.of(IMG_2, IMG_1));

        Product loaded = reload(repository.save(product).getId());

        assertEquals(List.of(size.getId(), color.getId()), List.copyOf(loaded.getOptions().keySet()));
        assertEquals(List.of(blue, red), List.copyOf(loaded.getOptions().get(color.getId())));
        assertEquals(List.of(IMG_2, IMG_1), List.copyOf(loaded.getMedia()));
    }

    @Test
    void find_unknownIdIsEmpty() {
        assertTrue(repository.find(ProductId.of(-1L)).isEmpty());
    }

    // ---- save existing ----

    @Test
    void save_existingProductCanChangeItsBasicFieldsAndStatus() {
        Product saved = repository.save(newProduct("shirt"));
        Product loaded = reload(saved.getId());

        loaded.rename(ProductName.of("Polo"));
        loaded.changeSlug(ProductSlug.of("polo"), repository::existsBySlug);
        loaded.changeDescription(Description.of("A polo"));
        loaded.updateAttributes(
            Map.of("material", List.of("wool")),
            categoryRepository.findAttributes(category.getId())
        );
        loaded.publish();
        repository.save(loaded);

        Product reloaded = reload(saved.getId());
        assertEquals("Polo", reloaded.getName().value());
        assertEquals("polo", reloaded.getSlug().value());
        assertEquals("A polo", reloaded.getDescription().value());
        assertEquals(Map.of("material", List.of("wool")), reloaded.getAttributes());
        assertEquals(ProductStatus.PUBLISHED, reloaded.getStatus());
    }

    @Test
    void save_existingProductCanAddAVariant() {
        Product saved = repository.save(newShirt("shirt", "SHIRT-RED-S"));
        Product loaded = reload(saved.getId());

        addVariant(loaded, "SHIRT-BLUE-M", 90_000, IMG_2, Map.of(color.getId(), blue, size.getId(), m));
        repository.save(loaded);

        Product reloaded = reload(saved.getId());
        assertEquals(2, reloaded.getVariants().size());
        Variant added = variantBySku(reloaded, "SHIRT-BLUE-M");
        assertEquals(Money.of(90_000), added.getPrice());
        assertEquals(Map.of(color.getId(), blue, size.getId(), m), added.getCombination());
    }

    @Test
    void save_existingProductCanChangeAVariant() {
        Product saved = repository.save(newShirt("shirt", "SHIRT-RED-S"));
        Product loaded = reload(saved.getId());
        Variant variant = variantBySku(loaded, "SHIRT-RED-S");

        loaded.updateVariantPrice(variant.getId(), Money.of(80_000), Money.of(120_000));
        loaded.updateVariantSku(variant.getId(), Sku.of("SHIRT-RED-S2"), repository::existsBySku);
        loaded.updateVariantWeight(variant.getId(), Weight.ofGrams(900));
        loaded.updateVariantDimensions(variant.getId(), Dimensions.of(1, 2, 3));
        loaded.updateVariantMedia(variant.getId(), null);
        repository.save(loaded);

        Variant changed = variantBySku(reload(saved.getId()), "SHIRT-RED-S2");
        assertEquals(variant.getId(), changed.getId());
        assertEquals(Money.of(80_000), changed.getPrice());
        assertEquals(Money.of(120_000), changed.getCompareAt());
        assertEquals(Weight.ofGrams(900), changed.getWeight());
        assertEquals(Dimensions.of(1, 2, 3), changed.getDimensions());
        assertNull(changed.getMediaId());
    }

    @Test
    void save_existingProductCanRemoveAVariant() {
        Product saved = repository.save(newShirt("shirt", "SHIRT-RED-S"));
        Product loaded = reload(saved.getId());

        loaded.removeVariant(variantBySku(loaded, "SHIRT-RED-S").getId());
        repository.save(loaded);

        assertTrue(reload(saved.getId()).getVariants().isEmpty());
        assertFalse(repository.existsBySku(Sku.of("SHIRT-RED-S")));
    }

    @Test
    void save_existingProductCanAddAnOptionAndUpdateEveryVariantCombination() {
        Option material = optionRepository.save(
            Option.create(OptionName.of("Material"), List.of(ValueName.of("Cotton")), name -> false)
        );
        ValueId cotton = valueId(material, "Cotton");
        Product saved = repository.save(newShirt("shirt", "SHIRT-RED-S"));
        Product loaded = reload(saved.getId());

        loaded.addOption(material.getId(), cotton);
        repository.save(loaded);

        Product reloaded = reload(saved.getId());
        assertEquals(
            List.of(color.getId(), size.getId(), material.getId()),
            List.copyOf(reloaded.getOptions().keySet())
        );
        assertEquals(
            Map.of(color.getId(), red, size.getId(), s, material.getId(), cotton),
            reloaded.getVariants().get(0).getCombination()
        );
    }

    @Test
    void save_existingProductCanRemoveAValueAndAnOption() {
        Product saved = repository.save(newShirt("shirt", "SHIRT-RED-S"));
        Product loaded = reload(saved.getId());

        loaded.removeValue(color.getId(), blue);
        loaded.removeValue(size.getId(), m);
        loaded.removeOption(size.getId());
        repository.save(loaded);

        Product reloaded = reload(saved.getId());
        assertEquals(List.of(red), List.copyOf(reloaded.getOptions().get(color.getId())));
        assertFalse(reloaded.getOptions().containsKey(size.getId()));
        assertEquals(Map.of(color.getId(), red), reloaded.getVariants().get(0).getCombination());
    }

    @Test
    void save_existingProductCanAddAndRemoveMedia() {
        Product saved = repository.save(newShirt("shirt", "SHIRT-RED-S"));
        Product loaded = reload(saved.getId());

        loaded.removeMedia(IMG_2);
        loaded.addMedia(MediaId.of("img-3"));
        repository.save(loaded);

        assertEquals(List.of(IMG_1, MediaId.of("img-3")), List.copyOf(reload(saved.getId()).getMedia()));
    }

    @Test
    void save_existingProductCanReorderWhatWasAlreadyStored() {
        Product saved = repository.save(newShirt("shirt", "SHIRT-RED-S"));
        Product loaded = reload(saved.getId());

        loaded.reorderOptions(List.of(size.getId(), color.getId()));
        loaded.reorderOptionValues(size.getId(), List.of(m, s));
        loaded.reorderMedia(List.of(IMG_2, IMG_1));
        repository.save(loaded);

        Product reloaded = reload(saved.getId());
        assertEquals(List.of(size.getId(), color.getId()), List.copyOf(reloaded.getOptions().keySet()));
        assertEquals(List.of(m, s), List.copyOf(reloaded.getOptions().get(size.getId())));
        assertEquals(List.of(IMG_2, IMG_1), List.copyOf(reloaded.getMedia()));
    }

    @Test
    void save_storesTheMinAndMaxPriceOfTheVariants() {
        Product product = newShirt("shirt", "SHIRT-RED-S");
        addVariant(product, "SHIRT-BLUE-S", 60_000, null, Map.of(color.getId(), blue, size.getId(), s));
        ProductId id = repository.save(product).getId();
        em.flush();

        BigDecimal min = em
            .createQuery("select p.minPrice from Product p where p.id = :id", BigDecimal.class)
            .setParameter("id", id.value())
            .getSingleResult();
        BigDecimal max = em
            .createQuery("select p.maxPrice from Product p where p.id = :id", BigDecimal.class)
            .setParameter("id", id.value())
            .getSingleResult();

        assertEquals(0, min.compareTo(BigDecimal.valueOf(60_000)));
        assertEquals(0, max.compareTo(BigDecimal.valueOf(100_000)));
    }

    // ---- delete / existence / constraints ----
    // save() flushes inside a Spring repository, so database errors come out as Spring's DataIntegrityViolationException

    @Test
    void delete_removesTheProductAndItsVariants() {
        Product saved = repository.save(newShirt("shirt", "SHIRT-RED-S"));
        em.flush();
        em.clear();

        repository.delete(saved.getId());
        em.flush();
        em.clear();

        assertTrue(repository.find(saved.getId()).isEmpty());
        assertFalse(repository.existsBySku(Sku.of("SHIRT-RED-S")));
    }

    @Test
    void existsBySlug_andExistsBySku() {
        repository.save(newShirt("shirt", "SHIRT-RED-S"));
        em.flush();
        em.clear();

        assertTrue(repository.existsBySlug(ProductSlug.of("shirt")));
        assertFalse(repository.existsBySlug(ProductSlug.of("hat")));
        assertTrue(repository.existsBySku(Sku.of("SHIRT-RED-S")));
        assertFalse(repository.existsBySku(Sku.of("HAT-1")));
    }

    @Test
    void save_duplicateSlugViolatesUniqueConstraint() {
        repository.save(newProduct("shirt"));
        em.flush();

        assertThrows(DataIntegrityViolationException.class, () -> {
            repository.save(
                Product.create(
                    ProductName.of("Other"),
                    ProductSlug.of("shirt"),
                    slug -> false, // skip the domain check to reach the database constraint
                    Description.of(""),
                    category.getId(),
                    Map.of(),
                    List.of()
                )
            );
            em.flush();
        });
    }

    @Test
    void save_duplicateSkuViolatesUniqueConstraint() {
        repository.save(newShirt("shirt", "SHIRT-RED-S"));
        em.flush();

        assertThrows(DataIntegrityViolationException.class, () -> {
            Product other = newProduct("polo");
            other.addVariant(
                Sku.of("SHIRT-RED-S"),
                Money.of(1),
                null,
                Dimensions.of(1, 1, 1),
                Weight.ofGrams(1),
                null,
                sku -> false // skip the domain check to reach the database constraint
            );
            repository.save(other);
            em.flush();
        });
    }

    @Test
    void save_unknownCategoryViolatesForeignKey() {
        Product product = Product.create(
            ProductName.of("Shirt"),
            ProductSlug.of("shirt"),
            repository::existsBySlug,
            Description.of(""),
            CategoryId.of(-1L),
            Map.of(),
            List.of()
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            repository.save(product);
            em.flush();
        });
    }
}
