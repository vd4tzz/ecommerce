package io.d4tzz.ecommerce.domain.user.event;

import io.d4tzz.ecommerce.domain.user.Email;
import io.d4tzz.ecommerce.domain.user.UserId;

public record EmailVerifiedEvent(
    UserId userId,
    Email email
) {}
