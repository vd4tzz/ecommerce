package io.d4tzz.ecommerce.order.application;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.user.UserId;
import io.d4tzz.ecommerce.order.application.command.PayMyOrderCommand;
import io.d4tzz.ecommerce.order.application.port.PaymentGateway;
import io.d4tzz.ecommerce.order.application.port.PaymentGateway.PaymentRedirect;
import io.d4tzz.ecommerce.order.application.port.PaymentGateway.PaymentRequest;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderCode;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Where to send the customer to pay their prepaid order through the gateway they picked. Changes nothing, so the
 * customer can ask again (closed the page, cancelled, payment failed) until the order is paid or the time to pay is over.
 * The order becomes paid only when the gateway reports the money, never because the customer came back to the
 * shop's success page.
 */
@Service
@Transactional(readOnly = true)
public class PayMyOrderUseCase {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    public PayMyOrderUseCase(OrderRepository orderRepository, PaymentGateway paymentGateway) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
    }

    public PaymentRedirect execute(PayMyOrderCommand command) {
        NotFoundException notFound = new NotFoundException("Order not found with code: " + command.orderCode());
        Order order = orderRepository.findByOrderCode(OrderCode.of(command.orderCode())).orElseThrow(() -> notFound);

        // Someone else's order is reported as missing, not as forbidden
        if (!order.getUserId().equals(UserId.of(command.userId()))) {
            throw notFound;
        }

        order.ensureAwaitingPayment(Instant.now());
        return paymentGateway.request(
            new PaymentRequest(
                order.getOrderCode().toString(),
                order.getTotalAmount(),
                order.getPaymentExpiresAt(),
                command.clientIp(),
                command.gateway()
            )
        );
    }
}
