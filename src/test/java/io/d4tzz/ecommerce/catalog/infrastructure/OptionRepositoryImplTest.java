package io.d4tzz.ecommerce.catalog.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.category.Category;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryName;
import io.d4tzz.ecommerce.catalog.domain.category.CategorySlug;
import io.d4tzz.ecommerce.catalog.domain.option.Option;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.OptionName;
import io.d4tzz.ecommerce.catalog.domain.option.Value;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import io.d4tzz.ecommerce.catalog.domain.option.ValueName;
import io.d4tzz.ecommerce.catalog.domain.product.Description;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductName;
import io.d4tzz.ecommerce.catalog.domain.product.ProductSlug;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.repository.CategoryRepositoryImpl;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.repository.OptionRepositoryImpl;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.repository.ProductRepositoryImpl;
import io.d4tzz.ecommerce.support.PostgresContainerTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
// same @Import in every catalog repository test, so Spring builds one context and one connection pool for all of them
@Import({ ProductRepositoryImpl.class, OptionRepositoryImpl.class, CategoryRepositoryImpl.class })
class OptionRepositoryImplTest extends PostgresContainerTest {

    @Autowired OptionRepositoryImpl repository;
    @Autowired ProductRepositoryImpl productRepository;
    @Autowired CategoryRepositoryImpl categoryRepository;
    @Autowired EntityManager em;

    private Option save(String name, String... valueNames) {
        List<ValueName> values = java.util.Arrays.stream(valueNames).map(ValueName::of).toList();
        return repository.save(Option.create(OptionName.of(name), values, repository::existsByName));
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

    private static List<String> valueNames(Option option) {
        return option.getValues().stream().map(v -> v.getName().value()).toList();
    }

    /** Saves a product that uses {@code option} with the single value {@code valueName}. */
    private void saveProductUsing(Option option, String valueName) {
        Category category = categoryRepository.save(
            Category.create(CategoryName.of("Clothes"), CategorySlug.of("clothes"), slug -> false, null, List.of(), List.of())
        );
        Product product = Product.create(
            ProductName.of("Shirt"),
            ProductSlug.of("shirt"),
            productRepository::existsBySlug,
            Description.of(""),
            category.getId(),
            Map.of(),
            List.of()
        );
        product.addOption(option.getId(), valueId(option, valueName));
        productRepository.save(product);
    }

    /** Writes pending changes and forgets them, so the next read comes from the database. */
    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    // ---- save / find ----

    @Test
    void save_newOptionGetsIdsAndRoundTripsItsValues() {
        Option saved = save("Color", "Red", "Blue");
        flushAndClear();

        Option loaded = repository.find(saved.getId()).orElseThrow();

        assertNotNull(saved.getId());
        assertEquals("Color", loaded.getName().value());
        assertEquals(List.of("Red", "Blue"), valueNames(loaded));
        assertTrue(loaded.getValues().stream().allMatch(v -> v.getId() != null));
    }

    @Test
    void find_unknownIdIsEmpty() {
        assertTrue(repository.find(OptionId.of(-1L)).isEmpty());
    }

    @Test
    void save_existingOptionCanRenameAndAddAValue() {
        Option saved = save("Color", "Red");
        flushAndClear();

        Option loaded = repository.find(saved.getId()).orElseThrow();
        loaded.rename(OptionName.of("Colour"), repository::existsByName);
        loaded.addValue(ValueName.of("Blue"));
        repository.save(loaded);
        flushAndClear();

        Option reloaded = repository.find(saved.getId()).orElseThrow();
        assertEquals("Colour", reloaded.getName().value());
        assertEquals(List.of("Red", "Blue"), valueNames(reloaded));
    }

    @Test
    void save_existingOptionCanRenameAValue() {
        Option saved = save("Color", "Red", "Blue");
        flushAndClear();

        Option loaded = repository.find(saved.getId()).orElseThrow();
        loaded.renameValue(valueId(loaded, "Red"), ValueName.of("Crimson"));
        repository.save(loaded);
        flushAndClear();

        Option reloaded = repository.find(saved.getId()).orElseThrow();
        assertEquals(List.of("Crimson", "Blue"), valueNames(reloaded));
        assertEquals(valueId(saved, "Red"), valueId(reloaded, "Crimson"));
    }

    @Test
    void save_existingOptionCanRemoveAValue() {
        Option saved = save("Color", "Red", "Blue");
        flushAndClear();

        Option loaded = repository.find(saved.getId()).orElseThrow();
        loaded.removeValue(valueId(loaded, "Red"), id -> false);
        repository.save(loaded);
        flushAndClear();

        assertEquals(List.of("Blue"), valueNames(repository.find(saved.getId()).orElseThrow()));
    }

    @Test
    void save_duplicateNameViolatesUniqueConstraint() {
        save("Color");
        em.flush();

        assertThrows(ConstraintViolationException.class, () -> {
            repository.save(Option.create(OptionName.of("Color"), List.of(), name -> false));
            em.flush();
        });
    }

    @Test
    void delete_removesTheOptionAndItsValues() {
        Option saved = save("Color", "Red");
        flushAndClear();

        repository.delete(saved.getId());
        flushAndClear();

        assertTrue(repository.find(saved.getId()).isEmpty());
        assertFalse(repository.existsById(saved.getId()));
    }

    // ---- existence checks ----

    @Test
    void existsByName_andExistsById() {
        Option saved = save("Color");
        flushAndClear();

        assertTrue(repository.existsByName(OptionName.of("Color")));
        assertFalse(repository.existsByName(OptionName.of("Size")));
        assertTrue(repository.existsById(saved.getId()));
        assertFalse(repository.existsById(OptionId.of(-1L)));
    }

    @Test
    void areValuesBelongToOption_isTrueOnlyWhenEveryValueIsFromThatOption() {
        Option color = save("Color", "Red", "Blue");
        Option size = save("Size", "S");
        flushAndClear();

        ValueId red = valueId(color, "Red");
        ValueId blue = valueId(color, "Blue");
        ValueId s = valueId(size, "S");

        assertTrue(repository.areValuesBelongToOption(color.getId(), List.of(red, blue)));
        assertFalse(repository.areValuesBelongToOption(color.getId(), List.of(red, s)));
        assertFalse(repository.areValuesBelongToOption(color.getId(), List.of(ValueId.of(-1L))));
    }

    @Test
    void areValuesBelongToOption_countsDuplicateIdsOnceAndAcceptsAnEmptyList() {
        Option color = save("Color", "Red");
        flushAndClear();
        ValueId red = valueId(color, "Red");

        assertTrue(repository.areValuesBelongToOption(color.getId(), List.of(red, red)));
        assertTrue(repository.areValuesBelongToOption(color.getId(), List.of()));
    }

    // ---- usage checks ----

    @Test
    void isOptionReferenced_andIsValueReferenced_areTrueOnlyForWhatAProductUses() {
        Option color = save("Color", "Red", "Blue");
        saveProductUsing(color, "Red");
        flushAndClear();

        assertTrue(repository.isOptionReferenced(color.getId()));
        assertTrue(repository.isValueReferenced(valueId(color, "Red")));
        assertFalse(repository.isValueReferenced(valueId(color, "Blue")));
    }

    @Test
    void isOptionReferenced_isFalseForAnOptionNoProductUses() {
        Option size = save("Size", "S");
        flushAndClear();

        assertFalse(repository.isOptionReferenced(size.getId()));
        assertFalse(repository.isValueReferenced(valueId(size, "S")));
    }
}
