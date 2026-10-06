package io.d4tzz.ecommerce.payment.application.port;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.payment.domain.PaymentMethod;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Every gateway adapter Spring finds, by the name of the payment method it handles (e.g. "SEPAY"). */
@Component
public class PaymentGateways {

    private final Map<String, PaymentGateway> gatewaysByName;

    public PaymentGateways(List<PaymentGateway> gateways) {
        // Two adapters for the same method fail here, at startup
        this.gatewaysByName = gateways
            .stream()
            .collect(Collectors.toMap(gateway -> gateway.method().name(), Function.identity()));
    }

    /** The methods a customer can pick, e.g. to show one button per gateway. */
    public List<PaymentMethod> available() {
        return gatewaysByName.values().stream().map(PaymentGateway::method).sorted(Comparator.comparing(Enum::name)).toList();
    }

    /** @param name a payment method name, any case, e.g. "sepay" */
    public PaymentGateway get(String name) {
        // Enum names are upper case; Locale.ROOT so the result does not depend on the server's language
        PaymentGateway gateway = gatewaysByName.get(name.trim().toUpperCase(Locale.ROOT));
        if (gateway == null) {
            throw new InvalidOperationException("Payment gateway " + name + " is not available");
        }
        return gateway;
    }
}
