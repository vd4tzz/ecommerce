package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.common.Identifier;
import java.util.UUID;

public class StockLedgerEntryId extends Identifier<UUID> {

    public StockLedgerEntryId(UUID id) {
        super(id);
    }

    public static StockLedgerEntryId of(UUID id) {
        return new StockLedgerEntryId(id);
    }

    public static StockLedgerEntryId generate() {
        return new StockLedgerEntryId(UUID.randomUUID());
    }
}
