package io.d4tzz.ecommerce.user.domain.event;

import io.d4tzz.ecommerce.user.domain.Email;
import io.d4tzz.ecommerce.user.domain.auth.OtpCode;
import io.d4tzz.ecommerce.user.domain.UserId;

public record EmailVerificationRequestedEvent(
    UserId userId,
    Email email,
    OtpCode otpCode
) {}
