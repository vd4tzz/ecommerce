package io.d4tzz.ecommerce.order.query;

/** {@code keyword} matches the order code, recipient name or phone. Every filter is optional. */
public record OrderSearchCriteria(
    Long userId,
    String status,
    String paymentStatus,
    Boolean readyToConfirm,
    String keyword,
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
