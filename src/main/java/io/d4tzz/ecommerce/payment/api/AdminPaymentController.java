package io.d4tzz.ecommerce.payment.api;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.payment.application.AssignPaymentToOrderUseCase;
import io.d4tzz.ecommerce.payment.application.command.AssignPaymentToOrderCommand;
import io.d4tzz.ecommerce.payment.query.PaymentQueryService;
import io.d4tzz.ecommerce.payment.query.PaymentSearchCriteria;
import io.d4tzz.ecommerce.payment.query.PaymentView;
import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.rest.ValidationHelper;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Money received and sent back, as recorded from gateways and banks. */
@RestController
@RequestMapping("/admin/payments")
@Tag(name = "Admin - Payments", description = "Payments and refunds, and payments no order was found for")
public class AdminPaymentController {

    private final PaymentQueryService paymentQueryService;
    private final AssignPaymentToOrderUseCase assignPaymentToOrderUseCase;
    private final Validator validator;

    public AdminPaymentController(
        PaymentQueryService paymentQueryService,
        AssignPaymentToOrderUseCase assignPaymentToOrderUseCase,
        Validator validator
    ) {
        this.paymentQueryService = paymentQueryService;
        this.assignPaymentToOrderUseCase = assignPaymentToOrderUseCase;
        this.validator = validator;
    }

    /** {@code ?unmatched=true} lists the payments waiting to be assigned to an order. */
    @GetMapping
    public ResponseEntity<PageResponse<PaymentView>> getPayments(
        @RequestParam(required = false) String orderReference,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String method,
        @RequestParam(required = false) String shopAccount,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean unmatched,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var criteria = new PaymentSearchCriteria(
            orderReference,
            type,
            method,
            shopAccount,
            keyword,
            unmatched,
            from,
            to,
            page,
            size
        );
        return ResponseEntity.ok(paymentQueryService.searchPayments(criteria));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentView> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(findPayment(id));
    }

    /** Gives an unmatched payment its order; the order then accepts it or asks for a refund. */
    @PostMapping("/{id}/assign")
    public ResponseEntity<PaymentView> assign(@PathVariable Long id, @RequestBody AssignPaymentRequest request) {
        var command = new AssignPaymentToOrderCommand(id, request.orderCode());
        ValidationHelper.validate(command, validator);
        assignPaymentToOrderUseCase.execute(command);
        return ResponseEntity.ok(findPayment(id));
    }

    private PaymentView findPayment(Long id) {
        return paymentQueryService
            .getPayment(id)
            .orElseThrow(() -> new NotFoundException("Payment not found with id: " + id));
    }
}

record AssignPaymentRequest(String orderCode) {}
