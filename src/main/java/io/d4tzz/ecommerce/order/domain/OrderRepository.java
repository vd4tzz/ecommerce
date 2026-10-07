package io.d4tzz.ecommerce.order.domain;

import io.d4tzz.ecommerce.common.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends Repository<Order, OrderId> {

    /** Locks the order row until the end of the transaction. */
    Optional<Order> findForUpdate(OrderId id);

    Optional<Order> findByOrderCode(OrderCode orderCode);

    /** Like {@link #findForUpdate}, but by order code. */
    Optional<Order> findForUpdateByOrderCode(OrderCode orderCode);

    /** Prepaid orders still PENDING and UNPAID whose payment deadline is before {@code now}. */
    List<OrderId> findPaymentExpiredIds(Instant now);
}
