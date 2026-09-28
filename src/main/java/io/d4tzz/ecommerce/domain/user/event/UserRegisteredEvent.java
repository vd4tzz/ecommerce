package io.d4tzz.ecommerce.domain.user.event;

import io.d4tzz.ecommerce.domain.security.OtpCode;
import io.d4tzz.ecommerce.domain.user.Email;
import io.d4tzz.ecommerce.domain.user.UserId;

public record UserRegisteredEvent(
    UserId userId,
    Email email,
    OtpCode otpCode
) {}
