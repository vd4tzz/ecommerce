package io.d4tzz.ecommerce.inventory.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.category.Category;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryName;
import io.d4tzz.ecommerce.catalog.domain.category.CategorySlug;
import io.d4tzz.ecommerce.catalog.domain.product.Description;
import io.d4tzz.ecommerce.catalog.domain.product.Dimensions;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductName;
import io.d4tzz.ecommerce.catalog.domain.product.ProductSlug;
import io.d4tzz.ecommerce.catalog.domain.product.Sku;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.catalog.domain.product.Weight;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.repository.CategoryRepositoryImpl;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.repository.ProductRepositoryImpl;
import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.inventory.domain.InventoryItem;
import io.d4tzz.ecommerce.inventory.domain.ReservationLine;
import io.d4tzz.ecommerce.inventory.domain.ReservationStatus;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerEntry;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerEntryId;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerEntryType;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerReference;
import io.d4tzz.ecommerce.inventory.domain.StockReservation;
import io.d4tzz.ecommerce.inventory.domain.Warehouse;
import io.d4tzz.ecommerce.inventory.domain.WarehouseCode;
import io.d4tzz.ecommerce.inventory.infrastructure.persistence.repository.InventoryItemRepositoryImpl;
import io.d4tzz.ecommerce.inventory.infrastructure.persistence.repository.StockLedgerRepositoryImpl;
import io.d4tzz.ecommerce.inventory.infrastructure.persistence.repository.StockReservationRepositoryImpl;
import io.d4tzz.ecommerce.inventory.infrastructure.persistence.repository.WarehouseRepositoryImpl;
import io.d4tzz.ecommerce.inventory.infrastructure.query.JdbcStockQueryService;
import io.d4tzz.ecommerce.inventory.infrastructure.query.JdbcWarehouseQueryService;
import io.d4tzz.ecommerce.inventory.query.StockItemView;
import io.d4tzz.ecommerce.inventory.query.StockLedgerEntryView;
import io.d4tzz.ecommerce.inventory.query.StockLedgerSearchCriteria;
import io.d4tzz.ecommerce.inventory.query.StockReservationView;
import io.d4tzz.ecommerce.inventory.query.StockSearchCriteria;
import io.d4tzz.ecommerce.inventory.query.VariantAvailabilityView;
import io.d4tzz.ecommerce.inventory.query.WarehouseView;
import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.support.PostgresContainerTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@DataJpaTest
@Import({
    WarehouseRepositoryImpl.class,
    InventoryItemRepositoryImpl.class,
    StockReservationRepositoryImpl.class,
    StockLedgerRepositoryImpl.class,
    JdbcStockQueryService.class,
    JdbcWarehouseQueryService.class,
    ProductRepositoryImpl.class,
    CategoryRepositoryImpl.class,
})
class InventoryQueryServicesTest extends PostgresContainerTest {

    // Postgres keeps microseconds; Instant.now() can carry nanoseconds
    private static final Instant T0 = Instant.now().truncatedTo(ChronoUnit.MILLIS).minusSeconds(7200);

    // private static final Address ADDRESS = Address.of("Ha Noi", "Cau Giay", "Dich Vong", "1 Xuan Thuy");

    @Autowired
    WarehouseRepositoryImpl warehouseRepository;

    @Autowired
    InventoryItemRepositoryImpl itemRepository;

    @Autowired
    StockReservationRepositoryImpl reservationRepository;

    @Autowired
    StockLedgerRepositoryImpl ledgerRepository;

    @Autowired
    JdbcStockQueryService stockQuery;

    @Autowired
    JdbcWarehouseQueryService warehouseQuery;

    @Autowired
    ProductRepositoryImpl productRepository;

    @Autowired
    CategoryRepositoryImpl categoryRepository;

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    EntityManager em;

    private Category category;

    @BeforeEach
    void setUp() {
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
        category = categoryRepository.save(
            Category.create(
                CategoryName.of("Clothes"),
                CategorySlug.of("clothes"),
                slug -> false,
                null,
                List.of(),
                List.of()
            )
        );
    }

    // ---- fixtures ----

    private void flush() {
        em.flush();
        em.clear();
    }

