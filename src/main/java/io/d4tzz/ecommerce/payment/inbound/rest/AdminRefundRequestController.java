package io.d4tzz.ecommerce.payment.inbound.rest;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.payment.application.CompleteRefundRequestUseCase;
import io.d4tzz.ecommerce.payment.application.command.CompleteRefundRequestCommand;
import io.d4tzz.ecommerce.payment.domain.PaymentMethod;
import io.d4tzz.ecommerce.payment.query.RefundRequestQueryService;
import io.d4tzz.ecommerce.payment.query.RefundRequestSearchCriteria;
import io.d4tzz.ecommerce.payment.query.RefundRequestView;
import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.spring.SpringValidation;
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

/** Money the shop owes back to customers, sent by hand from the shop's account. */
@RestController
@RequestMapping("/admin/refund-requests")
@Tag(name = "Admin - Refund requests", description = "Refunds waiting to be sent by hand")
public class AdminRefundRequestController {

    private final RefundRequestQueryService refundRequestQueryService;
    private final CompleteRefundRequestUseCase completeRefundRequestUseCase;
    private final Validator validator;

    public AdminRefundRequestController(
        RefundRequestQueryService refundRequestQueryService,
        CompleteRefundRequestUseCase completeRefundRequestUseCase,
        Validator validator
    ) {
        this.refundRequestQueryService = refundRequestQueryService;
        this.completeRefundRequestUseCase = completeRefundRequestUseCase;
        this.validator = validator;
    }

    /**
     * {@code ?status=PENDING} lists what still has to be sent, oldest first; {@code ?orderReference=ORD...} the
     * requests of one order.
     */
    @GetMapping
    public ResponseEntity<PageResponse<RefundRequestView>> getRefundRequests(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String orderReference,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
            refundRequestQueryService.searchRefundRequests(new RefundRequestSearchCriteria(status, orderReference, page, size))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<RefundRequestView> getRefundRequest(@PathVariable Long id) {
        return ResponseEntity.ok(findRefundRequest(id));
    }

    /** Call after the money was sent: records the refund and marks the request done. */
    @PostMapping("/{id}/complete")
    public ResponseEntity<RefundRequestView> complete(
        @PathVariable Long id,
        @RequestBody CompleteRefundRequestRequest request
    ) {
        var command = new CompleteRefundRequestCommand(
            id,
            request.method(),
            request.shopAccount(),
            request.transactionNo(),
            request.note()
        );
        SpringValidation.validate(command, validator);
        completeRefundRequestUseCase.execute(command);
        return ResponseEntity.ok(findRefundRequest(id));
    }

    private RefundRequestView findRefundRequest(Long id) {
        return refundRequestQueryService
            .getRefundRequest(id)
            .orElseThrow(() -> new NotFoundException("Refund request not found with id: " + id));
    }
}

record CompleteRefundRequestRequest(PaymentMethod method, String shopAccount, String transactionNo, String note) {}
