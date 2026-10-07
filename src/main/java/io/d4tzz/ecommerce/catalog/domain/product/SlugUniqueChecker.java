package io.d4tzz.ecommerce.catalog.domain.product;

@FunctionalInterface
public interface SlugUniqueChecker {
    boolean exists(ProductSlug slug);
}
