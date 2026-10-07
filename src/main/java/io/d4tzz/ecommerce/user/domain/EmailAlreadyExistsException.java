package io.d4tzz.ecommerce.user.domain;

import io.d4tzz.ecommerce.common.exception.AppException;

public class EmailAlreadyExistsException extends AppException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
