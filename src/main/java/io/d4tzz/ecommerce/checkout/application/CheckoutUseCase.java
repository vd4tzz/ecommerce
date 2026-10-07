package io.d4tzz.ecommerce.checkout.application;

import io.d4tzz.ecommerce.checkout.application.command.CheckoutCommand;
import io.d4tzz.ecommerce.checkout.application.port.VariantCatalog;
import io.d4tzz.ecommerce.checkout.application.port.VariantCatalog.VariantSnapshot;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import io.d4tzz.ecommerce.domain.user.Address;
import io.d4tzz.ecommerce.inventory.application.AllocateStockUseCase;
import io.d4tzz.ecommerce.inventory.application.ReserveStockUseCase;
import io.d4tzz.ecommerce.inventory.application.command.AllocateStockCommand;
import io.d4tzz.ecommerce.inventory.application.command.ReserveStockCommand;
import io.d4tzz.ecommerce.inventory.application.result.StockAllocation;
import io.d4tzz.ecommerce.order.application.PlaceOrderUseCase;
import io.d4tzz.ecommerce.order.application.command.PlaceOrderCommand;
import io.d4tzz.ecommerce.shipping.application.ShippingFeeCalculator;
import io.d4tzz.ecommerce.shipping.application.ShippingFeeInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Turns what the customer picked into an order:
 * <ol>
 *   <li>Read price and details of each variant from the catalog. The client only sends ids and quantities.
 *   <li>Pick the warehouses that send it.
 *   <li>Price shipping, one package per warehouse.
 *   <li>Place the order and hold its stock in one transaction: if the stock was taken since step 2, the order
 *       is rolled back.
 * </ol>
 * Steps 1-3 only read, so they run outside that transaction and a slow carrier does not hold a connection.
 * A prepaid order must be paid within {@link #PAYMENT_WINDOW}, or it is cancelled and its stock released.
 * Returns the new order id.
 */
@Service
public class CheckoutUseCase {

    static final Duration PAYMENT_WINDOW = Duration.ofMinutes(15);

    private final VariantCatalog variantCatalog;
    private final AllocateStockUseCase allocateStockUseCase;
    private final ShippingFeeCalculator shippingFeeCalculator;
    private final PlaceOrderUseCase placeOrderUseCase;
    private final ReserveStockUseCase reserveStockUseCase;
    private final TransactionTemplate transactionTemplate;

    public CheckoutUseCase(
        VariantCatalog variantCatalog,
        AllocateStockUseCase allocateStockUseCase,
        ShippingFeeCalculator shippingFeeCalculator,
        PlaceOrderUseCase placeOrderUseCase,
        ReserveStockUseCase reserveStockUseCase,
        TransactionTemplate transactionTemplate
    ) {
        this.variantCatalog = variantCatalog;
        this.allocateStockUseCase = allocateStockUseCase;
        this.shippingFeeCalculator = shippingFeeCalculator;
        this.placeOrderUseCase = placeOrderUseCase;
        this.reserveStockUseCase = reserveStockUseCase;
        this.transactionTemplate = transactionTemplate;
    }

    public Long execute(CheckoutCommand command) {
        // The same variant twice becomes one order line
        Map<VariantId, Integer> quantities = new LinkedHashMap<>();
        for (CheckoutCommand.Item item : command.items()) {
            quantities.merge(VariantId.of(item.variantId()), item.quantity(), Integer::sum);
        }

        Map<VariantId, VariantSnapshot> snapshots = variantCatalog.findForSale(quantities.keySet());
        for (VariantId variantId : quantities.keySet()) {
            if (!snapshots.containsKey(variantId)) {
                throw new InvalidOperationException("Variant " + variantId + " is not for sale");
            }
        }

        List<StockAllocation> allocations = allocateStockUseCase.execute(
            new AllocateStockCommand(
                command.province(),
                quantities
                    .entrySet()
                    .stream()
                    .map(entry -> new AllocateStockCommand.Line(entry.getKey().value(), entry.getValue()))
                    .toList()
            )
        );

        Address destination = Address.of(
            command.province(),
            command.district(),
            command.ward(),
            command.detailAddress()
        );
        Money shippingFee = shippingFeeCalculator.calculateFee(
            new ShippingFeeInfo(
                destination,
                command.carrier(),
                allocations.stream().map(CheckoutUseCase::toPackage).toList()
            )
        );

        PlaceOrderCommand placeOrderCmd = new PlaceOrderCommand(
            command.userId(),
            command.recipientName(),
            command.recipientPhone(),
            destination.province(),
            destination.district(),
            destination.ward(),
            destination.detailAddress(),
            quantities
                .entrySet()
                .stream()
                .map(entry -> toOrderItem(entry.getKey(), entry.getValue(), snapshots.get(entry.getKey())))
                .toList(),
            shippingFee.amount(),
            command.paymentMode(),
            command.paymentMode() == PlaceOrderCommand.PaymentMode.PREPAID ? Instant.now().plus(PAYMENT_WINDOW) : null,
            command.note()
        );

        return transactionTemplate.execute(status -> {
            Long orderId = placeOrderUseCase.execute(placeOrderCmd);
            for (StockAllocation allocation : allocations) {
                reserveStockUseCase.execute(toReservation(orderId, allocation));
            }
            return orderId;
        });
    }

    private static ShippingFeeInfo.Package toPackage(StockAllocation allocation) {
        return new ShippingFeeInfo.Package(
            allocation.warehouseId(),
            allocation
                .lines()
                .stream()
                .map(line -> new ShippingFeeInfo.Item(line.variantId(), line.quantity()))
                .toList()
        );
    }

    private static ReserveStockCommand toReservation(Long orderId, StockAllocation allocation) {
        return new ReserveStockCommand(
            orderId,
            allocation.warehouseId(),
            allocation
                .lines()
                .stream()
                .map(line -> new ReserveStockCommand.Line(line.variantId(), line.quantity()))
                .toList()
        );
    }

    private static PlaceOrderCommand.Item toOrderItem(VariantId variantId, int quantity, VariantSnapshot snapshot) {
        return new PlaceOrderCommand.Item(
            variantId.value(),
            snapshot.productId(),
            snapshot.productName(),
            snapshot.sku(),
            snapshot.optionsDescription(),
            snapshot.thumbnailUrl(),
            snapshot.price(),
            quantity
        );
    }
}
