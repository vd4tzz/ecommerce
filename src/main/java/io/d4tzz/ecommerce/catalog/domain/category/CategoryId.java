package io.d4tzz.ecommerce.catalog.domain.category;

import io.d4tzz.ecommerce.common.Identifier;

public class CategoryId extends Identifier<Long> {

    public CategoryId(Long id) {
        super(id);
    }

    public static CategoryId of(Long id) {
        return new CategoryId(id);
    }
}
