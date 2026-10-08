package io.d4tzz.ecommerce.order.infrastructure;

import static io.d4tzz.ecommerce.order.domain.OrderFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderCode;
import io.d4tzz.ecommerce.order.domain.Recipient;
import io.d4tzz.ecommerce.order.infrastructure.persistence.repository.OrderRepositoryImpl;
import io.d4tzz.ecommerce.order.infrastructure.query.JdbcOrderQueryService;
import io.d4tzz.ecommerce.order.query.OrderSearchCriteria;
import io.d4tzz.ecommerce.order.query.OrderSummaryView;
import io.d4tzz.ecommerce.order.query.OrderView;
import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.support.PostgresContainerTest;
import io.d4tzz.ecommerce.user.domain.PhoneNumber;
import io.d4tzz.ecommerce.user.domain.UserId;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@DataJpaTest
@Import({ OrderRepositoryImpl.class, JdbcOrderQueryService.class })
class JdbcOrderQueryServiceTest extends PostgresContainerTest {

    // Postgres keeps microseconds; Instant.now() can carry nanoseconds
    private static final Instant T0 = Instant.now().truncatedTo(ChronoUnit.MILLIS).minusSeconds(7200);

    @Autowired
    OrderRepositoryImpl repository;

    @Autowired
    JdbcOrderQueryService queryService;

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    EntityManager em;

    @BeforeEach
    void createUnaccentFunction() {
        // Normally created at startup by PostgresFtsInitializer; DDL is rolled back with each test
        jdbcClient.sql("CREATE EXTENSION IF NOT EXISTS unaccent").update();
        jdbcClient
            .sql(
                """
                CREATE OR REPLACE FUNCTION immutable_unaccent(text)
                RETURNS text AS $$ SELECT public.unaccent($1); $$ LANGUAGE sql IMMUTABLE PARALLEL SAFE
                """
            )
            .update();
    }

    private Order cod(String code, long userId, String recipientName, String phone, Instant createdAt) {
        return repository.save(
            Order.createCashOnDelivery(
                OrderCode.of(code),
                UserId.of(userId),
                new Recipient(recipientName, PhoneNumber.of(phone)),
                Address.of("Ha Noi", "Cau Giay", "Dich Vong", "1 Xuan Thuy"),
                List.of(item(100_000, 2)),
                SHIPPING_FEE,
                "leave at the door",
                createdAt
            )
        );
    }

    private Order prepaid(String code, long userId, Instant createdAt, Instant paymentExpiresAt) {
        return repository.save(
            Order.createPrepaid(
                OrderCode.of(code),
                UserId.of(userId),
                recipient(),
                address(),
                List.of(item(100_000, 2)),
                SHIPPING_FEE,
                null,
                paymentExpiresAt,
                createdAt
            )
        );
    }

    private OrderSearchCriteria criteria(
        Long userId,
        String status,
        String paymentStatus,
        Boolean ready,
        String keyword
    ) {
        return new OrderSearchCriteria(userId, status, paymentStatus, ready, keyword, 0, 20);
    }

    private List<String> codes(PageResponse<OrderSummaryView> page) {
        return page.content().stream().map(OrderSummaryView::orderCode).toList();
    }

    private void flush() {
        em.flush();
        em.clear();
    }

    // ---- single order ----

