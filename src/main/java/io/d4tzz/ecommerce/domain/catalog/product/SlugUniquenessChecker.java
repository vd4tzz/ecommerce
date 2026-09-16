package io.d4tzz.ecommerce.domain.catalog.product;

@FunctionalInterface
public interface SlugUniquenessChecker {
    boolean exists(ProductSlug slug);
}
