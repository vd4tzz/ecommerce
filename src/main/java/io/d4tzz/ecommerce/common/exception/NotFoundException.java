package io.d4tzz.ecommerce.common.exception;

public class NotFoundException extends AppException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
