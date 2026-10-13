package io.d4tzz.ecommerce.checkout.application;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.catalog.published.SellableVariantFinder;
import io.d4tzz.ecommerce.catalog.published.SellableVariantFinder.SellableVariant;
import io.d4tzz.ecommerce.checkout.application.command.CheckoutCommand;
import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.inventory.application.AllocateStockUseCase;
import io.d4tzz.ecommerce.inventory.application.ReserveStockUseCase;
import io.d4tzz.ecommerce.inventory.application.command.AllocateStockCommand;
import io.d4tzz.ecommerce.inventory.application.command.ReserveStockCommand;
import io.d4tzz.ecommerce.inventory.application.result.StockAllocation;
import io.d4tzz.ecommerce.inventory.domain.OutOfStockException;
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

@Service
public class CheckoutUseCase {

    /** How long a prepaid order has to be paid before it is cancelled. */
    static final Duration PAYMENT_WINDOW = Duration.ofMinutes(15);

    private final SellableVariantFinder sellableVariantFinder;
    private final AllocateStockUseCase allocateStockUseCase;
    private final ShippingFeeCalculator shippingFeeCalculator;
    private final PlaceOrderUseCase placeOrderUseCase;
    private final ReserveStockUseCase reserveStockUseCase;
    private final TransactionTemplate transactionTemplate;

    public CheckoutUseCase(
        SellableVariantFinder sellableVariantFinder,
        AllocateStockUseCase allocateStockUseCase,
        ShippingFeeCalculator shippingFeeCalculator,
        PlaceOrderUseCase placeOrderUseCase,
        ReserveStockUseCase reserveStockUseCase,
        TransactionTemplate transactionTemplate
    ) {
        this.sellableVariantFinder = sellableVariantFinder;
        this.allocateStockUseCase = allocateStockUseCase;
        this.shippingFeeCalculator = shippingFeeCalculator;
        this.placeOrderUseCase = placeOrderUseCase;
        this.reserveStockUseCase = reserveStockUseCase;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Turns what the customer picked into an order:
     * <ol>
     *   <li>Read price and details of each variant from the catalog. The client only sends ids and quantities, and
     *       the same variant listed twice becomes one order line.
     *   <li>Pick the warehouses that send it.
     *   <li>Price shipping, one package per warehouse.
     *   <li>Place the order and hold its stock in one transaction: if the stock was taken since step 2, the order
     *       is rolled back.
     * </ol>
     * Steps 1-3 only read, so they run outside that transaction and a slow carrier does not hold a connection.
     * A prepaid order must be paid within {@link #PAYMENT_WINDOW}, or it is cancelled and its stock released.
     *
     * @return the id of the new order
     * @throws InvalidOperationException if a variant is not for sale, the carrier is unknown, or a warehouse or the
     *                                   size and weight of a variant is not known to shipping
     * @throws OutOfStockException       if a variant has too little stock to allocate, or the stock was taken before
     *                                   it could be held
     * @throws InvalidArgumentException  if the recipient, phone number or address is invalid
     */
    public Long execute(CheckoutCommand command) {
        // Steps 1-3 only read, so they run before the transaction opens
        Map<VariantId, Integer> quantities = mergeQuantities(command.items());
        Map<VariantId, SellableVariant> snapshots = findSellable(quantities);

        List<StockAllocation> allocations = allocateStock(command.province(), quantities);
        Address destination = Address.of(
            command.province(),
            command.district(),
            command.ward(),
            command.detailAddress()
        );
        Money shippingFee = priceShipping(command.carrier(), destination, allocations);

        PlaceOrderCommand order = toPlaceOrderCommand(command, destination, quantities, snapshots, shippingFee);

        return transactionTemplate.execute(status -> {
            Long orderId = placeOrderUseCase.execute(order);
            for (StockAllocation allocation : allocations) {
                reserveStockUseCase.execute(toReservation(orderId, allocation));
            }
            return orderId;
        });
    }

    /** The same variant twice becomes one order line. */
    private static Map<VariantId, Integer> mergeQuantities(List<CheckoutCommand.Item> items) {
        Map<VariantId, Integer> quantities = new LinkedHashMap<>();
        for (CheckoutCommand.Item item : items) {
            quantities.merge(VariantId.of(item.variantId()), item.quantity(), Integer::sum);
        }
        return quantities;
    }

    private Map<VariantId, SellableVariant> findSellable(Map<VariantId, Integer> quantities) {
        Map<VariantId, SellableVariant> snapshots = sellableVariantFinder.findSellable(quantities.keySet());
        for (VariantId variantId : quantities.keySet()) {
            if (!snapshots.containsKey(variantId)) {
                throw new InvalidOperationException("Variant " + variantId + " is not for sale");
            }
        }
        return snapshots;
    }

    private List<StockAllocation> allocateStock(String province, Map<VariantId, Integer> quantities) {
        return allocateStockUseCase.execute(
            new AllocateStockCommand(
                province,
                quantities
                    .entrySet()
                    .stream()
                    .map(entry -> new AllocateStockCommand.Line(entry.getKey().value(), entry.getValue()))
                    .toList()
            )
        );
    }

    private Money priceShipping(String carrier, Address destination, List<StockAllocation> allocations) {
        return shippingFeeCalculator.calculateFee(
            new ShippingFeeInfo(destination, carrier, allocations.stream().map(CheckoutUseCase::toPackage).toList())
        );
    }

    private static PlaceOrderCommand toPlaceOrderCommand(
        CheckoutCommand command,
        Address destination,
        Map<VariantId, Integer> quantities,
        Map<VariantId, SellableVariant> snapshots,
        Money shippingFee
    ) {
        boolean prepaid = command.paymentMode() == PlaceOrderCommand.PaymentMode.PREPAID;
        return new PlaceOrderCommand(
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
            prepaid ? Instant.now().plus(PAYMENT_WINDOW) : null,
            command.note()
        );
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

    private static PlaceOrderCommand.Item toOrderItem(VariantId variantId, int quantity, SellableVariant snapshot) {
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
