package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;

/**
 * The business document that caused a stock change. The value is trimmed; a blank one throws an
 * {@link InvalidArgumentException}.
 */
public record StockLedgerReference(Type type, String value) {

    /** The kind of document: an order, or a goods receipt typed in by an admin. */
    public enum Type {
        ORDER,
        GOODS_RECEIPT
    }

    public StockLedgerReference {
        value = value.trim();
        if (value.isEmpty()) {
            throw new InvalidArgumentException("Stock reference must not be blank");
        }
    }

    /** An order, by its id. */
    public static StockLedgerReference order(Long orderId) {
        return new StockLedgerReference(Type.ORDER, orderId.toString());
    }

    /** A goods receipt, by the code the admin gave it. */
    public static StockLedgerReference goodsReceipt(String receiptCode) {
        return new StockLedgerReference(Type.GOODS_RECEIPT, receiptCode);
    }
}
