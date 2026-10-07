package io.d4tzz.ecommerce.order.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * {@code gateway} is the one the customer picked, e.g. SEPAY (see GET /payment/gateways). {@code clientIp} is the
 * customer's IP address, which some gateways require.
 */
public record PayMyOrderCommand(
    @NotNull Long userId,
    @NotBlank String orderCode,
    @NotBlank String gateway,
    @NotBlank String clientIp
) {}
