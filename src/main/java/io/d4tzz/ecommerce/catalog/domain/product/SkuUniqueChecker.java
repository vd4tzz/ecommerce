package io.d4tzz.ecommerce.catalog.domain.product;

@FunctionalInterface
public interface SkuUniqueChecker {
    boolean exists(Sku sku);
}