    /** Saves a product with one variant and returns the variant's id. */
    private VariantId variant(String productName, String slug, String sku) {
        Product product = Product.create(
            ProductName.of(productName),
            ProductSlug.of(slug),
            s -> false,
            Description.of(""),
            category.getId(),
            Map.of(),
            List.of()
        );
        product.addVariant(
            Sku.of(sku),
            Money.of(100_000),
            null,
            Dimensions.of(10, 10, 10),
            Weight.ofGrams(500),
            null,
            s -> false
        );
        return productRepository.save(product).getVariants().get(0).getId();
    }

    private Warehouse warehouse(String code, String province, boolean active) {
        Warehouse warehouse = warehouseRepository.save(
            Warehouse.create(
                WarehouseCode.of(code),
                "Warehouse " + code,
                Address.of(province, "District", "Ward", "1 Street"),
                c -> false,
                T0
            )
        );
        if (!active) {
            warehouse.deactivate(T0);
            warehouse = warehouseRepository.save(warehouse);
        }
        return warehouse;
    }

    /** Puts a stock item in the warehouse, with no ledger entry. */
    private void stock(Warehouse warehouse, VariantId variant, int onHand, int reserved, Instant at) {
        itemRepository.saveAll(
            List.of(InventoryItem.restore(null, warehouse.getId(), variant, onHand, reserved, at, at))
        );
    }

    /** Appends a receipt entry for the variant and returns its id. */
    private UUID receipt(Warehouse warehouse, VariantId variant, int quantity, String code, Instant at) {
        StockLedgerEntry entry = InventoryItem.create(warehouse.getId(), variant, at).receive(
            quantity,
            StockLedgerReference.goodsReceipt(code),
            "note " + code,
            at
        );
        ledgerRepository.saveAll(List.of(entry));
        return entry.id().value();
    }

    /** A RESERVE entry for an order (reserve itself is package-private in the domain). */
    private UUID reserveEntry(Warehouse warehouse, VariantId variant, long orderId, Instant at) {
        StockLedgerEntry entry = new StockLedgerEntry(
            StockLedgerEntryId.generate(),
            warehouse.getId(),
            variant,
            StockLedgerEntryType.RESERVE,
            2,
            10,
            2,
            StockLedgerReference.order(orderId),
            "",
            at
        );
        ledgerRepository.saveAll(List.of(entry));
        return entry.id().value();
    }

    private static List<String> skus(PageResponse<StockItemView> page) {
        return page.content().stream().map(StockItemView::sku).toList();
    }

    private StockSearchCriteria stockCriteria(Long warehouseId, String keyword, Integer maxAvailable) {
        return new StockSearchCriteria(warehouseId, keyword, maxAvailable, 0, 20);
    }

    private StockLedgerSearchCriteria ledgerCriteria(
        Long warehouseId,
        Long variantId,
        String type,
        String referenceType,
        String referenceValue,
        Instant from,
        Instant to
    ) {
        return new StockLedgerSearchCriteria(
            warehouseId,
            variantId,
            type,
            referenceType,
            referenceValue,
            from,
            to,
            0,
            20
        );
    }

    // ---- warehouses ----

    @Test
    void getWarehouses_listsActiveOnesFirstThenByCode() {
        warehouse("B-02", "Ha Noi", true);
        warehouse("A-01", "Ha Noi", false);
        warehouse("A-02", "Ha Noi", true);
        flush();

        List<WarehouseView> views = warehouseQuery.getWarehouses();

        assertEquals(List.of("A-02", "B-02", "A-01"), views.stream().map(WarehouseView::code).toList());
    }

    @Test
    void getWarehouse_mapsEveryColumn() {
        Warehouse saved = warehouse("HN-01", "Ha Noi", true);
        flush();

        WarehouseView view = warehouseQuery.getWarehouse(saved.getId().value()).orElseThrow();

        assertEquals(saved.getId().value(), view.id());
        assertEquals("HN-01", view.code());
        assertEquals("Warehouse HN-01", view.name());
        assertEquals("Ha Noi", view.province());
        assertEquals("District", view.district());
        assertEquals("Ward", view.ward());
        assertEquals("1 Street", view.detailAddress());
        assertEquals("ACTIVE", view.status());
        assertEquals(T0, view.createdAt());
        assertEquals(T0, view.updatedAt());
    }