    @Test
    void getOrder_mapsEveryColumnAndItsItems() {
        Order saved = cod("ORD1", USER_ID, "Jane Doe", "0901234567", T0);
        flush();

        OrderView view = queryService.getOrder(saved.getId().value()).orElseThrow();

        assertEquals("ORD1", view.orderCode());
        assertEquals(USER_ID, view.userId());
        assertEquals("Jane Doe", view.recipientName());
        assertEquals("0901234567", view.recipientPhone());
        assertEquals("Ha Noi", view.province());
        assertEquals("Cau Giay", view.district());
        assertEquals("Dich Vong", view.ward());
        assertEquals("1 Xuan Thuy", view.detailAddress());
        assertEquals(0, view.subtotal().compareTo(SUBTOTAL.amount()));
        assertEquals(0, view.shippingFee().compareTo(SHIPPING_FEE.amount()));
        assertEquals(0, view.totalAmount().compareTo(TOTAL.amount()));
        assertEquals("PENDING", view.status());
        assertEquals("UNPAID", view.paymentStatus());
        assertEquals("leave at the door", view.note());
        assertNull(view.paymentExpiresAt());
        assertFalse(view.paymentExpired());
        assertEquals(T0, view.createdAt());
        assertEquals(1, view.items().size());
        OrderView.Item item = view.items().get(0);
        assertEquals("Shirt", item.productName());
        assertEquals("SKU-1", item.sku());
        assertEquals("M / Red", item.optionsDescription());
        assertEquals(2, item.quantity());
        assertEquals(0, item.totalPrice().compareTo(SUBTOTAL.amount()));
    }

    @Test
    void getOrder_unknownIdIsEmpty() {
        assertTrue(queryService.getOrder(999_999L).isEmpty());
    }

    @Test
    void getOrder_paymentExpiredIsComputedFromTheDeadlineAtReadTime() {
        Order overdue = prepaid("LATE", USER_ID, T0, T0.plusSeconds(900));
        Order open = prepaid(
            "OPEN",
            USER_ID,
            Instant.now().truncatedTo(ChronoUnit.MILLIS),
            Instant.now().plusSeconds(900)
        );
        flush();

        assertTrue(queryService.getOrder(overdue.getId().value()).orElseThrow().paymentExpired());
        assertFalse(queryService.getOrder(open.getId().value()).orElseThrow().paymentExpired());
    }

    @Test
    void getOrder_paidOrCancelledOrderIsNeverReportedAsExpired() {
        Order paid = Order.createPrepaid(
            OrderCode.of("PAID"),
            UserId.of(USER_ID),
            recipient(),
            address(),
            List.of(item(100_000, 2)),
            SHIPPING_FEE,
            null,
            T0.plusSeconds(900),
            T0
        );
        paid.markPaid("55", paid.getTotalAmount(), T0.plusSeconds(60));
        paid = repository.save(paid);
        flush();

        OrderView view = queryService.getOrder(paid.getId().value()).orElseThrow();

        assertEquals("PAID", view.paymentStatus());
        assertFalse(view.paymentExpired());
        assertNotNull(view.paidAt());
    }

    @Test
    void getUserOrder_matchesCodeIgnoringCaseAndOnlyForTheOwner() {
        cod("ORD1", USER_ID, "Jane Doe", "0901234567", T0);
        flush();

        assertTrue(queryService.getUserOrder(USER_ID, " ord1 ").isPresent());
        assertTrue(queryService.getUserOrder(USER_ID + 1, "ORD1").isEmpty());
        assertTrue(queryService.getUserOrder(USER_ID, "NOPE").isEmpty());
    }

    // ---- search ----

    @Test
    void search_newestFirstWithTotalQuantity() {
        cod("OLD", USER_ID, "Jane Doe", "0901234567", T0);
        cod("NEW", USER_ID, "Jane Doe", "0901234567", T0.plusSeconds(60));
        flush();

        PageResponse<OrderSummaryView> page = queryService.searchOrders(criteria(null, null, null, null, null));

        assertEquals(List.of("NEW", "OLD"), codes(page));
        assertEquals(2, page.totalElements());
        assertEquals(2, page.content().get(0).totalQuantity());
        assertEquals(0, page.content().get(0).totalAmount().compareTo(TOTAL.amount()));
    }

    @Test
    void search_filtersByUser() {
        cod("MINE", USER_ID, "Jane Doe", "0901234567", T0);
        cod("THEIRS", USER_ID + 1, "John Roe", "0907654321", T0.plusSeconds(1));
        flush();

        assertEquals(List.of("MINE"), codes(queryService.searchOrders(criteria(USER_ID, null, null, null, null))));
    }

