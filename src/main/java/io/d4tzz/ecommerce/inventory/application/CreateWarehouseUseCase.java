package io.d4tzz.ecommerce.inventory.application;

import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.inventory.application.command.CreateWarehouseCommand;
import io.d4tzz.ecommerce.inventory.domain.Warehouse;
import io.d4tzz.ecommerce.inventory.domain.WarehouseCode;
import io.d4tzz.ecommerce.inventory.domain.WarehouseRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateWarehouseUseCase {

    private final WarehouseRepository warehouseRepository;

    public CreateWarehouseUseCase(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    /**
     * Adds a warehouse. Its code must not be used by another warehouse. Returns the id of the new warehouse.
     *
     * @throws InvalidArgumentException if the code, name or address is invalid
     * @throws ConflictException        if the code is already used by another warehouse
     */
    public Long execute(CreateWarehouseCommand command) {
        Warehouse warehouse = Warehouse.create(
            WarehouseCode.of(command.code()),
            command.name(),
            Address.of(command.province(), command.district(), command.ward(), command.detailAddress()),
            warehouseRepository::existsByCode,
            Instant.now()
        );
        return warehouseRepository.save(warehouse).getId().value();
    }
}
