package io.d4tzz.ecommerce.inventory.query;

/**
 * Every filter is optional. {@code maxAvailable} keeps the items with at most that many sellable units: 0 lists
 * what is out of stock, a small number what is running low. With it, the fewest sellable units come first.
 */
public record StockSearchCriteria(
    Long warehouseId,
    String keyword,
    Integer maxAvailable,
    Integer page,
    Integer size
) {
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
