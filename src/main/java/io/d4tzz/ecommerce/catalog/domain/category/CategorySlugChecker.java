package io.d4tzz.ecommerce.catalog.domain.category;

@FunctionalInterface
public interface CategorySlugChecker {
    boolean exists(CategorySlug slug);
}
