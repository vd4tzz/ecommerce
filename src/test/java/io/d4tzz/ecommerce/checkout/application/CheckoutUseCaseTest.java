package io.d4tzz.ecommerce.checkout.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.catalog.published.SellableVariantFinder;
import io.d4tzz.ecommerce.catalog.published.SellableVariantFinder.SellableVariant;
import io.d4tzz.ecommerce.checkout.application.command.CheckoutCommand;
import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.inventory.application.AllocateStockUseCase;
import io.d4tzz.ecommerce.inventory.application.ReserveStockUseCase;
import io.d4tzz.ecommerce.inventory.application.command.AllocateStockCommand;
import io.d4tzz.ecommerce.inventory.application.command.ReserveStockCommand;
import io.d4tzz.ecommerce.inventory.application.result.StockAllocation;
import io.d4tzz.ecommerce.inventory.domain.OutOfStockException;
import io.d4tzz.ecommerce.order.application.PlaceOrderUseCase;
import io.d4tzz.ecommerce.order.application.command.PlaceOrderCommand;
import io.d4tzz.ecommerce.order.application.command.PlaceOrderCommand.PaymentMode;
import io.d4tzz.ecommerce.shipping.application.ShippingFeeCalculator;
import io.d4tzz.ecommerce.shipping.application.ShippingFeeInfo;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class CheckoutUseCaseTest {

    private static final long USER = 7L;
    private static final long ORDER_ID = 100L;
    private static final long SHIRT = 1L;
    private static final long HAT = 2L;

    private SellableVariantFinder variantFinder;
    private AllocateStockUseCase allocateStock;
    private ShippingFeeCalculator shippingFee;
    private PlaceOrderUseCase placeOrder;
    private ReserveStockUseCase reserveStock;
    private TransactionTemplate transactionTemplate;
    private CheckoutUseCase checkout;

    /** True only while the callback of the transaction template runs. */
    private final AtomicBoolean inTransaction = new AtomicBoolean();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        variantFinder = mock(SellableVariantFinder.class);
        allocateStock = mock(AllocateStockUseCase.class);
        shippingFee = mock(ShippingFeeCalculator.class);
        placeOrder = mock(PlaceOrderUseCase.class);
        reserveStock = mock(ReserveStockUseCase.class);
        transactionTemplate = mock(TransactionTemplate.class);
        // Run the callback directly; a RuntimeException from it propagates like a rolled back transaction
        when(transactionTemplate.execute(any())).thenAnswer(call -> {
            inTransaction.set(true);
            try {
                return ((TransactionCallback<Object>) call.getArgument(0)).doInTransaction(null);
            } finally {
                inTransaction.set(false);
            }
        });
        checkout = new CheckoutUseCase(
            variantFinder,
            allocateStock,
            shippingFee,
            placeOrder,
            reserveStock,
            transactionTemplate
        );

        when(variantFinder.findSellable(any())).thenReturn(
            Map.of(
                VariantId.of(SHIRT),
                snapshot(SHIRT, "Shirt", 100_000),
                VariantId.of(HAT),
                snapshot(HAT, "Hat", 50_000)
            )
        );
        when(allocateStock.execute(any())).thenReturn(
            List.of(
                new StockAllocation(1L, List.of(new StockAllocation.Line(SHIRT, 2), new StockAllocation.Line(HAT, 1)))
            )
        );
        when(shippingFee.calculateFee(any())).thenReturn(Money.of(30_000));
        when(placeOrder.execute(any())).thenReturn(ORDER_ID);
    }

    // ---- fixtures ----

    private static SellableVariant snapshot(long id, String name, long price) {
        return new SellableVariant(
            id * 10,
            name,
            name.toUpperCase() + "-1",
            "Red / S",
            "http://cdn/" + id,
            BigDecimal.valueOf(price)
        );
    }

    private static CheckoutCommand command(PaymentMode mode, CheckoutCommand.Item... items) {
        return new CheckoutCommand(
            USER,
            "Jane Doe",
            "0901234567",
            "Ha Noi",
            "Cau Giay",
            "Dich Vong",
            "1 Xuan Thuy",
            "GHN",
            List.of(items),
            mode,
            "leave at the door"
        );
    }

    private static CheckoutCommand.Item item(long variantId, int quantity) {
        return new CheckoutCommand.Item(variantId, quantity);
    }

    private CheckoutCommand shirtAndHat(PaymentMode mode) {
        return command(mode, item(SHIRT, 2), item(HAT, 1));
    }

    private PlaceOrderCommand placed() {
        ArgumentCaptor<PlaceOrderCommand> captor = ArgumentCaptor.forClass(PlaceOrderCommand.class);
        verify(placeOrder).execute(captor.capture());
        return captor.getValue();
    }

    // ---- the order ----

    @Test
    void returnsTheIdOfThePlacedOrder() {
        assertEquals(ORDER_ID, checkout.execute(shirtAndHat(PaymentMode.COD)));
    }

    @Test
    void theOrderCopiesRecipientAddressAndNoteFromTheRequest() {
        checkout.execute(shirtAndHat(PaymentMode.COD));

        PlaceOrderCommand order = placed();
        assertEquals(USER, order.userId());
        assertEquals("Jane Doe", order.recipientName());
        assertEquals("0901234567", order.recipientPhone());
        assertEquals("Ha Noi", order.province());
        assertEquals("Cau Giay", order.district());
        assertEquals("Dich Vong", order.ward());
        assertEquals("1 Xuan Thuy", order.detailAddress());
        assertEquals("leave at the door", order.note());
    }

    @Test
    void theOrderLinesTakeNameSkuAndPriceFromTheCatalog() {
        checkout.execute(shirtAndHat(PaymentMode.COD));

        List<PlaceOrderCommand.Item> items = placed().items();
        assertEquals(2, items.size());
        PlaceOrderCommand.Item shirt = items.get(0);
        assertEquals(SHIRT, shirt.variantId());
        assertEquals(SHIRT * 10, shirt.productId());
        assertEquals("Shirt", shirt.productName());
        assertEquals("SHIRT-1", shirt.sku());
        assertEquals("Red / S", shirt.optionsDescription());
        assertEquals("http://cdn/1", shirt.thumbnailUrl());
        assertEquals(0, BigDecimal.valueOf(100_000).compareTo(shirt.unitPrice()));
        assertEquals(2, shirt.quantity());
        assertEquals(HAT, items.get(1).variantId());
    }

    @Test
    void theSameVariantTwiceBecomesOneLineWithTheQuantitiesAdded() {
        checkout.execute(command(PaymentMode.COD, item(SHIRT, 2), item(SHIRT, 3)));

        List<PlaceOrderCommand.Item> items = placed().items();
        assertEquals(1, items.size());
        assertEquals(5, items.get(0).quantity());
        ArgumentCaptor<AllocateStockCommand> allocation = ArgumentCaptor.forClass(AllocateStockCommand.class);
        verify(allocateStock).execute(allocation.capture());
        assertEquals(List.of(new AllocateStockCommand.Line(SHIRT, 5)), allocation.getValue().lines());
    }

    @Test
    void theOrderUsesTheShippingFeeFromTheCalculator() {
        checkout.execute(shirtAndHat(PaymentMode.COD));

        assertEquals(0, BigDecimal.valueOf(30_000).compareTo(placed().shippingFee()));
    }

    // ---- payment ----

    @Test
    void aCashOnDeliveryOrderHasNoPaymentDeadline() {
        checkout.execute(shirtAndHat(PaymentMode.COD));

        PlaceOrderCommand order = placed();
        assertEquals(PaymentMode.COD, order.paymentMode());
        assertNull(order.paymentExpiresAt());
    }

    @Test
    void aPrepaidOrderMustBePaidWithinTheWindow() {
        Instant before = Instant.now();

        checkout.execute(shirtAndHat(PaymentMode.PREPAID));

        Instant after = Instant.now();
        PlaceOrderCommand order = placed();
        assertEquals(PaymentMode.PREPAID, order.paymentMode());
        assertEquals(Duration.ofMinutes(15), CheckoutUseCase.PAYMENT_WINDOW);
        assertFalse(order.paymentExpiresAt().isBefore(before.plus(CheckoutUseCase.PAYMENT_WINDOW)));
        assertFalse(order.paymentExpiresAt().isAfter(after.plus(CheckoutUseCase.PAYMENT_WINDOW)));
    }

    // ---- shipping ----

    @Test
    void shippingIsPricedWithTheCarrierDestinationAndOnePackagePerWarehouse() {
        when(allocateStock.execute(any())).thenReturn(
            List.of(
                new StockAllocation(1L, List.of(new StockAllocation.Line(SHIRT, 2))),
                new StockAllocation(2L, List.of(new StockAllocation.Line(HAT, 1)))
            )
        );

        checkout.execute(shirtAndHat(PaymentMode.COD));

        ArgumentCaptor<ShippingFeeInfo> info = ArgumentCaptor.forClass(ShippingFeeInfo.class);
        verify(shippingFee).calculateFee(info.capture());
        assertEquals("GHN", info.getValue().carrier());
        assertEquals(Address.of("Ha Noi", "Cau Giay", "Dich Vong", "1 Xuan Thuy"), info.getValue().destination());
        assertEquals(
            List.of(
                new ShippingFeeInfo.Package(1L, List.of(new ShippingFeeInfo.Item(SHIRT, 2))),
                new ShippingFeeInfo.Package(2L, List.of(new ShippingFeeInfo.Item(HAT, 1)))
            ),
            info.getValue().packages()
        );
    }

    @Test
    void theDestinationProvinceIsGivenToTheWarehouseChoice() {
        checkout.execute(shirtAndHat(PaymentMode.COD));

        ArgumentCaptor<AllocateStockCommand> allocation = ArgumentCaptor.forClass(AllocateStockCommand.class);
        verify(allocateStock).execute(allocation.capture());
        assertEquals("Ha Noi", allocation.getValue().province());
    }

    // ---- holding the stock ----

    @Test
    void theStockIsHeldOncePerWarehouseForTheNewOrderInTheOrderGiven() {
        when(allocateStock.execute(any())).thenReturn(
            List.of(
                new StockAllocation(1L, List.of(new StockAllocation.Line(SHIRT, 2))),
                new StockAllocation(2L, List.of(new StockAllocation.Line(HAT, 1)))
            )
        );

        checkout.execute(shirtAndHat(PaymentMode.COD));

        var inOrder = inOrder(placeOrder, reserveStock);
        inOrder.verify(placeOrder).execute(any());
        inOrder
            .verify(reserveStock)
            .execute(new ReserveStockCommand(ORDER_ID, 1L, List.of(new ReserveStockCommand.Line(SHIRT, 2))));
        inOrder
            .verify(reserveStock)
            .execute(new ReserveStockCommand(ORDER_ID, 2L, List.of(new ReserveStockCommand.Line(HAT, 1))));
    }

    @Test
    void placingTheOrderAndHoldingTheStockShareOneTransaction() {
        AtomicBoolean placedInTransaction = new AtomicBoolean();
        AtomicBoolean reservedInTransaction = new AtomicBoolean();
        when(placeOrder.execute(any())).thenAnswer(call -> {
            placedInTransaction.set(inTransaction.get());
            return ORDER_ID;
        });
        doAnswer(call -> {
            reservedInTransaction.set(inTransaction.get());
            return null;
        })
            .when(reserveStock)
            .execute(any());

        checkout.execute(shirtAndHat(PaymentMode.COD));

        assertTrue(placedInTransaction.get());
        assertTrue(reservedInTransaction.get());
        verify(transactionTemplate, times(1)).execute(any());
    }

    @Test
    void readingAndPricingRunOutsideTheTransaction() {
        AtomicBoolean readInTransaction = new AtomicBoolean(true);
        AtomicBoolean allocatedInTransaction = new AtomicBoolean(true);
        AtomicBoolean pricedInTransaction = new AtomicBoolean(true);
        when(variantFinder.findSellable(any())).thenAnswer(call -> {
            readInTransaction.set(inTransaction.get());
            return Map.of(
                VariantId.of(SHIRT),
                snapshot(SHIRT, "Shirt", 100_000),
                VariantId.of(HAT),
                snapshot(HAT, "Hat", 50_000)
            );
        });
        when(allocateStock.execute(any())).thenAnswer(call -> {
            allocatedInTransaction.set(inTransaction.get());
            return List.of(
                new StockAllocation(1L, List.of(new StockAllocation.Line(SHIRT, 2), new StockAllocation.Line(HAT, 1)))
            );
        });
        when(shippingFee.calculateFee(any())).thenAnswer(call -> {
            pricedInTransaction.set(inTransaction.get());
            return Money.of(30_000);
        });

        checkout.execute(shirtAndHat(PaymentMode.COD));

        assertFalse(readInTransaction.get());
        assertFalse(allocatedInTransaction.get());
        assertFalse(pricedInTransaction.get());
    }

    // ---- failures ----

    @Test
    void aVariantThatIsNotForSaleFailsBeforeAnythingElseHappens() {
        when(variantFinder.findSellable(any())).thenReturn(
            Map.of(VariantId.of(SHIRT), snapshot(SHIRT, "Shirt", 100_000))
        );

        var thrown = assertThrows(InvalidOperationException.class, () ->
            checkout.execute(shirtAndHat(PaymentMode.COD))
        );

        assertTrue(thrown.getMessage().contains("not for sale"));
        verifyNoInteractions(allocateStock, shippingFee, placeOrder, reserveStock, transactionTemplate);
    }

    @Test
    void notEnoughStockPlacesNothing() {
        when(allocateStock.execute(any())).thenThrow(new OutOfStockException(VariantId.of(SHIRT), 2, 1));

        assertThrows(OutOfStockException.class, () -> checkout.execute(shirtAndHat(PaymentMode.COD)));

        verifyNoInteractions(shippingFee, placeOrder, reserveStock, transactionTemplate);
    }

    @Test
    void aShippingFailurePlacesNothing() {
        when(shippingFee.calculateFee(any())).thenThrow(new InvalidOperationException("Unknown carrier: GHN"));

        assertThrows(InvalidOperationException.class, () -> checkout.execute(shirtAndHat(PaymentMode.COD)));

        verifyNoInteractions(placeOrder, reserveStock, transactionTemplate);
    }

    @Test
    void aFailedHoldPropagatesAndStopsHoldingTheOtherWarehouses() {
        when(allocateStock.execute(any())).thenReturn(
            List.of(
                new StockAllocation(1L, List.of(new StockAllocation.Line(SHIRT, 2))),
                new StockAllocation(2L, List.of(new StockAllocation.Line(HAT, 1)))
            )
        );
        doThrow(new OutOfStockException(VariantId.of(SHIRT), 2, 0))
            .when(reserveStock)
            .execute(argThat(command -> command.warehouseId() == 1L));

        assertThrows(OutOfStockException.class, () -> checkout.execute(shirtAndHat(PaymentMode.COD)));

        verify(reserveStock, never()).execute(argThat(command -> command.warehouseId() == 2L));
    }
}