    @Test
    void getWarehouse_isEmptyForAnUnknownId() {
        assertTrue(warehouseQuery.getWarehouse(999_999L).isEmpty());
    }

    @Test
    void getWarehouse_showsAnInactiveStatus() {
        Warehouse saved = warehouse("HN-01", "Ha Noi", false);
        flush();

        assertEquals("INACTIVE", warehouseQuery.getWarehouse(saved.getId().value()).orElseThrow().status());
    }

    // ---- stock ----

    @Test
    void getStock_showsProductDetailsAndQuantities() {
        Warehouse warehouse = warehouse("HN-01", "Ha Noi", true);
        VariantId shirt = variant("Plain shirt", "shirt", "SHIRT-1");
        stock(warehouse, shirt, 10, 4, T0);
        flush();

        StockItemView item = stockQuery
            .getStock(stockCriteria(null, null, null))
            .content()
            .get(0);

        assertEquals(warehouse.getId().value(), item.warehouseId());
        assertEquals("HN-01", item.warehouseCode());
        assertEquals(shirt.value(), item.variantId());
        assertEquals("SHIRT-1", item.sku());
        assertEquals("Plain shirt", item.productName());
        assertNotNull(item.productId());
        assertEquals(10, item.onHand());
        assertEquals(4, item.reserved());
        assertEquals(6, item.available());
        assertEquals(T0, item.updatedAt());
    }

    @Test
    void getStock_isEmptyWithoutItems() {
        PageResponse<StockItemView> page = stockQuery.getStock(stockCriteria(null, null, null));

        assertTrue(page.content().isEmpty());
        assertEquals(0, page.totalElements());
    }

    @Test
    void getStock_sortsByProductNameThenSkuThenWarehouseCode() {
        Warehouse first = warehouse("HN-01", "Ha Noi", true);
        Warehouse second = warehouse("HN-02", "Ha Noi", true);
        VariantId hat = variant("Hat", "hat", "HAT-1");
        VariantId shirt = variant("Shirt", "shirt", "SHIRT-1");
        stock(second, shirt, 1, 0, T0);
        stock(first, shirt, 1, 0, T0);
        stock(first, hat, 1, 0, T0);
        flush();

        List<StockItemView> content = stockQuery.getStock(stockCriteria(null, null, null)).content();

        assertEquals(List.of("HAT-1", "SHIRT-1", "SHIRT-1"), content.stream().map(StockItemView::sku).toList());
        assertEquals(List.of("HN-01", "HN-01", "HN-02"), content.stream().map(StockItemView::warehouseCode).toList());
    }

    @Test
    void getStock_filtersByWarehouse() {
        Warehouse first = warehouse("HN-01", "Ha Noi", true);
        Warehouse second = warehouse("HN-02", "Ha Noi", true);
        VariantId shirt = variant("Shirt", "shirt", "SHIRT-1");
        stock(first, shirt, 1, 0, T0);
        stock(second, shirt, 1, 0, T0);
        flush();

        PageResponse<StockItemView> page = stockQuery.getStock(stockCriteria(second.getId().value(), null, null));

        assertEquals(1, page.totalElements());
        assertEquals("HN-02", page.content().get(0).warehouseCode());
    }

    @Test
    void getStock_keywordMatchesSkuOrProductNameIgnoringCaseAndAccents() {
        Warehouse warehouse = warehouse("HN-01", "Ha Noi", true);
        stock(warehouse, variant("Áo thun", "ao-thun", "TEE-1"), 1, 0, T0);
        stock(warehouse, variant("Hat", "hat", "CAP-9"), 1, 0, T0);
        flush();

        assertEquals(List.of("TEE-1"), skus(stockQuery.getStock(stockCriteria(null, "ao thun", null))));
        assertEquals(List.of("TEE-1"), skus(stockQuery.getStock(stockCriteria(null, "tee", null))));
        assertEquals(List.of("CAP-9"), skus(stockQuery.getStock(stockCriteria(null, "cap-9", null))));
        assertTrue(
            stockQuery
                .getStock(stockCriteria(null, "nothing", null))
                .content()
                .isEmpty()
        );
        assertEquals(2, stockQuery.getStock(stockCriteria(null, "  ", null)).totalElements());
    }

