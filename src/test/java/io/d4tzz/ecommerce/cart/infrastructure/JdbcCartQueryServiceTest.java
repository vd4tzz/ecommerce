package io.d4tzz.ecommerce.cart.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.cart.domain.Cart;
import io.d4tzz.ecommerce.cart.infrastructure.persistence.repository.CartRepositoryImpl;
import io.d4tzz.ecommerce.cart.infrastructure.query.JdbcCartQueryService;
import io.d4tzz.ecommerce.cart.query.CartItemView;
import io.d4tzz.ecommerce.cart.query.CartView;
import io.d4tzz.ecommerce.catalog.domain.category.Category;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryName;
import io.d4tzz.ecommerce.catalog.domain.category.CategorySlug;
import io.d4tzz.ecommerce.catalog.domain.product.Description;
import io.d4tzz.ecommerce.catalog.domain.product.Dimensions;
import io.d4tzz.ecommerce.catalog.domain.product.MediaId;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductName;
import io.d4tzz.ecommerce.catalog.domain.product.ProductSlug;
import io.d4tzz.ecommerce.catalog.domain.product.Sku;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.catalog.domain.product.Weight;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.repository.CategoryRepositoryImpl;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.repository.ProductRepositoryImpl;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.storage.MediaUrlResolver;
import io.d4tzz.ecommerce.storage.StorageProperties;
import io.d4tzz.ecommerce.support.PostgresContainerTest;
import io.d4tzz.ecommerce.user.domain.UserId;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(
    {
        CartRepositoryImpl.class,
        JdbcCartQueryService.class,
        ProductRepositoryImpl.class,
        CategoryRepositoryImpl.class,
        JdbcCartQueryServiceTest.StorageConfig.class,
    }
)
class JdbcCartQueryServiceTest extends PostgresContainerTest {

    private static final UserId USER = UserId.of(7L);

    @TestConfiguration
    static class StorageConfig {

        @Bean
        MediaUrlResolver mediaUrlResolver() {
            return new MediaUrlResolver(
                new StorageProperties("bucket", "region", "http://s3", "key", "secret", "http://cdn.test/shop", 15)
            );
        }
    }

    @Autowired CartRepositoryImpl cartRepository;
    @Autowired JdbcCartQueryService queryService;
    @Autowired ProductRepositoryImpl productRepository;
    @Autowired CategoryRepositoryImpl categoryRepository;
    @Autowired EntityManager em;

    private Category category;

    @BeforeEach
    void setUp() {
        category = categoryRepository.save(
            Category.create(CategoryName.of("Clothes"), CategorySlug.of("clothes"), slug -> false, null, List.of(), List.of())
        );
    }

    /**
     * Saves a product with a single variant and returns that variant's id.
     *
     * @param gallery      the product's images, in order
     * @param variantMedia the variant's own image, or {@code null}
     */
    private VariantId variant(String slug, String sku, long price, List<String> gallery, String variantMedia) {
        Product product = Product.create(
            ProductName.of("Product " + slug),
            ProductSlug.of(slug),
            s -> false,
            Description.of(""),
            category.getId(),
            Map.of(),
            List.of()
        );
        gallery.forEach(id -> product.addMedia(MediaId.of(id)));
        product.addVariant(
            Sku.of(sku),
            Money.of(price),
            variantMedia == null ? null : MediaId.of(variantMedia),
            Dimensions.of(10, 10, 10),
            Weight.ofGrams(500),
            null,
            s -> false
        );
        return productRepository.save(product).getVariants().get(0).getId();
    }

    private CartView cartWith(Object... variantsAndQuantities) {
        Cart cart = Cart.create(USER);
        for (int i = 0; i < variantsAndQuantities.length; i += 2) {
            cart.addItem((VariantId) variantsAndQuantities[i], (Integer) variantsAndQuantities[i + 1]);
        }
        cartRepository.save(cart);
        em.flush();
        em.clear();
        return queryService.getCart(USER.value());
    }

    // ---- empty carts ----

