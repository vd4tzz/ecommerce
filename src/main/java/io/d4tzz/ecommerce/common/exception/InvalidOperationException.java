package io.d4tzz.ecommerce.common.exception;

public class InvalidOperationException extends AppException {

    public InvalidOperationException(String message) {
        super(message);
    }

    public InvalidOperationException(String code, String message) {
        super(code, message);
    }
}
