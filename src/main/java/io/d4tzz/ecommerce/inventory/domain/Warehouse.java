package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.Address;
import java.time.Instant;

/**
 * A place that keeps stock. It is ACTIVE when created. While INACTIVE it takes no new goods and no new
 * reservations, but reservations it already holds can still be released, committed or restocked.
 */
public class Warehouse {

    private static final int NAME_MAX_LENGTH = 150;

    private final WarehouseId id;
    private final WarehouseCode code;
    private String name;
    private Address address;
    private WarehouseStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Warehouse(
        WarehouseId id,
        WarehouseCode code,
        String name,
        Address address,
        WarehouseStatus status,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.code = code;
        this.name = validateName(name);
        this.address = address;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * A new ACTIVE warehouse.
     *
     * @throws ConflictException        if the code is taken
     * @throws InvalidArgumentException if the name is blank or longer than 150 characters
     */
    public static Warehouse create(
        WarehouseCode code,
        String name,
        Address address,
        WarehouseCodeChecker checker,
        Instant now
    ) {
        if (checker.exists(code)) {
            throw new ConflictException("Warehouse code '" + code + "' already exists");
        }
        return new Warehouse(null, code, name, address, WarehouseStatus.ACTIVE, now, now);
    }

    /**
     * Rebuilds a warehouse from storage.
     *
     * @throws InvalidArgumentException if the name is blank or longer than 150 characters
     */
    public static Warehouse restore(
        WarehouseId id,
        WarehouseCode code,
        String name,
        Address address,
        WarehouseStatus status,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new Warehouse(id, code, name, address, status, createdAt, updatedAt);
    }

    /**
     * Changes the name and address. The code never changes.
     *
     * @throws InvalidArgumentException if the name is blank or longer than 150 characters
     */
    public void update(String name, Address address, Instant now) {
        this.name = validateName(name);
        this.address = address;
        this.updatedAt = now;
    }

    /** Returns {@code false} when it was already active. */
    public boolean activate(Instant now) {
        if (status == WarehouseStatus.ACTIVE) {
            return false;
        }
        this.status = WarehouseStatus.ACTIVE;
        this.updatedAt = now;
        return true;
    }

    /** Returns {@code false} when it was already inactive. */
    public boolean deactivate(Instant now) {
        if (status == WarehouseStatus.INACTIVE) {
            return false;
        }
        this.status = WarehouseStatus.INACTIVE;
        this.updatedAt = now;
        return true;
    }

    public boolean isActive() {
        return status == WarehouseStatus.ACTIVE;
    }

    /**
     * Checks the warehouse can take part in new receipts and reservations.
     *
     * @throws InvalidOperationException if the warehouse is inactive
     */
    public void ensureActive() {
        if (!isActive()) {
            throw new InvalidOperationException("Warehouse " + code + " is inactive");
        }
    }

    private static String validateName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > NAME_MAX_LENGTH) {
            throw new InvalidArgumentException(
                "Warehouse name must be between 1 and " + NAME_MAX_LENGTH + " characters"
            );
        }
        return trimmed;
    }

    public WarehouseId getId() {
        return id;
    }

    public WarehouseCode getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Address getAddress() {
        return address;
    }

    public WarehouseStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
