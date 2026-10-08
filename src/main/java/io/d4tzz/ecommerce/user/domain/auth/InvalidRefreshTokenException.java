package io.d4tzz.ecommerce.user.domain.auth;

import io.d4tzz.ecommerce.common.exception.AppException;

public class InvalidRefreshTokenException extends AppException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
