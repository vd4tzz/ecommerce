package io.d4tzz.ecommerce.inventory.application;

import io.d4tzz.ecommerce.domain.user.Address;
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
