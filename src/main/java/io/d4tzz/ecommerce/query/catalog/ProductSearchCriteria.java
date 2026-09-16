package io.d4tzz.ecommerce.query.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductSearchCriteria(
    String keyword,
    Long categoryId,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    List<Long> optionValueIds,
    Map<String, String> attributes,
    String sortBy,
    Integer page,
    Integer size,
    Boolean fullFacets
) {
    public ProductSearchCriteria(
        String keyword,
        Long categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        List<Long> optionValueIds,
        Map<String, String> attributes,
        String sortBy,
        Integer page,
        Integer size
    ) {
        this(
            keyword,
            categoryId,
            minPrice,
            maxPrice,
            optionValueIds,
            attributes,
            sortBy,
            page,
            size,
            null
        );
    }

    public boolean isFullFacets() {
        return Boolean.TRUE.equals(fullFacets);
    }

    public static ProductSearchCriteria empty() {
        return new ProductSearchCriteria(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public int getPage() {
        return page == null || page < 0 ? 0 : page;
    }

    public int getSize() {
        return size == null || size <= 0 ? 20 : size;
    }

    public int getOffset() {
        return getPage() * getSize();
    }
}
