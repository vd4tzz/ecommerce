package io.d4tzz.ecommerce.inventory.query;

import java.time.Instant;
import java.util.UUID;

public record StockLedgerEntryView(
    UUID id,
    Long warehouseId,
    String warehouseCode,
    Long variantId,
    String sku,
    String productName,
    String type,
    int quantity,
    int onHandAfter,
    int reservedAfter,
    String referenceType,
    String referenceValue,
    String note,
    Instant occurredAt
) {}
