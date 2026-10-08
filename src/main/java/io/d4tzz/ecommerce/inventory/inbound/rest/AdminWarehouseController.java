package io.d4tzz.ecommerce.inventory.inbound.rest;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.inventory.application.ChangeWarehouseStatusUseCase;
import io.d4tzz.ecommerce.inventory.application.CreateWarehouseUseCase;
import io.d4tzz.ecommerce.inventory.application.UpdateWarehouseUseCase;
import io.d4tzz.ecommerce.inventory.application.command.ChangeWarehouseStatusCommand;
import io.d4tzz.ecommerce.inventory.application.command.CreateWarehouseCommand;
import io.d4tzz.ecommerce.inventory.application.command.UpdateWarehouseCommand;
import io.d4tzz.ecommerce.inventory.domain.WarehouseStatus;
import io.d4tzz.ecommerce.inventory.query.WarehouseQueryService;
import io.d4tzz.ecommerce.inventory.query.WarehouseView;
import io.d4tzz.ecommerce.spring.SpringValidation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/admin/warehouses")
@Tag(name = "Admin - Warehouses", description = "Manage warehouses")
public class AdminWarehouseController {

    private final WarehouseQueryService warehouseQueryService;
    private final CreateWarehouseUseCase createWarehouseUseCase;
    private final UpdateWarehouseUseCase updateWarehouseUseCase;
    private final ChangeWarehouseStatusUseCase changeWarehouseStatusUseCase;
    private final Validator validator;

    public AdminWarehouseController(
        WarehouseQueryService warehouseQueryService,
        CreateWarehouseUseCase createWarehouseUseCase,
        UpdateWarehouseUseCase updateWarehouseUseCase,
        ChangeWarehouseStatusUseCase changeWarehouseStatusUseCase,
        Validator validator
    ) {
        this.warehouseQueryService = warehouseQueryService;
        this.createWarehouseUseCase = createWarehouseUseCase;
        this.updateWarehouseUseCase = updateWarehouseUseCase;
        this.changeWarehouseStatusUseCase = changeWarehouseStatusUseCase;
        this.validator = validator;
    }

    @GetMapping
    public ResponseEntity<List<WarehouseView>> getWarehouses() {
        return ResponseEntity.ok(warehouseQueryService.getWarehouses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseView> getWarehouse(@PathVariable Long id) {
        return ResponseEntity.ok(findWarehouse(id));
    }

    @PostMapping
    public ResponseEntity<WarehouseView> createWarehouse(@RequestBody WarehouseRequest request) {
        var command = new CreateWarehouseCommand(
            request.code(),
            request.name(),
            request.province(),
            request.district(),
            request.ward(),
            request.detailAddress()
        );
        SpringValidation.validate(command, validator);
        Long id = createWarehouseUseCase.execute(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(findWarehouse(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WarehouseView> updateWarehouse(@PathVariable Long id, @RequestBody WarehouseRequest request) {
        var command = new UpdateWarehouseCommand(
            id,
            request.name(),
            request.province(),
            request.district(),
            request.ward(),
            request.detailAddress()
        );
        SpringValidation.validate(command, validator);
        updateWarehouseUseCase.execute(command);
        return ResponseEntity.ok(findWarehouse(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WarehouseView> changeStatus(@PathVariable Long id, @RequestBody ChangeWarehouseStatusRequest request) {
        var command = new ChangeWarehouseStatusCommand(id, request.status());
        SpringValidation.validate(command, validator);
        changeWarehouseStatusUseCase.execute(command);
        return ResponseEntity.ok(findWarehouse(id));
    }

    private WarehouseView findWarehouse(Long id) {
        return warehouseQueryService
            .getWarehouse(id)
            .orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + id));
    }
}

/** {@code code} is ignored on update: a warehouse code never changes. */
record WarehouseRequest(
    String code,
    String name,
    String province,
    String district,
    String ward,
    String detailAddress
) {}

record ChangeWarehouseStatusRequest(WarehouseStatus status) {}
