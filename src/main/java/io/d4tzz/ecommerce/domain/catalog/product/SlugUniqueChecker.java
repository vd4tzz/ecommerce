package io.d4tzz.ecommerce.domain.catalog.product;

@FunctionalInterface
public interface SlugUniqueChecker {
    boolean exists(ProductSlug slug);
}