    @Test
    void getStock_maxAvailableKeepsLowStockAndListsTheLowestFirst() {
        Warehouse warehouse = warehouse("HN-01", "Ha Noi", true);
        stock(warehouse, variant("Plenty", "plenty", "P-1"), 50, 0, T0);
        stock(warehouse, variant("Low", "low", "L-1"), 5, 2, T0);
        stock(warehouse, variant("Empty", "empty", "E-1"), 3, 3, T0);
        flush();

        assertEquals(List.of("E-1"), skus(stockQuery.getStock(stockCriteria(null, null, 0))));
        // available 0, 3, 50: with a limit the fewest come first, not by name
        assertEquals(List.of("E-1", "L-1"), skus(stockQuery.getStock(stockCriteria(null, null, 3))));
        assertEquals(List.of("E-1", "L-1", "P-1"), skus(stockQuery.getStock(stockCriteria(null, null, 50))));
    }

    @Test
    void getStock_pagesTheResult() {
        Warehouse warehouse = warehouse("HN-01", "Ha Noi", true);
        for (int i = 1; i <= 5; i++) {
            stock(warehouse, variant("Product " + i, "p" + i, "SKU-" + i), 1, 0, T0);
        }
        flush();

        PageResponse<StockItemView> second = stockQuery.getStock(new StockSearchCriteria(null, null, null, 1, 2));

        assertEquals(List.of("SKU-3", "SKU-4"), skus(second));
        assertEquals(5, second.totalElements());
        assertEquals(3, second.totalPages());
        assertTrue(second.hasNext());
        assertTrue(second.hasPrevious());
    }

    // ---- availability ----

    @Test
    void getAvailability_listsOnlyActiveWarehousesMostAvailableFirst() {
        Warehouse first = warehouse("HN-01", "Ha Noi", true);
        Warehouse second = warehouse("HN-02", "Ha Noi", true);
        Warehouse closed = warehouse("HN-03", "Ha Noi", false);
        VariantId shirt = variant("Shirt", "shirt", "SHIRT-1");
        stock(first, shirt, 5, 4, T0);
        stock(second, shirt, 9, 0, T0);
        stock(closed, shirt, 100, 0, T0);
        flush();

        List<VariantAvailabilityView> views = stockQuery.getAvailability(List.of(shirt.value()));

        assertEquals(2, views.size());
        assertEquals("HN-02", views.get(0).warehouseCode());
        assertEquals(9, views.get(0).available());
        assertEquals(shirt.value(), views.get(0).variantId());
        assertEquals("HN-01", views.get(1).warehouseCode());
        assertEquals(1, views.get(1).available());
    }

    @Test
    void getAvailability_onlyReturnsTheRequestedVariants() {
        Warehouse warehouse = warehouse("HN-01", "Ha Noi", true);
        VariantId shirt = variant("Shirt", "shirt", "SHIRT-1");
        VariantId hat = variant("Hat", "hat", "HAT-1");
        stock(warehouse, shirt, 5, 0, T0);
        stock(warehouse, hat, 5, 0, T0);
        flush();

        List<VariantAvailabilityView> views = stockQuery.getAvailability(List.of(hat.value()));

        assertEquals(List.of(hat.value()), views.stream().map(VariantAvailabilityView::variantId).toList());
    }

    @Test
    void getAvailability_isEmptyForNoVariants() {
        assertTrue(stockQuery.getAvailability(List.of()).isEmpty());
    }

    // ---- ledger ----

    @Test
    void getLedger_mapsEveryColumn() {
        Warehouse warehouse = warehouse("HN-01", "Ha Noi", true);
        VariantId shirt = variant("Shirt", "shirt", "SHIRT-1");
        UUID id = receipt(warehouse, shirt, 5, "GR-1", T0);
        flush();

        StockLedgerEntryView view = stockQuery
            .getLedger(ledgerCriteria(null, null, null, null, null, null, null))
            .content()
            .get(0);

        assertEquals(id, view.id());
        assertEquals(warehouse.getId().value(), view.warehouseId());
        assertEquals("HN-01", view.warehouseCode());
        assertEquals(shirt.value(), view.variantId());
        assertEquals("SHIRT-1", view.sku());
        assertEquals("Shirt", view.productName());
        assertEquals("RECEIVE", view.type());
        assertEquals(5, view.quantity());
        assertEquals(5, view.onHandAfter());
        assertEquals(0, view.reservedAfter());
        assertEquals("GOODS_RECEIPT", view.referenceType());
        assertEquals("GR-1", view.referenceValue());
        assertEquals("note GR-1", view.note());
        assertEquals(T0, view.occurredAt());
    }

