package io.d4tzz.ecommerce.order.inbound.rest;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.order.application.CancelMyOrderUseCase;
import io.d4tzz.ecommerce.order.application.PayMyOrderUseCase;
import io.d4tzz.ecommerce.order.application.command.CancelMyOrderCommand;
import io.d4tzz.ecommerce.order.application.command.PayMyOrderCommand;
import io.d4tzz.ecommerce.order.query.OrderQueryService;
import io.d4tzz.ecommerce.order.query.OrderSearchCriteria;
import io.d4tzz.ecommerce.order.query.OrderSummaryView;
import io.d4tzz.ecommerce.order.query.OrderView;
import io.d4tzz.ecommerce.payment.application.port.PaymentRedirect;
import io.d4tzz.ecommerce.common.PageResponse;
import io.d4tzz.ecommerce.spring.SpringValidation;
import io.d4tzz.ecommerce.spring.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The current user's own orders, addressed by order code. */
@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Customer order history and cancellation")
public class OrderController {

    private static final String DEFAULT_CANCEL_REASON = "Cancelled by customer";

    private final OrderQueryService orderQueryService;
    private final CancelMyOrderUseCase cancelMyOrderUseCase;
    private final PayMyOrderUseCase payMyOrderUseCase;
    private final Validator validator;

    public OrderController(
        OrderQueryService orderQueryService,
        CancelMyOrderUseCase cancelMyOrderUseCase,
        PayMyOrderUseCase payMyOrderUseCase,
        Validator validator
    ) {
        this.orderQueryService = orderQueryService;
        this.cancelMyOrderUseCase = cancelMyOrderUseCase;
        this.payMyOrderUseCase = payMyOrderUseCase;
        this.validator = validator;
    }

    @Operation(
        summary = "List my orders",
        description = "Paged list of the authenticated user's orders, newest first. Can be filtered by status."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "A page of order summaries")
    })
    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryView>> getMyOrders(
        @CurrentUser Long userId,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var criteria = new OrderSearchCriteria(userId, status, null, null, null, page, size);
        return ResponseEntity.ok(orderQueryService.searchOrders(criteria));
    }

    @Operation(
        summary = "Get my order",
        description = "Returns one of the authenticated user's orders by order code."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The order"),
        @ApiResponse(
            responseCode = "404",
            description = "Order not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @GetMapping("/{orderCode}")
    public ResponseEntity<OrderView> getMyOrder(@CurrentUser Long userId, @PathVariable String orderCode) {
        return ResponseEntity.ok(getOrder(userId, orderCode));
    }

    @Operation(
        summary = "Cancel my order",
        description = "Cancels the order while it is still PENDING. The reason is optional and defaults to a standard text. Reserved stock is released, and a refund is requested if the order was already paid. Cancelling an already cancelled order changes nothing."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The order after cancellation"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, or the order can no longer be cancelled by the customer",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Order not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping("/{orderCode}/cancel")
    public ResponseEntity<OrderView> cancel(
        @CurrentUser Long userId,
        @PathVariable String orderCode,
        @RequestBody(required = false) CancelMyOrderRequest request
    ) {
        String reason =
            request != null && request.reason() != null && !request.reason().isBlank()
                ? request.reason()
                : DEFAULT_CANCEL_REASON;
        var command = new CancelMyOrderCommand(userId, orderCode, reason);
        SpringValidation.validate(command, validator);
        cancelMyOrderUseCase.execute(command);
        return ResponseEntity.ok(getOrder(userId, orderCode));
    }

    /**
     * Where to send the customer to pay a prepaid order through the gateway they picked. The client sends
     * {@code params} to {@code url} with {@code httpMethod} (e.g. by submitting a hidden form), which takes the
     * customer to the gateway's page.
     */
    @Operation(
        summary = "Start paying my order",
        description = "Returns where to send the customer to pay a prepaid order through the chosen gateway. Changes nothing, so it can be called again until the order is paid or the time to pay is over. The order becomes paid only when the gateway reports the money."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Redirect instructions for the gateway"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, or the order cannot be paid online now (paid on delivery, already paid, not pending, or time to pay is over)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Order not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping("/{orderCode}/payment")
    public ResponseEntity<PaymentRedirect> pay(
        @CurrentUser Long userId,
        @PathVariable String orderCode,
        @RequestBody PayMyOrderRequest request,
        HttpServletRequest httpRequest
    ) {
        // Behind a reverse proxy this is the proxy's address unless server.forward-headers-strategy is set
        String clientIp = httpRequest.getRemoteAddr();
        var command = new PayMyOrderCommand(userId, orderCode, request.gateway(), clientIp);
        SpringValidation.validate(command, validator);
        return ResponseEntity.ok(payMyOrderUseCase.execute(command));
    }

    private OrderView getOrder(Long userId, String orderCode) {
        return orderQueryService
            .getUserOrder(userId, orderCode)
            .orElseThrow(() -> new NotFoundException("Order not found with code: " + orderCode));
    }
}

record CancelMyOrderRequest(String reason) {}

record PayMyOrderRequest(String gateway) {}
