package io.d4tzz.ecommerce.payment.query;

import java.time.Instant;

/**
 * {@code keyword} matches the transaction number or the order code. {@code unmatched} true keeps the payments no
 * order code was found for, waiting for a person to assign them. {@code from} (included) and {@code to} (excluded)
 * bound when the money moved. Every filter is optional.
 */
public record PaymentSearchCriteria(
    String orderReference,
    String type,
    String method,
    String shopAccount,
    String keyword,
    Boolean unmatched,
    Instant from,
    Instant to,
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
