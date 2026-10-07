package io.d4tzz.ecommerce.user.domain;

import io.d4tzz.ecommerce.common.exception.AppException;

public class AccountBlockedException extends AppException {
    public AccountBlockedException(String message) {
        super(message);
    }
}
