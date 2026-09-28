package io.d4tzz.ecommerce.domain.catalog.product;

@FunctionalInterface
public interface SkuUniqueChecker {
    boolean exists(Sku sku);
}
