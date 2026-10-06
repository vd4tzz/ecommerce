package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stock held for one order in one warehouse. Unique per (order, warehouse).
 *
 * <p>Status changes are package-private: they are driven by {@link StockReservationService} together with the
 * matching {@link InventoryItem} changes. They return {@code false} when already in the target status, so
 * redelivered events are harmless.
 */
public class StockReservation {

    private final StockReservationId id;
    // the order the stock is held for; inventory only keeps the reference
    private final Long orderId;
    private final WarehouseId warehouseId;
    private final List<ReservationLine> lines;
    private ReservationStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private StockReservation(
        StockReservationId id,
        Long orderId,
        WarehouseId warehouseId,
        List<ReservationLine> lines,
        ReservationStatus status,
        Instant createdAt,
        Instant updatedAt
    ) {
        if (lines.isEmpty()) {
            throw new InvalidArgumentException("Reservation must contain at least one line");
        }
        this.id = id;
        this.orderId = orderId;
        this.warehouseId = warehouseId;
        this.lines = List.copyOf(lines);
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static StockReservation create(Long orderId, WarehouseId warehouseId, List<ReservationLine> lines, Instant now) {
        return new StockReservation(
            null,
            orderId,
            warehouseId,
            mergeByVariant(lines),
            ReservationStatus.RESERVED,
            now,
            now
        );
    }

    public static StockReservation restore(
        StockReservationId id,
        Long orderId,
        WarehouseId warehouseId,
        List<ReservationLine> lines,
        ReservationStatus status,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new StockReservation(id, orderId, warehouseId, lines, status, createdAt, updatedAt);
    }

    boolean release(Instant now) {
        return transitionTo(ReservationStatus.RELEASED, now);
    }

    boolean commit(Instant now) {
        return transitionTo(ReservationStatus.COMMITTED, now);
    }

    boolean restock(Instant now) {
        return transitionTo(ReservationStatus.RESTOCKED, now);
    }

    public List<VariantId> variantIds() {
        return lines.stream().map(ReservationLine::variantId).toList();
    }

    private boolean transitionTo(ReservationStatus next, Instant now) {
        if (status == next) {
            return false;
        }
        if (!status.canTransitionTo(next)) {
            throw new InvalidOperationException(
                "Cannot change reservation of order " + orderId + " from " + status + " to " + next
            );
        }
        this.status = next;
        this.updatedAt = now;
        return true;
    }

    /** Sums duplicate variants and sorts by variant id (also the lock order of inventory items). */
    private static List<ReservationLine> mergeByVariant(List<ReservationLine> lines) {
        Map<VariantId, Integer> quantities = new LinkedHashMap<>();
        for (ReservationLine line : lines) {
            quantities.merge(line.variantId(), line.quantity(), Integer::sum);
        }
        return quantities
            .entrySet()
            .stream()
            .map(e -> new ReservationLine(e.getKey(), e.getValue()))
            .sorted(Comparator.comparing(line -> line.variantId().value()))
            .toList();
    }

    public StockReservationId getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public WarehouseId getWarehouseId() {
        return warehouseId;
    }

    public List<ReservationLine> getLines() {
        return lines;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
