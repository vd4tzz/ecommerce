package io.d4tzz.ecommerce.payment.query;

/** {@code status} is PENDING or DONE; {@code orderReference} is the order code. Every filter is optional. */
public record RefundRequestSearchCriteria(String status, String orderReference, Integer page, Integer size) {
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
