package io.d4tzz.ecommerce.user.domain.auth;

import io.d4tzz.ecommerce.common.exception.AppException;

public class OtpInvalidCodeException extends AppException {
    public OtpInvalidCodeException(String message) {
        super(message);
    }
}
