package io.d4tzz.ecommerce.user.domain.auth;

import io.d4tzz.ecommerce.common.exception.AppException;

public class OtpInvalidException extends AppException {
    public OtpInvalidException(String message) {
        super(message);
    }
}
