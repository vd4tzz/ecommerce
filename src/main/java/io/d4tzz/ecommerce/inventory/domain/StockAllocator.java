package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Picks which warehouses send an order. Each warehouse is one more package to pay shipping for, so it uses as
 * few as it can:
 * <ol>
 *   <li>Take the warehouse that can send the most of what is still missing. On a tie, prefer one near the
 *       destination, then the lowest id.
 *   <li>Repeat with what is left until nothing is missing.
 * </ol>
 * A warehouse that has everything always wins the first round, so the order then ships as one package. This is
 * greedy: fine for a handful of warehouses, but not always the fewest possible.
 *
 * <p>Nothing is held here. Holding stock is {@link StockReservationService}'s job, and it fails if someone took
 * the stock in between.
 */
public class StockAllocator {

    /**
     * One allocation per warehouse, in ascending warehouse id order (the order reservations must be made in).
     *
     * @throws InvalidArgumentException if there are no lines
     * @throws OutOfStockException       if a variant has too little available stock in all warehouses together
     */
    public List<WarehouseAllocation> allocate(List<ReservationLine> lines, List<WarehouseStock> warehouses) {
        if (lines.isEmpty()) {
            throw new InvalidArgumentException("Nothing to allocate");
        }

        Map<VariantId, Integer> missing = LinkedHashMap.newLinkedHashMap(lines.size());
        for (ReservationLine line : lines) {
            missing.merge(line.variantId(), line.quantity(), Integer::sum);
        }
        // ensure all stocks from warehouses are enough
        ensureEnoughStock(missing, warehouses);

        // preference get best warehouse
        Comparator<WarehouseStock> preference = Comparator.comparingInt((WarehouseStock warehouse) ->
            quantitiesCovered(warehouse, missing)
        )
            .thenComparing(WarehouseStock::nearDestination)
            .thenComparing(warehouse -> warehouse.warehouseId().value(), Comparator.reverseOrder());

        List<WarehouseStock> candidates = new ArrayList<>(warehouses);
        List<WarehouseAllocation> allocations = new ArrayList<>();

        // At each iteration, get the best warehouse and missing map is shrinked
        // Since missing is shrinked, so must sort and get the best warehouse using Collections.max() at each iteration.
        while (!missing.isEmpty()) {
            WarehouseStock best = Collections.max(candidates, preference);
            candidates.remove(best);
            allocations.add(
                new WarehouseAllocation(
                    best.warehouseId(),
                    take(best, missing) // also shrinks missing, which make the loop end
                )
            );
        }
        allocations.sort(Comparator.comparing(allocation -> allocation.warehouseId().value()));
        return allocations;
    }

    private static void ensureEnoughStock(Map<VariantId, Integer> wanted, List<WarehouseStock> warehouses) {
        for (Map.Entry<VariantId, Integer> entry : wanted.entrySet()) {
            int available = warehouses
                .stream()
                .mapToInt(warehouse -> warehouse.availableOf(entry.getKey()))
                .sum();
            if (available < entry.getValue()) {
                throw new OutOfStockException(entry.getKey(), entry.getValue(), available);
            }
        }
    }

    /** Get the quantity stocks warehouse can provide */
    private static int quantitiesCovered(WarehouseStock warehouse, Map<VariantId, Integer> missing) {
        int quantities = 0;
        for (Map.Entry<VariantId, Integer> entry : missing.entrySet()) {
            quantities += Math.min(entry.getValue(), warehouse.availableOf(entry.getKey()));
        }
        return quantities;
    }

    /** Takes what the warehouse has of each missing variant, and lowers or removes it from {@code missing}. */
    private static List<ReservationLine> take(WarehouseStock warehouse, Map<VariantId, Integer> missing) {
        List<ReservationLine> lines = new ArrayList<>();
        Iterator<Map.Entry<VariantId, Integer>> it = missing.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<VariantId, Integer> entry = it.next();

            // taken is the quantity warehouse can provide.
            int taken = Math.min(entry.getValue(), warehouse.availableOf(entry.getKey()));

            // Nothing to provide, continue
            if (taken == 0) {
                continue;
            }

            lines.add(new ReservationLine(entry.getKey(), taken));

            if (taken == entry.getValue()) {
                // enough, remove from missing
                it.remove();
            } else {
                // not enough, minus the quantity taken
                entry.setValue(entry.getValue() - taken);
            }
        }
        return lines;
    }
}
