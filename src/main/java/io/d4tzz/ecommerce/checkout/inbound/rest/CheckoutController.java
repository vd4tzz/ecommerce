package io.d4tzz.ecommerce.checkout.inbound.rest;

import io.d4tzz.ecommerce.checkout.application.CheckoutUseCase;
import io.d4tzz.ecommerce.checkout.application.command.CheckoutCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.order.application.command.PlaceOrderCommand.PaymentMode;
import io.d4tzz.ecommerce.order.query.OrderQueryService;
import io.d4tzz.ecommerce.order.query.OrderView;
import io.d4tzz.ecommerce.spring.SpringValidation;
import io.d4tzz.ecommerce.spring.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Turns the items the customer picked into an order. The prices come from the catalog, never from the request. */
@RestController
@RequestMapping("/checkout")
@Tag(name = "Checkout", description = "Place an order from the items the customer picked")
public class CheckoutController {

    private final CheckoutUseCase checkoutUseCase;
    private final OrderQueryService orderQueryService;
    private final Validator validator;

    public CheckoutController(
        CheckoutUseCase checkoutUseCase,
        OrderQueryService orderQueryService,
        Validator validator
    ) {
        this.checkoutUseCase = checkoutUseCase;
        this.orderQueryService = orderQueryService;
        this.validator = validator;
    }

    @Operation(
        summary = "Place an order",
        description = "Creates an order for the authenticated user from variant ids and quantities. Prices come from the catalog, the same variant listed twice becomes one line, the warehouses that send the order are chosen automatically, and the shipping fee is the sum of one package per warehouse with the given carrier. The stock is held in the same transaction as the order. A prepaid order must be paid within 15 minutes, or it is cancelled and its stock released; a cash on delivery order has no deadline."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The new order"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, a variant is not for sale, or the carrier is unknown",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "A variant has too little stock, or the stock was taken while the order was being placed",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping
    public ResponseEntity<OrderView> checkout(@CurrentUser Long userId, @RequestBody CheckoutRequest request) {
        var command = new CheckoutCommand(
            userId,
            request.recipientName(),
            request.recipientPhone(),
            request.province(),
            request.district(),
            request.ward(),
            request.detailAddress(),
            request.carrier(),
            request.items() == null
                ? List.of()
                : request.items().stream().map(item -> new CheckoutCommand.Item(item.variantId(), item.quantity())).toList(),
            request.paymentMode(),
            request.note()
        );
        SpringValidation.validate(command, validator);
        Long orderId = checkoutUseCase.execute(command);
        OrderView order = orderQueryService
            .getOrder(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found with id: " + orderId));
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}

record CheckoutRequest(
    String recipientName,
    String recipientPhone,
    String province,
    String district,
    String ward,
    String detailAddress,
    String carrier,
    List<Item> items,
    PaymentMode paymentMode,
    String note
) {
    record Item(Long variantId, int quantity) {}
}