    @Test
    void search_filtersByStatusAndPaymentStatusCaseInsensitively() {
        cod("PEND", USER_ID, "Jane Doe", "0901234567", T0);
        Order cancelled = cod("CANC", USER_ID, "Jane Doe", "0901234567", T0.plusSeconds(1));
        cancelled.cancel("Changed my mind", T0.plusSeconds(2));
        repository.save(cancelled);
        Order paid = Order.createPrepaid(
            OrderCode.of("PAID"),
            UserId.of(USER_ID),
            recipient(),
            address(),
            List.of(item(100_000, 2)),
            SHIPPING_FEE,
            null,
            T0.plusSeconds(900),
            T0.plusSeconds(3)
        );
        paid.markPaid("55", paid.getTotalAmount(), T0.plusSeconds(4));
        repository.save(paid);
        flush();

        assertEquals(
            List.of("CANC"),
            codes(queryService.searchOrders(criteria(null, " cancelled ", null, null, null)))
        );
        assertEquals(List.of("PAID"), codes(queryService.searchOrders(criteria(null, null, "paid", null, null))));
        assertEquals(
            List.of("CANC", "PEND"),
            codes(queryService.searchOrders(criteria(null, null, "UNPAID", null, null)))
        );
    }

    @Test
    void search_readyToConfirmIsPendingCodOrPaidPrepaid() {
        cod("COD", USER_ID, "Jane Doe", "0901234567", T0);
        prepaid("UNPAID", USER_ID, T0.plusSeconds(1), T0.plusSeconds(900));
        Order paid = Order.createPrepaid(
            OrderCode.of("PAID"),
            UserId.of(USER_ID),
            recipient(),
            address(),
            List.of(item(100_000, 2)),
            SHIPPING_FEE,
            null,
            T0.plusSeconds(900),
            T0.plusSeconds(2)
        );
        paid.markPaid("55", paid.getTotalAmount(), T0.plusSeconds(3));
        repository.save(paid);
        Order confirmed = cod("DONE", USER_ID, "Jane Doe", "0901234567", T0.plusSeconds(4));
        confirmed.confirm(T0.plusSeconds(5));
        repository.save(confirmed);
        flush();

        assertEquals(List.of("PAID", "COD"), codes(queryService.searchOrders(criteria(null, null, null, true, null))));
    }

    @Test
    void search_keywordMatchesCodePhoneAndNameIgnoringAccents() {
        cod("ORD111", USER_ID, "Nguyễn Văn An", "0911111111", T0);
        cod("ORD222", USER_ID, "Tran Binh", "0922222222", T0.plusSeconds(1));
        flush();

        assertEquals(List.of("ORD111"), codes(queryService.searchOrders(criteria(null, null, null, null, "ord111"))));
        assertEquals(List.of("ORD222"), codes(queryService.searchOrders(criteria(null, null, null, null, "0922"))));
        assertEquals(
            List.of("ORD111"),
            codes(queryService.searchOrders(criteria(null, null, null, null, "nguyen van")))
        );
        assertEquals(List.of("ORD111"), codes(queryService.searchOrders(criteria(null, null, null, null, "Nguyễn"))));
        assertTrue(
            queryService
                .searchOrders(criteria(null, null, null, null, "zzz"))
                .content()
                .isEmpty()
        );
    }

    @Test
    void search_pagesResults() {
        for (int i = 0; i < 5; i++) {
            cod("ORD" + i, USER_ID, "Jane Doe", "0901234567", T0.plusSeconds(i));
        }
        flush();

        PageResponse<OrderSummaryView> second = queryService.searchOrders(
            new OrderSearchCriteria(null, null, null, null, null, 1, 2)
        );

        assertEquals(List.of("ORD2", "ORD1"), codes(second));
        assertEquals(5, second.totalElements());
        assertEquals(3, second.totalPages());
        assertTrue(second.hasNext());
        assertTrue(second.hasPrevious());
    }

    @Test
    void search_noMatchIsAnEmptyPage() {
        PageResponse<OrderSummaryView> page = queryService.searchOrders(criteria(999L, null, null, null, null));

        assertTrue(page.content().isEmpty());
        assertEquals(0, page.totalElements());
        assertFalse(page.hasNext());
    }
}
