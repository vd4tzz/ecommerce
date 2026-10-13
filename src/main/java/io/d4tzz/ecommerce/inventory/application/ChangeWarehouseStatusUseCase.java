package io.d4tzz.ecommerce.inventory.application;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.inventory.application.command.ChangeWarehouseStatusCommand;
import io.d4tzz.ecommerce.inventory.domain.Warehouse;
import io.d4tzz.ecommerce.inventory.domain.WarehouseId;
import io.d4tzz.ecommerce.inventory.domain.WarehouseRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChangeWarehouseStatusUseCase {

    private final WarehouseRepository warehouseRepository;

    public ChangeWarehouseStatusUseCase(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    /**
     * Activates or deactivates a warehouse. Deactivating only blocks new receipts and reservations; existing
     * reservations can still be released, committed or restocked.
     *
     * @throws NotFoundException if the warehouse does not exist
     */
    public void execute(ChangeWarehouseStatusCommand command) {
        Warehouse warehouse = warehouseRepository
            .find(WarehouseId.of(command.warehouseId()))
            .orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + command.warehouseId()));

        Instant now = Instant.now();
        boolean changed = switch (command.status()) {
            case ACTIVE -> warehouse.activate(now);
            case INACTIVE -> warehouse.deactivate(now);
        };

        if (changed) {
            warehouseRepository.save(warehouse);
        }
    }
}
