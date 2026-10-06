package io.d4tzz.ecommerce.payment.api;

import io.d4tzz.ecommerce.payment.application.port.PaymentGateways;
import io.d4tzz.ecommerce.payment.domain.PaymentMethod;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Payment - Gateways", description = "Gateways a customer can pay through")
public class PaymentGatewayController {

    private final PaymentGateways paymentGateways;

    public PaymentGatewayController(PaymentGateways paymentGateways) {
        this.paymentGateways = paymentGateways;
    }

    /** The names to send as {@code gateway} when asking for a payment form, e.g. ["SEPAY"]. */
    @GetMapping("/payment/gateways")
    public ResponseEntity<List<PaymentMethod>> getGateways() {
        return ResponseEntity.ok(paymentGateways.available());
    }
}
