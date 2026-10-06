package io.d4tzz.ecommerce.inventory.infrastructure.persistence.entity;

import io.d4tzz.ecommerce.inventory.domain.ReservationStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "StockReservation")
@Table(
    name = "stock_reservations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_stock_reservations_order_warehouse", columnNames = { "order_id", "warehouse_id" })
    },
    indexes = {
        @Index(name = "idx_stock_reservations_warehouse_status", columnList = "warehouse_id, status")
    }
)
public class JpaStockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @ElementCollection
    @CollectionTable(
        name = "stock_reservation_lines",
        joinColumns = @JoinColumn(name = "reservation_id", nullable = false),
        indexes = @Index(name = "idx_stock_reservation_lines_reservation_id", columnList = "reservation_id")
    )
    @OrderBy("variantId ASC")
    private List<JpaReservationLine> lines = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public JpaStockReservation() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public List<JpaReservationLine> getLines() {
        return lines;
    }

    public void setLines(List<JpaReservationLine> lines) {
        this.lines = lines;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