    @Test
    void getLedger_listsTheNewestFirst() {
        Warehouse warehouse = warehouse("HN-01", "Ha Noi", true);
        VariantId shirt = variant("Shirt", "shirt", "SHIRT-1");
        receipt(warehouse, shirt, 1, "GR-OLD", T0);
        receipt(warehouse, shirt, 1, "GR-NEW", T0.plusSeconds(60));
        flush();

        var page = stockQuery.getLedger(ledgerCriteria(null, null, null, null, null, null, null));

        assertEquals(
            List.of("GR-NEW", "GR-OLD"),
            page.content().stream().map(StockLedgerEntryView::referenceValue).toList()
        );
    }

    @Test
    void getLedger_filtersByWarehouseAndVariant() {
        Warehouse first = warehouse("HN-01", "Ha Noi", true);
        Warehouse second = warehouse("HN-02", "Ha Noi", true);
        VariantId shirt = variant("Shirt", "shirt", "SHIRT-1");
        VariantId hat = variant("Hat", "hat", "HAT-1");
        receipt(first, shirt, 1, "GR-1", T0);
        receipt(second, shirt, 1, "GR-2", T0);
        receipt(first, hat, 1, "GR-3", T0);
        flush();

        var byWarehouse = stockQuery.getLedger(
            ledgerCriteria(second.getId().value(), null, null, null, null, null, null)
        );
        var byVariant = stockQuery.getLedger(ledgerCriteria(null, hat.value(), null, null, null, null, null));

        assertEquals(
            List.of("GR-2"),
            byWarehouse.content().stream().map(StockLedgerEntryView::referenceValue).toList()
        );
        assertEquals(List.of("GR-3"), byVariant.content().stream().map(StockLedgerEntryView::referenceValue).toList());
    }

    @Test
    void getLedger_filtersByTypeAndReferenceIgnoringCase() {
        Warehouse warehouse = warehouse("HN-01", "Ha Noi", true);
        VariantId shirt = variant("Shirt", "shirt", "SHIRT-1");
        receipt(warehouse, shirt, 1, "GR-1", T0);
        reserveEntry(warehouse, shirt, 42L, T0.plusSeconds(1));
        flush();

        var reserves = stockQuery.getLedger(ledgerCriteria(null, null, " reserve ", null, null, null, null));
        var orders = stockQuery.getLedger(ledgerCriteria(null, null, null, "order", "42", null, null));
        var receipts = stockQuery.getLedger(ledgerCriteria(null, null, null, "GOODS_RECEIPT", null, null, null));
        var other = stockQuery.getLedger(ledgerCriteria(null, null, null, "ORDER", "43", null, null));

        assertEquals(List.of("RESERVE"), reserves.content().stream().map(StockLedgerEntryView::type).toList());
        assertEquals(List.of("42"), orders.content().stream().map(StockLedgerEntryView::referenceValue).toList());
        assertEquals(List.of("GR-1"), receipts.content().stream().map(StockLedgerEntryView::referenceValue).toList());
        assertTrue(other.content().isEmpty());
    }

    @Test
    void getLedger_fromIsIncludedAndToIsExcluded() {
        Warehouse warehouse = warehouse("HN-01", "Ha Noi", true);
        VariantId shirt = variant("Shirt", "shirt", "SHIRT-1");
        receipt(warehouse, shirt, 1, "GR-1", T0);
        receipt(warehouse, shirt, 1, "GR-2", T0.plusSeconds(60));
        receipt(warehouse, shirt, 1, "GR-3", T0.plusSeconds(120));
        flush();

        var page = stockQuery.getLedger(
            ledgerCriteria(null, null, null, null, null, T0.plusSeconds(60), T0.plusSeconds(120))
        );

        assertEquals(List.of("GR-2"), page.content().stream().map(StockLedgerEntryView::referenceValue).toList());
    }

