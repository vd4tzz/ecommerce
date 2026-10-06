package io.d4tzz.ecommerce.payment.domain;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.time.Instant;

/**
 * Money the shop owes back for one payment, waiting for someone to send it. Bank transfers cannot be reversed by
 * any API, so a person sends the money from the shop's account and then marks the request done, which records
 * the REFUND. At most one request per payment is PENDING at a time.
 */
public class RefundRequest {

    private final RefundRequestId id;
    // the PAYMENT to give back
    private final PaymentId paymentId;
    private final Money amount;
    private final String reason;
    private RefundRequestStatus status;
    // the REFUND recorded when the money was sent; null while PENDING
    private PaymentId refundId;
    private final Instant createdAt;
    private Instant completedAt;

    private RefundRequest(
        RefundRequestId id,
        PaymentId paymentId,
        Money amount,
        String reason,
        RefundRequestStatus status,
        PaymentId refundId,
        Instant createdAt,
        Instant completedAt
    ) {
        if (amount.isZero()) {
            throw new InvalidArgumentException("Refund amount must be greater than zero");
        }
        if (reason == null || reason.isBlank()) {
            throw new InvalidArgumentException("Refund reason must not be blank");
        }
        this.id = id;
        this.paymentId = paymentId;
        this.amount = amount;
        this.reason = reason.trim();
        this.status = status;
        this.refundId = refundId;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public static RefundRequest create(PaymentId paymentId, Money amount, String reason, Instant now) {
        return new RefundRequest(null, paymentId, amount, reason, RefundRequestStatus.PENDING, null, now, null);
    }

    public static RefundRequest restore(
        RefundRequestId id,
        PaymentId paymentId,
        Money amount,
        String reason,
        RefundRequestStatus status,
        PaymentId refundId,
        Instant createdAt,
        Instant completedAt
    ) {
        return new RefundRequest(id, paymentId, amount, reason, status, refundId, createdAt, completedAt);
    }

    /** Returns false when it was already done, e.g. the admin clicked twice. */
    public boolean complete(PaymentId refundId, Instant now) {
        if (status == RefundRequestStatus.DONE) {
            return false;
        }
        this.status = RefundRequestStatus.DONE;
        this.refundId = refundId;
        this.completedAt = now;
        return true;
    }

    public boolean isPending() {
        return status == RefundRequestStatus.PENDING;
    }

    public RefundRequestId getId() {
        return id;
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }

    public Money getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public RefundRequestStatus getStatus() {
        return status;
    }

    public PaymentId getRefundId() {
        return refundId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
