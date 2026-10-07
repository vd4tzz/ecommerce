package io.d4tzz.ecommerce.inventory.application;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.inventory.application.command.UpdateWarehouseCommand;
import io.d4tzz.ecommerce.inventory.domain.Warehouse;
import io.d4tzz.ecommerce.inventory.domain.WarehouseId;
import io.d4tzz.ecommerce.inventory.domain.WarehouseRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateWarehouseUseCase {

    private final WarehouseRepository warehouseRepository;

    public UpdateWarehouseUseCase(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public void execute(UpdateWarehouseCommand command) {
        Warehouse warehouse = warehouseRepository
            .find(WarehouseId.of(command.warehouseId()))
            .orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + command.warehouseId()));

        warehouse.update(
            command.name(),
            Address.of(command.province(), command.district(), command.ward(), command.detailAddress()),
            Instant.now()
        );
        warehouseRepository.save(warehouse);
    }
}