    @Test
    void getLedger_pagesTheResult() {
        Warehouse warehouse = warehouse("HN-01", "Ha Noi", true);
        VariantId shirt = variant("Shirt", "shirt", "SHIRT-1");
        for (int i = 0; i < 5; i++) {
            receipt(warehouse, shirt, 1, "GR-" + i, T0.plusSeconds(i));
        }
        flush();

        var page = stockQuery.getLedger(new StockLedgerSearchCriteria(null, null, null, null, null, null, null, 1, 2));

        assertEquals(
            List.of("GR-2", "GR-1"),
            page.content().stream().map(StockLedgerEntryView::referenceValue).toList()
        );
        assertEquals(5, page.totalElements());
    }

    @Test
    void getLedgerEntries_returnsTheGivenEntriesByWarehouseThenVariant() {
        Warehouse first = warehouse("HN-01", "Ha Noi", true);
        Warehouse second = warehouse("HN-02", "Ha Noi", true);
        VariantId shirt = variant("Shirt", "shirt", "SHIRT-1");
        VariantId hat = variant("Hat", "hat", "HAT-1");
        UUID a = receipt(second, shirt, 1, "GR-1", T0);
        UUID b = receipt(first, hat, 1, "GR-2", T0);
        UUID c = receipt(first, shirt, 1, "GR-3", T0);
        receipt(first, shirt, 1, "GR-4", T0);
        flush();

        List<StockLedgerEntryView> views = stockQuery.getLedgerEntries(List.of(a, b, c));

        assertEquals(3, views.size());
        assertEquals(
            List.of("HN-01", "HN-01", "HN-02"),
            views.stream().map(StockLedgerEntryView::warehouseCode).toList()
        );
        assertEquals(
            List.of(shirt.value(), hat.value()).stream().sorted().toList(),
            views.subList(0, 2).stream().map(StockLedgerEntryView::variantId).toList()
        );
        assertTrue(views.stream().noneMatch(v -> v.referenceValue().equals("GR-4")));
    }

    @Test
    void getLedgerEntries_isEmptyForNoIds() {
        assertTrue(stockQuery.getLedgerEntries(List.of()).isEmpty());
    }

    // ---- reservations ----

    private StockReservation reserved(
        long orderId,
        Warehouse warehouse,
        ReservationStatus status,
        ReservationLine... lines
    ) {
        return reservationRepository.save(
            StockReservation.restore(null, orderId, warehouse.getId(), List.of(lines), status, T0, T0.plusSeconds(5))
        );
    }

    @Test
    void getReservationsByOrder_groupsLinesPerWarehouseInAscendingOrder() {
        Warehouse first = warehouse("HN-01", "Ha Noi", true);
        Warehouse second = warehouse("HN-02", "Ha Noi", true);
        VariantId shirt = variant("Shirt", "shirt", "SHIRT-1");
        VariantId hat = variant("Hat", "hat", "HAT-1");
        reserved(9L, second, ReservationStatus.COMMITTED, new ReservationLine(hat, 1));
        reserved(9L, first, ReservationStatus.RESERVED, new ReservationLine(shirt, 2), new ReservationLine(hat, 3));
        reserved(10L, first, ReservationStatus.RESERVED, new ReservationLine(shirt, 7));
        flush();

        List<StockReservationView> views = stockQuery.getReservationsByOrder(9L);

        assertEquals(2, views.size());
        StockReservationView one = views.get(0);
        assertEquals("HN-01", one.warehouseCode());
        assertEquals(first.getId().value(), one.warehouseId());
        assertEquals(9L, one.orderId());
        assertEquals("RESERVED", one.status());
        assertEquals(T0, one.createdAt());
        assertEquals(T0.plusSeconds(5), one.updatedAt());
        assertEquals(2, one.lines().size());
        assertEquals(List.of("SHIRT-1", "HAT-1"), one.lines().stream().map(StockReservationView.Line::sku).toList());
        assertEquals(5, one.lines().stream().mapToInt(StockReservationView.Line::quantity).sum());
        assertEquals("HN-02", views.get(1).warehouseCode());
        assertEquals("COMMITTED", views.get(1).status());
        assertEquals(1, views.get(1).lines().size());
    }

    @Test
    void getReservationsByOrder_isEmptyForAnOrderWithoutReservations() {
        assertTrue(stockQuery.getReservationsByOrder(404L).isEmpty());
    }
}
