package io.d4tzz.ecommerce.payment.domain;

import java.util.Optional;

public interface RefundRequestRepository {
    /** Locks the request row until the end of the transaction. */
    Optional<RefundRequest> findForUpdate(RefundRequestId id);

    boolean existsPending(PaymentId paymentId);

    /** Inserts a new request, or updates an existing one; returns it with its id. */
    RefundRequest save(RefundRequest request);
}
