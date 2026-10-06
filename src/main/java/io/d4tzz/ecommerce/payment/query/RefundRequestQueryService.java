package io.d4tzz.ecommerce.payment.query;

import io.d4tzz.ecommerce.query.PageResponse;
import java.util.Optional;

public interface RefundRequestQueryService {
    /** Oldest first: they have waited longest. */
    PageResponse<RefundRequestView> searchRefundRequests(RefundRequestSearchCriteria criteria);

    Optional<RefundRequestView> getRefundRequest(Long id);
}
