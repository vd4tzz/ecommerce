package io.d4tzz.ecommerce.user.domain.event;

import io.d4tzz.ecommerce.user.domain.Email;
import io.d4tzz.ecommerce.user.domain.UserId;

public record EmailVerifiedEvent(
    UserId userId,
    Email email
) {}
