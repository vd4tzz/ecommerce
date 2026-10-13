package io.d4tzz.ecommerce.inventory.inbound.rest;

import io.d4tzz.ecommerce.inventory.application.ReceiveGoodsUseCase;
import io.d4tzz.ecommerce.inventory.application.command.ReceiveGoodsCommand;
import io.d4tzz.ecommerce.inventory.query.StockItemView;
import io.d4tzz.ecommerce.inventory.query.StockLedgerEntryView;
import io.d4tzz.ecommerce.inventory.query.StockLedgerSearchCriteria;
import io.d4tzz.ecommerce.inventory.query.StockQueryService;
import io.d4tzz.ecommerce.inventory.query.StockReservationView;
import io.d4tzz.ecommerce.inventory.query.StockSearchCriteria;
import io.d4tzz.ecommerce.inventory.query.VariantAvailabilityView;
import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.spring.SpringValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin stock operations. Reserve/release/commit/restock are not exposed here: they are driven by the order
 * flow (direct call or events), never by hand.
 */
@RestController
@RequestMapping("/admin/inventory")
@Tag(name = "Admin - Inventory", description = "Goods receipts, stock levels, ledger and reservations")
public class AdminStockController {

    private final StockQueryService stockQueryService;
    private final ReceiveGoodsUseCase receiveGoodsUseCase;
    private final Validator validator;

    public AdminStockController(
        StockQueryService stockQueryService,
        ReceiveGoodsUseCase receiveGoodsUseCase,
        Validator validator
    ) {
        this.stockQueryService = stockQueryService;
        this.receiveGoodsUseCase = receiveGoodsUseCase;
        this.validator = validator;
    }

    @Operation(
        summary = "Receive goods",
        description = "Puts goods into an active warehouse and records one ledger entry per variant. A variant listed twice is summed. The receipt code identifies the receipt and can be used once per warehouse, so sending the same receipt again is rejected instead of counted twice. Returns the new ledger entries."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The ledger entries of the receipt"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, or the warehouse is inactive",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "The warehouse or a variant does not exist",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "The receipt code was already received into this warehouse",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping("/receipts")
    public ResponseEntity<List<StockLedgerEntryView>> receiveGoods(@RequestBody ReceiveGoodsRequest request) {
        List<ReceiveGoodsCommand.Item> items =
            request.items() != null
                ? request
                      .items()
                      .stream()
                      .map(item -> new ReceiveGoodsCommand.Item(item.variantId(), item.quantity()))
                      .toList()
                : List.of();

        var command = new ReceiveGoodsCommand(
            request.warehouseId(),
            request.receiptCode(),
            request.note() != null ? request.note() : "",
            items
        );
        SpringValidation.validate(command, validator);
        List<UUID> entryIds = receiveGoodsUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(stockQueryService.getLedgerEntries(entryIds));
    }

    @Operation(
        summary = "Search stock",
        description = "Lists the stock of variants per warehouse with on-hand, reserved and available units, by product name. The keyword matches the SKU or the product name, ignoring accents. maxAvailable keeps items with at most that many available units (0 for out of stock) and puts the lowest first."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "A page of stock items")
    })
    @GetMapping("/stock")
    public ResponseEntity<PageResponse<StockItemView>> getStock(
        @RequestParam(required = false) Long warehouseId,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer maxAvailable,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
            stockQueryService.getStock(new StockSearchCriteria(warehouseId, keyword, maxAvailable, page, size))
        );
    }

    @Operation(
        summary = "Get availability of variants",
        description = "Returns the sellable units of the given variants in each active warehouse. A variant without stock in a warehouse does not appear for it."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The availability, by variant then most available first")
    })
    @GetMapping("/availability")
    public ResponseEntity<List<VariantAvailabilityView>> getAvailability(@RequestParam List<Long> variantIds) {
        return ResponseEntity.ok(stockQueryService.getAvailability(variantIds));
    }

    @Operation(
        summary = "Search the stock ledger",
        description = "Lists the stock ledger, newest first. Every filter is optional. type is RECEIVE, RESERVE, RELEASE, COMMIT or RESTOCK; referenceType is ORDER or GOODS_RECEIPT. from is included and to is excluded."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "A page of ledger entries")
    })
    @GetMapping("/ledger")
    public ResponseEntity<PageResponse<StockLedgerEntryView>> getLedger(
        @RequestParam(required = false) Long warehouseId,
        @RequestParam(required = false) Long variantId,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String referenceType,
        @RequestParam(required = false) String referenceValue,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var criteria = new StockLedgerSearchCriteria(
            warehouseId,
            variantId,
            type,
            referenceType,
            referenceValue,
            from,
            to,
            page,
            size
        );
        return ResponseEntity.ok(stockQueryService.getLedger(criteria));
    }

    @Operation(
        summary = "Get the reservations of an order",
        description = "Returns the stock held for an order, one reservation per warehouse that sends part of it, with the lines and status. An order without reservations gets an empty list."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The reservations of the order")
    })
    @GetMapping("/reservations")
    public ResponseEntity<List<StockReservationView>> getReservations(@RequestParam Long orderId) {
        return ResponseEntity.ok(stockQueryService.getReservationsByOrder(orderId));
    }
}

record ReceiveGoodsRequest(Long warehouseId, String receiptCode, String note, List<ReceiveGoodsItemRequest> items) {}

record ReceiveGoodsItemRequest(Long variantId, int quantity) {}
