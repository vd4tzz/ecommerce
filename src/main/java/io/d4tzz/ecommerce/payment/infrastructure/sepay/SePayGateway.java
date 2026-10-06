package io.d4tzz.ecommerce.payment.infrastructure.sepay;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.payment.application.port.PaymentGateway;
import io.d4tzz.ecommerce.payment.application.port.PaymentRedirect;
import io.d4tzz.ecommerce.payment.application.port.PaymentRequest;
import io.d4tzz.ecommerce.payment.domain.PaymentMethod;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * SePay's hosted checkout. The form is signed with HMAC-SHA256 over {@code name=value} pairs joined by commas,
 * in the order of {@link #SIGNED_FIELDS} (fields left out are skipped), then Base64-encoded. SePay rebuilds the
 * same string and rejects the form if the result differs, so a customer cannot change the amount.
 *
 * @see <a href="https://developer.sepay.vn/en/cong-thanh-toan/API/don-hang/form-thanh-toan">Create payment form</a>
 */
@Component
public class SePayGateway implements PaymentGateway {

    private static final List<String> SIGNED_FIELDS = List.of(
        "order_amount",
        "merchant",
        "currency",
        "operation",
        "order_description",
        "order_invoice_number",
        "customer_id",
        "payment_method",
        "success_url",
        "error_url",
        "cancel_url"
    );

    private final SePayProperties properties;

    public SePayGateway(SePayProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.SEPAY;
    }

    @Override
    public PaymentRedirect requestPayment(PaymentRequest request) {
        // SePay needs only the order and the amount
        String orderCode = request.orderCode();
        Money amount = request.amount();
        String orderUrl = properties.returnUrl().replaceAll("/+$", "") + "/" + orderCode;

        Map<String, String> fields = new LinkedHashMap<>();
        // VND has no smaller unit; a fraction here is a bug, so fail rather than round
        fields.put("order_amount", amount.amount().toBigIntegerExact().toString());
        fields.put("merchant", properties.merchant());
        fields.put("currency", "VND");
        fields.put("operation", "PURCHASE");
        fields.put("order_description", "Thanh toan don hang " + orderCode);
        fields.put("order_invoice_number", orderCode);
        fields.put("success_url", orderUrl + "?payment=success");
        fields.put("error_url", orderUrl + "?payment=error");
        fields.put("cancel_url", orderUrl + "?payment=cancel");
        fields.put("signature", sign(fields));

        // SePay takes a signed form, posted by the customer's browser
        return new PaymentRedirect("POST", properties.checkoutUrl(), fields);
    }

    private String sign(Map<String, String> fields) {
        List<String> pairs = new ArrayList<>();
        for (String name : SIGNED_FIELDS) {
            if (fields.containsKey(name)) {
                pairs.add(name + "=" + fields.get(name));
            }
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.secretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(String.join(",", pairs).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot sign SePay form", e);
        }
    }
}
