package io.d4tzz.ecommerce.payment.infrastructure.sepay;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param merchant    merchant id given by SePay
 * @param secretKey   signs checkout forms, and is the {@code X-Secret-Key} SePay sends with each IPN
 * @param checkoutUrl where the checkout form is posted (sandbox or production)
 * @param returnUrl   frontend page of an order; SePay sends the customer back to {@code <returnUrl>/<orderCode>}
 */
@ConfigurationProperties(prefix = "app.payment.sepay")
public record SePayProperties(String merchant, String secretKey, String checkoutUrl, String returnUrl) {
    public SePayProperties {
        Objects.requireNonNull(merchant, "merchant must not be null");
        Objects.requireNonNull(secretKey, "secretKey must not be null");
        Objects.requireNonNull(checkoutUrl, "checkoutUrl must not be null");
        Objects.requireNonNull(returnUrl, "returnUrl must not be null");
    }
}
