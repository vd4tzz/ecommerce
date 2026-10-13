package io.d4tzz.ecommerce.inventory.application;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.inventory.application.command.RestockReturnedCommand;
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
public class RestockReturnedUseCase {

    private final InventoryItemRepository inventoryItemRepository;
    private final StockReservationRepository reservationRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final StockReservationService reservationService;

    public RestockReturnedUseCase(
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
     * The shipped goods of an order came back: puts them back into sellable stock in every warehouse of the order.
     * Already restocked reservations are skipped.
     *
     * @throws InvalidOperationException if a reservation of the order cannot be restocked, because it is not committed
     */
    public void execute(RestockReturnedCommand command) {
        Instant now = Instant.now();

        // Lock order: reservations (by warehouse id), then items of each reservation (by variant id)
        for (StockReservation reservation : reservationRepository.findAllForUpdate(command.orderId())) {
            Map<VariantId, InventoryItem> items = inventoryItemRepository.findAllForUpdate(
                reservation.getWarehouseId(),
                reservation.variantIds()
            );

            List<StockLedgerEntry> entries = reservationService.restock(reservation, items, now);
            if (entries.isEmpty()) {
                continue;
            }

            inventoryItemRepository.saveAll(items.values());
            reservationRepository.save(reservation);
            stockLedgerRepository.saveAll(entries);
        }
    }
}
