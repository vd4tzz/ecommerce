package io.d4tzz.ecommerce.checkout.inbound.rest;

import io.d4tzz.ecommerce.checkout.application.CheckoutUseCase;
import io.d4tzz.ecommerce.checkout.application.command.CheckoutCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.order.application.command.PlaceOrderCommand.PaymentMode;
import io.d4tzz.ecommerce.order.query.OrderQueryService;
import io.d4tzz.ecommerce.order.query.OrderView;
import io.d4tzz.ecommerce.rest.ValidationHelper;
import io.d4tzz.ecommerce.rest.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        ValidationHelper.validate(command, validator);
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
