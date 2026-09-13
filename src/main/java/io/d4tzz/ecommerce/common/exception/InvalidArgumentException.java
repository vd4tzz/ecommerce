package io.d4tzz.ecommerce.common.exception;

public class InvalidArgumentException extends AppException {

    public InvalidArgumentException(String message) {
        super(message);
    }

    public InvalidArgumentException(String code, String message) {
        super(code, message);
    }
}
