package io.d4tzz.ecommerce.catalog.domain.product;

import io.d4tzz.ecommerce.common.Repository;

public interface ProductRepository extends Repository<Product, ProductId> {
    boolean existsBySlug(ProductSlug slug);

    boolean existsBySku(Sku sku);
}
