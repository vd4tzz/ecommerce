package io.d4tzz.ecommerce.order.infrastructure;

import static io.d4tzz.ecommerce.order.domain.OrderFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.order.domain.MarkPaidResult;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderCode;
import io.d4tzz.ecommerce.order.domain.OrderId;
import io.d4tzz.ecommerce.order.domain.OrderItem;
import io.d4tzz.ecommerce.order.domain.OrderStatus;
import io.d4tzz.ecommerce.order.domain.PaymentStatus;
import io.d4tzz.ecommerce.order.infrastructure.persistence.repository.OrderRepositoryImpl;
import io.d4tzz.ecommerce.support.PostgresContainerTest;
import io.d4tzz.ecommerce.user.domain.UserId;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(OrderRepositoryImpl.class)
class OrderRepositoryImplTest extends PostgresContainerTest {

    // Postgres keeps microseconds; Instant.now() can carry nanoseconds
    private static final Instant NOW_DB = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired OrderRepositoryImpl repository;
    @Autowired EntityManager em;

    private Order newOrder(String code, Instant paymentExpiresAt, Instant now) {
        List<OrderItem> items = List.of(item(100_000, 2));
        if (paymentExpiresAt == null) {
            return Order.createCashOnDelivery(
                OrderCode.of(code), UserId.of(USER_ID), recipient(), address(), items, SHIPPING_FEE, null, now
            );
        }
        return Order.createPrepaid(
            OrderCode.of(code), UserId.of(USER_ID), recipient(), address(), items, SHIPPING_FEE, null, paymentExpiresAt, now
        );
    }

    /** Writes pending changes and forgets them, so the next read comes from the database. */
    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    void save_newOrderGetsIdAndRoundTripsEveryField() {
        List<OrderItem> items = List.of(
            OrderItem.create(VariantId.of(1L), ProductId.of(10L), "Shirt", "SKU-1", "M / Red", "http://img/1", Money.of(100_000), 2),
            OrderItem.create(VariantId.of(2L), ProductId.of(20L), "Hat", "SKU-2", "", null, Money.of(50_000), 1)
        );
        Order order = Order.createPrepaid(
            OrderCode.of("ORD1"), UserId.of(USER_ID), recipient(), address(), items, SHIPPING_FEE, "  call first ",
            NOW_DB.plusSeconds(900), NOW_DB
        );

        Order saved = repository.save(order);
        flushAndClear();
        Order loaded = repository.find(saved.getId()).orElseThrow();

        assertNotNull(saved.getId());
        assertEquals("ORD1", loaded.getOrderCode().value());
        assertEquals(USER_ID, loaded.getUserId().value());
        assertEquals("Jane Doe", loaded.getRecipient().fullName());
        assertEquals("0901234567", loaded.getRecipient().phoneNumber().value());
        assertEquals(address(), loaded.getShippingAddress());
        assertEquals(Money.of(250_000), loaded.getSubtotal());
        assertEquals(SHIPPING_FEE, loaded.getShippingFee());
        assertEquals(Money.of(280_000), loaded.getTotalAmount());
        assertEquals(OrderStatus.PENDING, loaded.getStatus());
        assertEquals(PaymentStatus.UNPAID, loaded.getPaymentStatus());
        assertEquals("call first", loaded.getNote());
        assertEquals(NOW_DB.plusSeconds(900), loaded.getPaymentExpiresAt());
        assertEquals(NOW_DB, loaded.getCreatedAt());
        assertNull(loaded.getPaidAt());
        assertNull(loaded.getPaymentReference());
        assertNull(loaded.getCancelReason());
    }

    @Test
    void save_keepsItemsWithTheirSnapshotAndInOrder() {
        List<OrderItem> items = List.of(
            OrderItem.create(VariantId.of(1L), ProductId.of(10L), "Shirt", "SKU-1", "M / Red", "http://img/1", Money.of(100_000), 2),
            OrderItem.create(VariantId.of(2L), ProductId.of(20L), "Hat", "SKU-2", "", null, Money.of(50_000), 1)
        );
        Order saved = repository.save(
            Order.createCashOnDelivery(
                OrderCode.of("ORD1"), UserId.of(USER_ID), recipient(), address(), items, SHIPPING_FEE, null, NOW_DB
            )
        );
        flushAndClear();

        List<OrderItem> loaded = repository.find(saved.getId()).orElseThrow().getItems();

        assertEquals(2, loaded.size());
        OrderItem shirt = loaded.get(0);
        assertNotNull(shirt.getId());
        assertEquals(1L, shirt.getVariantId().value());
        assertEquals(10L, shirt.getProductId().value());
        assertEquals("Shirt", shirt.getProductName());
        assertEquals("SKU-1", shirt.getSku());
        assertEquals("M / Red", shirt.getAttributesDescription());
        assertEquals("http://img/1", shirt.getThumbnailUrl());
        assertEquals(Money.of(100_000), shirt.getUnitPrice());
        assertEquals(2, shirt.getQuantity());
        assertEquals(Money.of(200_000), shirt.getTotalPrice());
        assertEquals("Hat", loaded.get(1).getProductName());
        assertNull(loaded.get(1).getThumbnailUrl());
    }

    @Test
    void save_codOrderHasNoPaymentDeadline() {
        Order saved = repository.save(newOrder("ORD1", null, NOW_DB));
        flushAndClear();

        Order loaded = repository.find(saved.getId()).orElseThrow();

        assertNull(loaded.getPaymentExpiresAt());
        assertFalse(loaded.requiresPrepayment());
    }

