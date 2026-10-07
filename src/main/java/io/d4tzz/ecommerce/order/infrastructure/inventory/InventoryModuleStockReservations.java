package io.d4tzz.ecommerce.order.infrastructure.inventory;

import io.d4tzz.ecommerce.inventory.application.CommitStockUseCase;
import io.d4tzz.ecommerce.inventory.application.ReleaseStockUseCase;
import io.d4tzz.ecommerce.inventory.application.RestockReturnedUseCase;
import io.d4tzz.ecommerce.inventory.application.command.CommitStockCommand;
import io.d4tzz.ecommerce.inventory.application.command.ReleaseStockCommand;
import io.d4tzz.ecommerce.inventory.application.command.RestockReturnedCommand;
import io.d4tzz.ecommerce.order.application.port.StockReservations;
import io.d4tzz.ecommerce.order.domain.OrderId;
import org.springframework.stereotype.Component;

/** Calls the inventory module directly; runs in the caller's transaction. */
@Component
public class InventoryModuleStockReservations implements StockReservations {

    private final ReleaseStockUseCase releaseStockUseCase;
    private final CommitStockUseCase commitStockUseCase;
    private final RestockReturnedUseCase restockReturnedUseCase;

    public InventoryModuleStockReservations(
        ReleaseStockUseCase releaseStockUseCase,
        CommitStockUseCase commitStockUseCase,
        RestockReturnedUseCase restockReturnedUseCase
    ) {
        this.releaseStockUseCase = releaseStockUseCase;
        this.commitStockUseCase = commitStockUseCase;
        this.restockReturnedUseCase = restockReturnedUseCase;
    }

    @Override
    public void release(OrderId orderId) {
        releaseStockUseCase.execute(new ReleaseStockCommand(orderId.value()));
    }

    @Override
    public void commit(OrderId orderId) {
        commitStockUseCase.execute(new CommitStockCommand(orderId.value()));
    }

    @Override
    public void restock(OrderId orderId) {
        restockReturnedUseCase.execute(new RestockReturnedCommand(orderId.value()));
    }
}
