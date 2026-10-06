package io.d4tzz.ecommerce.payment.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * {@code gateway} is the name of a {@link io.d4tzz.ecommerce.payment.domain.PaymentMethod} with a gateway, e.g. SEPAY.
 * {@code expiresAt} and {@code clientIp} may be null; see {@link io.d4tzz.ecommerce.payment.application.port.PaymentRequest}.
 */
public record RequestPaymentCommand(
    @NotBlank String orderCode,
    @NotNull @Positive BigDecimal amount,
    Instant expiresAt,
    String clientIp,
    @NotBlank String gateway
) {}
