package io.d4tzz.ecommerce.order.domain;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.user.domain.PhoneNumber;

public record Recipient(String fullName, PhoneNumber phoneNumber) {
    public Recipient {
        if (fullName.isBlank()) {
            throw new InvalidArgumentException("Recipient full name must not be blank");
        }
        fullName = fullName.trim();
    }
}
