package io.d4tzz.ecommerce.order.inbound.rest;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.order.application.CancelOrderUseCase;
import io.d4tzz.ecommerce.order.application.ConfirmOrderUseCase;
import io.d4tzz.ecommerce.order.application.DeliverOrderUseCase;
import io.d4tzz.ecommerce.order.application.ReturnOrderUseCase;
import io.d4tzz.ecommerce.order.application.ShipOrderUseCase;
import io.d4tzz.ecommerce.order.application.command.CancelOrderCommand;
import io.d4tzz.ecommerce.order.application.command.ConfirmOrderCommand;
import io.d4tzz.ecommerce.order.application.command.DeliverOrderCommand;
import io.d4tzz.ecommerce.order.application.command.ReturnOrderCommand;
import io.d4tzz.ecommerce.order.application.command.ShipOrderCommand;
import io.d4tzz.ecommerce.order.query.OrderQueryService;
import io.d4tzz.ecommerce.order.query.OrderSearchCriteria;
import io.d4tzz.ecommerce.order.query.OrderSummaryView;
import io.d4tzz.ecommerce.order.query.OrderView;
import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.rest.ValidationHelper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin order operations. Orders are created by checkout and payments are applied from payment events, so
 * neither is exposed here.
 */
@RestController
@RequestMapping("/admin/orders")
@Tag(name = "Admin - Orders", description = "Search orders and drive their fulfillment")
public class AdminOrderController {

    private final OrderQueryService orderQueryService;
    private final ConfirmOrderUseCase confirmOrderUseCase;
    private final ShipOrderUseCase shipOrderUseCase;
    private final DeliverOrderUseCase deliverOrderUseCase;
    private final ReturnOrderUseCase returnOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final Validator validator;

    public AdminOrderController(
        OrderQueryService orderQueryService,
        ConfirmOrderUseCase confirmOrderUseCase,
        ShipOrderUseCase shipOrderUseCase,
        DeliverOrderUseCase deliverOrderUseCase,
        ReturnOrderUseCase returnOrderUseCase,
        CancelOrderUseCase cancelOrderUseCase,
        Validator validator
    ) {
        this.orderQueryService = orderQueryService;
        this.confirmOrderUseCase = confirmOrderUseCase;
        this.shipOrderUseCase = shipOrderUseCase;
        this.deliverOrderUseCase = deliverOrderUseCase;
        this.returnOrderUseCase = returnOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.validator = validator;
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryView>> searchOrders(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String paymentStatus,
        @RequestParam(required = false) Boolean readyToConfirm,
        @RequestParam(required = false) Boolean readyToShip,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        String resolvedStatus = Boolean.TRUE.equals(readyToShip) ? "CONFIRMED" : status;
        var criteria = new OrderSearchCriteria(userId, resolvedStatus, paymentStatus, readyToConfirm, keyword, page, size);
        return ResponseEntity.ok(orderQueryService.searchOrders(criteria));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderView> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(findOrder(id));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<OrderView> confirm(@PathVariable Long id) {
        confirmOrderUseCase.execute(new ConfirmOrderCommand(id));
        return ResponseEntity.ok(findOrder(id));
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<OrderView> ship(@PathVariable Long id) {
        shipOrderUseCase.execute(new ShipOrderCommand(id));
        return ResponseEntity.ok(findOrder(id));
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<OrderView> deliver(@PathVariable Long id) {
        deliverOrderUseCase.execute(new DeliverOrderCommand(id));
        return ResponseEntity.ok(findOrder(id));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<OrderView> markReturned(@PathVariable Long id) {
        returnOrderUseCase.execute(new ReturnOrderCommand(id));
        return ResponseEntity.ok(findOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderView> cancel(@PathVariable Long id, @RequestBody CancelOrderRequest request) {
        var command = new CancelOrderCommand(id, request.reason());
        ValidationHelper.validate(command, validator);
        cancelOrderUseCase.execute(command);
        return ResponseEntity.ok(findOrder(id));
    }

    private OrderView findOrder(Long id) {
        return orderQueryService
            .getOrder(id)
            .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));
    }
}

record CancelOrderRequest(String reason) {}