    @Test
    void save_existingOrderStoresLifecycleChanges() {
        Order saved = repository.save(newOrder("ORD1", NOW_DB.plusSeconds(900), NOW_DB));
        flushAndClear();

        Order order = repository.find(saved.getId()).orElseThrow();
        assertEquals(MarkPaidResult.ACCEPTED, order.markPaid("55", order.getTotalAmount(), NOW_DB));
        order.confirm(NOW_DB.plusSeconds(60));
        order.markShipping(NOW_DB.plusSeconds(120));
        order.markDelivered(NOW_DB.plusSeconds(180));
        repository.save(order);
        flushAndClear();

        Order loaded = repository.find(saved.getId()).orElseThrow();
        assertEquals(OrderStatus.DELIVERED, loaded.getStatus());
        assertEquals(PaymentStatus.PAID, loaded.getPaymentStatus());
        assertEquals("55", loaded.getPaymentReference());
        assertEquals(NOW_DB, loaded.getPaidAt());
        assertEquals(NOW_DB.plusSeconds(180), loaded.getCompletedAt());
        assertEquals(NOW_DB.plusSeconds(180), loaded.getUpdatedAt());
        assertEquals(1, loaded.getItems().size());
    }

    @Test
    void save_cancelledOrderStoresReasonAndTime() {
        Order saved = repository.save(newOrder("ORD1", null, NOW_DB));
        flushAndClear();

        Order order = repository.find(saved.getId()).orElseThrow();
        order.cancel("Out of stock", NOW_DB.plusSeconds(60));
        repository.save(order);
        flushAndClear();

        Order loaded = repository.find(saved.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, loaded.getStatus());
        assertEquals("Out of stock", loaded.getCancelReason());
        assertEquals(NOW_DB.plusSeconds(60), loaded.getCancelledAt());
    }

    @Test
    void save_duplicateOrderCodeViolatesUniqueConstraint() {
        repository.save(newOrder("ORD1", null, NOW_DB));
        em.flush();

        // em.flush() is not behind a Spring repository proxy, so Hibernate's own exception comes out
        ConstraintViolationException e = assertThrows(ConstraintViolationException.class, () -> {
            repository.save(newOrder("ORD1", null, NOW_DB));
            em.flush();
        });
        assertEquals("uk_orders_order_code", e.getConstraintName());
    }

    // ---- finders ----

    @Test
    void find_unknownIdIsEmpty() {
        assertTrue(repository.find(OrderId.of(999_999L)).isEmpty());
        assertTrue(repository.findForUpdate(OrderId.of(999_999L)).isEmpty());
    }

    @Test
    void findByOrderCode_matchesTheStoredCode() {
        Order saved = repository.save(newOrder("ORDX", null, NOW_DB));
        flushAndClear();

        assertEquals(saved.getId(), repository.findByOrderCode(OrderCode.of(" ordx ")).orElseThrow().getId());
        assertTrue(repository.findByOrderCode(OrderCode.of("OTHER")).isEmpty());
    }

    @Test
    void findForUpdate_loadsOrderByIdAndByCode() {
        Order saved = repository.save(newOrder("ORDX", null, NOW_DB));
        flushAndClear();

        assertEquals("ORDX", repository.findForUpdate(saved.getId()).orElseThrow().getOrderCode().value());
        assertEquals(saved.getId(), repository.findForUpdateByOrderCode(OrderCode.of("ORDX")).orElseThrow().getId());
        assertTrue(repository.findForUpdateByOrderCode(OrderCode.of("OTHER")).isEmpty());
    }

    @Test
    void delete_removesOrderWithItsItems() {
        Order saved = repository.save(newOrder("ORD1", null, NOW_DB));
        flushAndClear();

        repository.delete(saved.getId());
        flushAndClear();

        assertTrue(repository.find(saved.getId()).isEmpty());
        Number items = (Number) em.createNativeQuery("select count(*) from order_items").getSingleResult();
        assertEquals(0, items.intValue());
    }

    // ---- payment expiry search ----

    @Test
    void findPaymentExpiredIds_onlyPendingUnpaidPrepaidOrdersPastTheirDeadline() {
        Instant created = NOW_DB.minusSeconds(3600);
        Instant overdue = created.plusSeconds(900);

        Order expired1 = repository.save(newOrder("EXP1", overdue, created));
        Order stillTime = repository.save(newOrder("TIME", NOW_DB.plusSeconds(900), NOW_DB));
        Order cod = repository.save(newOrder("COD1", null, created));

        Order paid = newOrder("PAID", overdue, created);
        paid.markPaid("55", paid.getTotalAmount(), created.plusSeconds(60));
        paid = repository.save(paid);

        Order cancelled = newOrder("CANC", overdue, created);
        cancelled.cancel("Changed my mind", created.plusSeconds(60));
        cancelled = repository.save(cancelled);

        Order expired2 = repository.save(newOrder("EXP2", overdue, created));
        flushAndClear();

        List<OrderId> ids = repository.findPaymentExpiredIds(NOW_DB);

        assertEquals(List.of(expired1.getId(), expired2.getId()), ids);
        assertFalse(ids.contains(stillTime.getId()));
        assertFalse(ids.contains(cod.getId()));
        assertFalse(ids.contains(paid.getId()));
        assertFalse(ids.contains(cancelled.getId()));
    }

    @Test
    void findPaymentExpiredIds_deadlineExactlyNowIsNotYetExpired() {
        Instant created = NOW_DB.minusSeconds(60);
        repository.save(newOrder("EDGE", NOW_DB, created));
        flushAndClear();

        assertTrue(repository.findPaymentExpiredIds(NOW_DB).isEmpty());
        assertEquals(1, repository.findPaymentExpiredIds(NOW_DB.plusMillis(1)).size());
    }
}
