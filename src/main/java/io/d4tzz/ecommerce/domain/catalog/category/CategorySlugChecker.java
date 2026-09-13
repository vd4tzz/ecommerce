package io.d4tzz.ecommerce.domain.catalog.category;

@FunctionalInterface
public interface CategorySlugChecker {
    boolean exists(CategorySlug slug);
}
