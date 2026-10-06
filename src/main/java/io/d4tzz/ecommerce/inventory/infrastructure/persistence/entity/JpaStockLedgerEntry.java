package io.d4tzz.ecommerce.inventory.infrastructure.persistence.entity;

import io.d4tzz.ecommerce.inventory.domain.StockLedgerEntryType;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "StockLedgerEntry")
@Table(
    name = "stock_ledger_entries",
    indexes = {
        @Index(name = "idx_stock_ledger_warehouse_variant", columnList = "warehouse_id, variant_id, occurred_at"),
        @Index(name = "idx_stock_ledger_reference", columnList = "reference_type, reference_value"),
        @Index(name = "idx_stock_ledger_occurred_at", columnList = "occurred_at"),
    }
)
public class JpaStockLedgerEntry {

    @Id
    private UUID id;

    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private Long warehouseId;

    @Column(name = "variant_id", nullable = false, updatable = false)
    private Long variantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, updatable = false)
    private StockLedgerEntryType type;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    @Column(name = "on_hand_after", nullable = false, updatable = false)
    private int onHandAfter;

    @Column(name = "reserved_after", nullable = false, updatable = false)
    private int reservedAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 30, updatable = false)
    private StockLedgerReference.Type referenceType;

    @Column(name = "reference_value", nullable = false, length = 100, updatable = false)
    private String referenceValue;

    @Column(name = "note", nullable = false, updatable = false)
    private String note;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    public JpaStockLedgerEntry() {}

    public JpaStockLedgerEntry(
        UUID id,
        Long warehouseId,
        Long variantId,
        StockLedgerEntryType type,
        int quantity,
        int onHandAfter,
        int reservedAfter,
        StockLedgerReference.Type referenceType,
        String referenceValue,
        String note,
        Instant occurredAt
    ) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.variantId = variantId;
        this.type = type;
        this.quantity = quantity;
        this.onHandAfter = onHandAfter;
        this.reservedAfter = reservedAfter;
        this.referenceType = referenceType;
        this.referenceValue = referenceValue;
        this.note = note;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public StockLedgerEntryType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getOnHandAfter() {
        return onHandAfter;
    }

    public int getReservedAfter() {
        return reservedAfter;
    }

    public StockLedgerReference.Type getReferenceType() {
        return referenceType;
    }

    public String getReferenceValue() {
        return referenceValue;
    }

    public String getNote() {
        return note;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
