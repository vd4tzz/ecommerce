package io.d4tzz.ecommerce.inventory.application;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.inventory.application.command.ReserveStockCommand;
import io.d4tzz.ecommerce.inventory.domain.InventoryItem;
import io.d4tzz.ecommerce.inventory.domain.InventoryItemRepository;
import io.d4tzz.ecommerce.inventory.domain.ReservationLine;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerRepository;
import io.d4tzz.ecommerce.inventory.domain.StockReservationRepository;
import io.d4tzz.ecommerce.inventory.domain.StockReservationService;
import io.d4tzz.ecommerce.inventory.domain.Warehouse;
import io.d4tzz.ecommerce.inventory.domain.WarehouseId;
import io.d4tzz.ecommerce.inventory.domain.WarehouseRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReserveStockUseCase {

    private final WarehouseRepository warehouseRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockReservationRepository reservationRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final StockReservationService reservationService;

    public ReserveStockUseCase(
        WarehouseRepository warehouseRepository,
        InventoryItemRepository inventoryItemRepository,
        StockReservationRepository reservationRepository,
        StockLedgerRepository stockLedgerRepository,
        StockReservationService reservationService
    ) {
        this.warehouseRepository = warehouseRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.reservationRepository = reservationRepository;
        this.stockLedgerRepository = stockLedgerRepository;
        this.reservationService = reservationService;
    }

    /**
     * Holds stock for an order in one warehouse. Meant to be called synchronously by order placement, inside the
     * same transaction: if any line is out of stock the whole order fails.
     *
     * <p>Idempotent per (order, warehouse): does nothing when the order already holds stock in the warehouse. When an
     * order spans several warehouses, call it once per warehouse in ascending warehouse id order.
     *
     * @throws NotFoundException         if the warehouse does not exist
     * @throws InvalidOperationException if the warehouse is inactive, or a variant has no item or too little
     *                                   available stock
     */
    public void execute(ReserveStockCommand command) {
        Long orderId = command.orderId();
        WarehouseId warehouseId = WarehouseId.of(command.warehouseId());

        // A concurrent duplicate that slips past this check fails on the unique (order, warehouse) constraint
        if (reservationRepository.exists(orderId, warehouseId)) {
            return;
        }

        Warehouse warehouse = warehouseRepository
            .find(warehouseId)
            .orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + command.warehouseId()));

        List<ReservationLine> lines = command
            .lines()
            .stream()
            .map(line -> new ReservationLine(VariantId.of(line.variantId()), line.quantity()))
            .toList();

        Map<VariantId, InventoryItem> items = inventoryItemRepository.findAllForUpdate(
            warehouseId,
            lines.stream().map(ReservationLine::variantId).toList()
        );

        var result = reservationService.reserve(orderId, warehouse, lines, items, Instant.now());

        inventoryItemRepository.saveAll(items.values());
        reservationRepository.save(result.reservation());
        stockLedgerRepository.saveAll(result.entries());
    }
}
