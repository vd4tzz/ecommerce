package io.d4tzz.ecommerce.domain.catalog.product;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.d4tzz.ecommerce.common.Repository;
import io.d4tzz.ecommerce.domain.catalog.category.CategoryId;
import io.d4tzz.ecommerce.domain.catalog.option.OptionId;
import io.d4tzz.ecommerce.domain.catalog.option.ValueId;

public interface ProductRepository extends Repository<Product, ProductId> {
    boolean existsBySlug(ProductSlug slug);

    boolean existsBySku(Sku sku);

    default Product restore(
            ProductId id,
            ProductName name,
            ProductSlug slug,
            Description description,
            CategoryId categoryId,
            ProductStatus status,
            Map<OptionId, Set<ValueId>> options,
            Set<MediaId> media,
            List<Variant> variants,
            Map<String, List<String>> attributes) {
        return Product.restore(
                id,
                name,
                slug,
                description,
                categoryId,
                status,
                options,
                media,
                variants,
                attributes);
    }
}
