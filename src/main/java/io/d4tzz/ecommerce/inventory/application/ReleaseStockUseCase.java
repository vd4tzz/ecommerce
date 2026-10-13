package io.d4tzz.ecommerce.inventory.application;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.inventory.application.command.ReleaseStockCommand;
import io.d4tzz.ecommerce.inventory.domain.InventoryItem;
import io.d4tzz.ecommerce.inventory.domain.InventoryItemRepository;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerEntry;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerRepository;
import io.d4tzz.ecommerce.inventory.domain.StockReservation;
import io.d4tzz.ecommerce.inventory.domain.StockReservationRepository;
import io.d4tzz.ecommerce.inventory.domain.StockReservationService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReleaseStockUseCase {

    private final InventoryItemRepository inventoryItemRepository;
    private final StockReservationRepository reservationRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final StockReservationService reservationService;

    public ReleaseStockUseCase(
        InventoryItemRepository inventoryItemRepository,
        StockReservationRepository reservationRepository,
        StockLedgerRepository stockLedgerRepository,
        StockReservationService reservationService
    ) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.reservationRepository = reservationRepository;
        this.stockLedgerRepository = stockLedgerRepository;
        this.reservationService = reservationService;
    }

    /**
     * Releases every reservation of a cancelled/expired order.
     * Already released reservations are skipped.
     *
     * @throws InvalidOperationException if a reservation of the order cannot be released,
     * because it is committed or restocked
     */
    public void execute(ReleaseStockCommand command) {
        Instant now = Instant.now();

        // Lock order: reservations (by warehouse id), then items of each reservation (by variant id)
        for (StockReservation reservation : reservationRepository.findAllForUpdate(command.orderId())) {
            Map<VariantId, InventoryItem> items = inventoryItemRepository.findAllForUpdate(
                reservation.getWarehouseId(),
                reservation.variantIds()
            );

            List<StockLedgerEntry> entries = reservationService.release(reservation, items, now);
            if (entries.isEmpty()) {
                continue;
            }

            inventoryItemRepository.saveAll(items.values());
            reservationRepository.save(reservation);
            stockLedgerRepository.saveAll(entries);
        }
    }
}