    @Test
    void getCart_forAVisitorIsEmptyAndHasNoUser() {
        CartView view = queryService.getCart(null);

        assertNull(view.id());
        assertNull(view.userId());
        assertTrue(view.items().isEmpty());
        assertEquals(0, view.totalQuantity());
        assertEquals(0, BigDecimal.ZERO.compareTo(view.subtotal()));
    }

    @Test
    void getCart_forAUserWithoutItemsIsEmptyButKeepsTheUser() {
        CartView view = queryService.getCart(USER.value());

        assertNull(view.id());
        assertEquals(USER.value(), view.userId());
        assertTrue(view.items().isEmpty());
    }

    @Test
    void getCart_forACartWithoutItemsIsEmpty() {
        cartRepository.save(Cart.create(USER));
        em.flush();
        em.clear();

        CartView view = queryService.getCart(USER.value());

        assertTrue(view.items().isEmpty());
        assertEquals(0, view.totalQuantity());
    }

    // ---- items and totals ----

    @Test
    void getCart_showsProductDetailsAndPricesOfEachItem() {
        VariantId shirt = variant("shirt", "SHIRT-1", 100_000, List.of(), null);

        CartView view = cartWith(shirt, 2);

        assertEquals(1, view.items().size());
        CartItemView item = view.items().get(0);
        assertNotNull(item.id());
        assertEquals(shirt.value(), item.variantId());
        assertEquals("SHIRT-1", item.sku());
        assertEquals("Product shirt", item.productName());
        assertEquals("shirt", item.productSlug());
        assertEquals(0, BigDecimal.valueOf(100_000).compareTo(item.unitPrice()));
        assertEquals(2, item.quantity());
        assertEquals(0, BigDecimal.valueOf(200_000).compareTo(item.totalPrice()));
    }

    @Test
    void getCart_sumsQuantitiesAndPricesOverAllItems() {
        VariantId shirt = variant("shirt", "SHIRT-1", 100_000, List.of(), null);
        VariantId hat = variant("hat", "HAT-1", 50_000, List.of(), null);

        CartView view = cartWith(shirt, 2, hat, 3);

        assertNotNull(view.id());
        assertEquals(USER.value(), view.userId());
        assertEquals(5, view.totalQuantity());
        assertEquals(0, BigDecimal.valueOf(350_000).compareTo(view.subtotal()));
    }

    @Test
    void getCart_listsItemsInTheOrderTheyWereAdded() {
        VariantId shirt = variant("shirt", "SHIRT-1", 100_000, List.of(), null);
        VariantId hat = variant("hat", "HAT-1", 50_000, List.of(), null);

        CartView view = cartWith(hat, 1, shirt, 1);

        assertEquals(List.of("HAT-1", "SHIRT-1"), view.items().stream().map(CartItemView::sku).toList());
    }

    @Test
    void getCart_onlyShowsTheRequestedUsersItems() {
        VariantId shirt = variant("shirt", "SHIRT-1", 100_000, List.of(), null);
        Cart other = Cart.create(UserId.of(8L));
        other.addItem(shirt, 9);
        cartRepository.save(other);

        CartView view = cartWith(shirt, 1);

        assertEquals(1, view.totalQuantity());
    }

    // ---- images ----

    @Test
    void getCart_usesTheVariantsOwnImageWhenItHasOne() {
        VariantId shirt = variant("shirt", "SHIRT-1", 100_000, List.of("img-a", "img-b"), "img-b");

        CartView view = cartWith(shirt, 1);

        assertEquals("http://cdn.test/shop/img-b", view.items().get(0).mediaUrl());
    }

    @Test
    void getCart_fallsBackToTheFirstImageOfTheProductGallery() {
        VariantId shirt = variant("shirt", "SHIRT-1", 100_000, List.of("img-c", "img-d"), null);

        CartView view = cartWith(shirt, 1);

        assertEquals("http://cdn.test/shop/img-c", view.items().get(0).mediaUrl());
    }

    @Test
    void getCart_hasNoImageWhenNeitherTheVariantNorTheProductHasOne() {
        VariantId shirt = variant("shirt", "SHIRT-1", 100_000, List.of(), null);

        CartView view = cartWith(shirt, 1);

        assertNull(view.items().get(0).mediaUrl());
    }
}
