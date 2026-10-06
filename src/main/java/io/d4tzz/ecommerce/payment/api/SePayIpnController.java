package io.d4tzz.ecommerce.payment.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.d4tzz.ecommerce.payment.application.RecordPaymentUseCase;
import io.d4tzz.ecommerce.payment.application.command.RecordPaymentCommand;
import io.d4tzz.ecommerce.payment.domain.PaymentMethod;
import io.d4tzz.ecommerce.payment.infrastructure.sepay.SePayProperties;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Where SePay reports payments (IPN, a webhook). Register {@code https://<host>/payment/sepay/ipn} in the SePay
 * dashboard with auth type SECRET_KEY.
 *
 * <p>Anyone can call this URL, so a request without the right {@code X-Secret-Key} is refused. SePay sends the
 * notification again until it gets a 200, so the same payment can arrive more than once; recording is idempotent
 * per transaction, so a repeat changes nothing.
 *
 * @see <a href="https://developer.sepay.vn/en/cong-thanh-toan/IPN">IPN</a>
 */
@RestController
@Tag(name = "Payment - SePay IPN", description = "Payment notifications sent by SePay")
public class SePayIpnController {

    private static final Logger log = LoggerFactory.getLogger(SePayIpnController.class);

    private final RecordPaymentUseCase recordPaymentUseCase;
    private final SePayProperties properties;

    public SePayIpnController(RecordPaymentUseCase recordPaymentUseCase, SePayProperties properties) {
        this.recordPaymentUseCase = recordPaymentUseCase;
        this.properties = properties;
    }

    @PostMapping("/payment/sepay/ipn")
    public ResponseEntity<Map<String, Object>> receive(
        @RequestHeader(name = "X-Secret-Key", required = false) String secretKey,
        @RequestBody SePayIpn ipn
    ) {
        if (!isOurSecret(secretKey)) {
            log.warn("SePay IPN refused: wrong or missing secret key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false));
        }

        if (!"ORDER_PAID".equals(ipn.notificationType())) {
            // e.g. TRANSACTION_VOID; nothing handles it yet
            log.info("SePay IPN {} ignored: {}", ipn.notificationType(), ipn);
            return ResponseEntity.ok(Map.of("success", true));
        }

        if (
            ipn.order() == null ||
            ipn.transaction() == null ||
            ipn.transaction().amount() == null ||
            ipn.transaction().transactionId() == null
        ) {
            // Sending it again will not fix it, so log the body for a person to look at
            log.error("SePay IPN ORDER_PAID without order or transaction details: {}", ipn);
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }

        recordPaymentUseCase.execute(
            new RecordPaymentCommand(
                ipn.order().invoiceNumber(),
                PaymentMethod.SEPAY,
                properties.merchant(),
                new BigDecimal(ipn.transaction().amount()),
                ipn.transaction().transactionId(),
                "SePay " + ipn.transaction().paymentMethod()
            )
        );
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** Compares in constant time, so the response time does not hint at how much of the key was right. */
    private boolean isOurSecret(String secretKey) {
        return (
            secretKey != null &&
            MessageDigest.isEqual(
                secretKey.getBytes(StandardCharsets.UTF_8),
                properties.secretKey().getBytes(StandardCharsets.UTF_8)
            )
        );
    }

    /** The parts of SePay's IPN body we use; other fields are ignored. */
    record SePayIpn(
        @JsonProperty("notification_type") String notificationType,
        Order order,
        Transaction transaction
    ) {
        record Order(@JsonProperty("order_invoice_number") String invoiceNumber) {}

        record Transaction(
            @JsonProperty("transaction_id") String transactionId,
            @JsonProperty("transaction_amount") String amount,
            @JsonProperty("payment_method") String paymentMethod
        ) {}
    }
}
