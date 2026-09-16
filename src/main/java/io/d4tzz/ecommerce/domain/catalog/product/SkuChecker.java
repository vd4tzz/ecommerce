package io.d4tzz.ecommerce.domain.catalog.product;

@FunctionalInterface
public interface SkuChecker {
    boolean exists(Sku sku);
}
