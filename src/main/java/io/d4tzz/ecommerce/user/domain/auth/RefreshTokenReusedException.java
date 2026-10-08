package io.d4tzz.ecommerce.user.domain.auth;

import io.d4tzz.ecommerce.common.exception.AppException;

public class RefreshTokenReusedException extends AppException {

    public RefreshTokenReusedException(String message) {
        super(message);
    }
}
