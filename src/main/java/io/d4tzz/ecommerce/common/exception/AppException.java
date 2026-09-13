package io.d4tzz.ecommerce.common.exception;

public class AppException extends RuntimeException {

    private final String code;

    public AppException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public AppException(String code, String message) {
        this(code, message, null);
    }

    public AppException(String message, Throwable cause) {
        this(null, message, cause);
    }

    public AppException(String message) {
        this(null, message, null);
    }

    public AppException(Throwable cause) {
        this(null, cause != null ? cause.getMessage() : null, cause);
    }

    public String getCode() {
        return code;
    }
}
