package io.d4tzz.ecommerce.query.catalog.product;

public record AdminProductSearchCriteria(
    String keyword,
    String status,
    Long categoryId,
    String sortBy,
    Integer page,
    Integer size
) {
    public static AdminProductSearchCriteria empty() {
        return new AdminProductSearchCriteria(null, null, null, null, 0, 20);
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
