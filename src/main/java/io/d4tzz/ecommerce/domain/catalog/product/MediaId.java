package io.d4tzz.ecommerce.domain.catalog.product;

import io.d4tzz.ecommerce.common.Identifier;

public class MediaId extends Identifier<String> {

    public MediaId(String id) {
        super(id);
    }

    public static MediaId of(String id) {
        return new MediaId(id);
    }
}
