package io.d4tzz.ecommerce.common.exception;

public class ConflictException extends AppException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
