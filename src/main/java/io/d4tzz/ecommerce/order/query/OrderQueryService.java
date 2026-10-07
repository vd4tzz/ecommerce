package io.d4tzz.ecommerce.order.query;

import io.d4tzz.ecommerce.query.PageResponse;
import java.util.Optional;

public interface OrderQueryService {
    PageResponse<OrderSummaryView> searchOrders(OrderSearchCriteria criteria);

    Optional<OrderView> getOrder(Long id);

    /** Empty when the order does not exist or belongs to another user. */
    Optional<OrderView> getUserOrder(Long userId, String orderCode);
}
