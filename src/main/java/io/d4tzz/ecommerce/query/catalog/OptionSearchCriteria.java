package io.d4tzz.ecommerce.query.catalog;

public record OptionSearchCriteria(
    String keyword,
    Integer page,
    Integer size
) {
    public static OptionSearchCriteria of(int page, int size) {
        return new OptionSearchCriteria(null, page, size);
    }

    public static OptionSearchCriteria of(String keyword, int page, int size) {
        return new OptionSearchCriteria(keyword, page, size);
    }

    public static OptionSearchCriteria selectLookup(String keyword, int limit) {
        return new OptionSearchCriteria(keyword, 0, limit);
    }

    public static OptionSearchCriteria empty() {
        return new OptionSearchCriteria(null, 0, 20);
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
