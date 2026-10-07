package io.d4tzz.ecommerce.user.domain;

import io.d4tzz.ecommerce.common.exception.AppException;

public class InvalidCredentialsException extends AppException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
