package io.d4tzz.ecommerce.inventory.application;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.inventory.application.command.AllocateStockCommand;
import io.d4tzz.ecommerce.inventory.application.result.StockAllocation;
import io.d4tzz.ecommerce.inventory.domain.ReservationLine;
import io.d4tzz.ecommerce.inventory.domain.StockAllocator;
import io.d4tzz.ecommerce.inventory.domain.WarehouseAllocation;
import io.d4tzz.ecommerce.inventory.domain.WarehouseId;
import io.d4tzz.ecommerce.inventory.domain.WarehouseStock;
import io.d4tzz.ecommerce.inventory.query.StockQueryService;
import io.d4tzz.ecommerce.inventory.query.VariantAvailabilityView;
import io.d4tzz.ecommerce.inventory.query.WarehouseQueryService;
import io.d4tzz.ecommerce.inventory.query.WarehouseView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AllocateStockUseCase {

    private final StockQueryService stockQueryService;
    private final WarehouseQueryService warehouseQueryService;
    private final StockAllocator stockAllocator;

    public AllocateStockUseCase(
        StockQueryService stockQueryService,
        WarehouseQueryService warehouseQueryService,
        StockAllocator stockAllocator
    ) {
        this.stockQueryService = stockQueryService;
        this.warehouseQueryService = warehouseQueryService;
        this.stockAllocator = stockAllocator;
    }

    /**
     * Says which active warehouses should send an order, so checkout can price shipping per warehouse and then
     * reserve. Only reads: call {@link ReserveStockUseCase} once per returned warehouse, in the returned order, to
     * actually hold the stock. Returns one entry per warehouse, in ascending warehouse id order.
     *
     * @throws InvalidOperationException if a variant has too little stock in all active warehouses together
     */
    public List<StockAllocation> execute(AllocateStockCommand command) {
        List<ReservationLine> lines = command
            .lines()
            .stream()
            .map(line -> new ReservationLine(VariantId.of(line.variantId()), line.quantity()))
            .toList();

        // warehouse_id -> variant_id -> quantity
        Map<WarehouseId, Map<VariantId, Integer>> availableByWarehouse = new HashMap<>();
        // for each variant, get the available quantity with warehouse_id
        List<VariantAvailabilityView> availability = stockQueryService.getAvailability(
            command.lines().stream().map(AllocateStockCommand.Line::variantId).distinct().toList()
        );
        // build availableByWarehouse
        for (VariantAvailabilityView view : availability) {
            if (view.available() > 0) {
                availableByWarehouse
                    .computeIfAbsent(WarehouseId.of(view.warehouseId()), id -> new HashMap<>())
                    .put(VariantId.of(view.variantId()), view.available());
            }
        }

        Map<WarehouseId, String> provinces = warehouseQueryService
            .getWarehouses()
            .stream()
            .collect(Collectors.toMap(warehouse -> WarehouseId.of(warehouse.id()), WarehouseView::province));

        List<WarehouseStock> warehouses = availableByWarehouse
            .entrySet()
            .stream()
            .map(entry ->
                new WarehouseStock(
                    entry.getKey(),
                    sameProvince(provinces.get(entry.getKey()), command.province()),
                    entry.getValue()
                )
            )
            .toList();

        return stockAllocator.allocate(lines, warehouses).stream().map(AllocateStockUseCase::toResult).toList();
    }

    /** Plain text match: "Hà Nội" and "TP. Hà Nội" count as different, which only loses the preference. */
    private static boolean sameProvince(String a, String b) {
        return a != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private static StockAllocation toResult(WarehouseAllocation allocation) {
        return new StockAllocation(
            allocation.warehouseId().value(),
            allocation
                .lines()
                .stream()
                .map(line -> new StockAllocation.Line(line.variantId().value(), line.quantity()))
                .toList()
        );
    }
}
