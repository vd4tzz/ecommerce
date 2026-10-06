package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;

/** The business document that caused a stock change. */
public record StockLedgerReference(Type type, String value) {

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

    public static StockLedgerReference order(Long orderId) {
        return new StockLedgerReference(Type.ORDER, orderId.toString());
    }

    public static StockLedgerReference goodsReceipt(String receiptCode) {
        return new StockLedgerReference(Type.GOODS_RECEIPT, receiptCode);
    }
}
