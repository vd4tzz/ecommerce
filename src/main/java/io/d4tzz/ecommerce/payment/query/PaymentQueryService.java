package io.d4tzz.ecommerce.payment.query;

import io.d4tzz.ecommerce.common.PageResponse;
import java.util.Optional;

public interface PaymentQueryService {
    PageResponse<PaymentView> searchPayments(PaymentSearchCriteria criteria);

    Optional<PaymentView> getPayment(Long id);
}
