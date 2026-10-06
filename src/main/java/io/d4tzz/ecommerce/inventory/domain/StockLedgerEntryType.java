package io.d4tzz.ecommerce.inventory.domain;

public enum StockLedgerEntryType {
    /** Goods received into the warehouse: onHand increases. */
    RECEIVE,
    /** Stock held for an order: reserved increases. */
    RESERVE,
    /** Hold released (order cancelled/expired): reserved decreases. */
    RELEASE,
    /** Goods left the warehouse for an order: onHand and reserved decrease. */
    COMMIT,
    /** Shipped goods came back (delivery failed): onHand increases. */
    RESTOCK
}
