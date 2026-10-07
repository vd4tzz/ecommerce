package io.d4tzz.ecommerce.user.domain.auth;

import io.d4tzz.ecommerce.common.exception.AppException;

public class OtpRateLimitedException extends AppException {
    public OtpRateLimitedException(String message) {
        super(message);
    }
}
