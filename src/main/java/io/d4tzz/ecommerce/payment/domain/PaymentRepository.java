package io.d4tzz.ecommerce.payment.domain;

import io.d4tzz.ecommerce.common.Money;
import java.util.Optional;

/** Payments are never deleted, and never updated except by {@link #saveOrderReference}. */
public interface PaymentRepository {
    Optional<Payment> find(PaymentId id);

    /** Locks the payment row until the end of the transaction. */
    Optional<Payment> findForUpdate(PaymentId id);

    /** Finds the record of one transfer. See {@link Payment} for why all three values are needed. */
    Optional<Payment> findByTransaction(PaymentMethod method, String shopAccount, String transactionNo);

    /** Sum of the refunds recorded against the given payment. */
    Money refundedAmount(PaymentId paymentId);

    /** Stores the order given to an unmatched payment (see {@link Payment#assignToOrder}). */
    void saveOrderReference(Payment payment);

    /** Inserts a new payment and returns it with its id. */
    Payment add(Payment payment);
}
