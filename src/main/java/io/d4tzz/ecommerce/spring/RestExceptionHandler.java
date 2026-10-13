package io.d4tzz.ecommerce.spring;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.inventory.domain.OutOfStockException;
import io.d4tzz.ecommerce.user.domain.AccountBlockedException;
import io.d4tzz.ecommerce.user.domain.EmailAlreadyExistsException;
import io.d4tzz.ecommerce.user.domain.InvalidCredentialsException;
import io.d4tzz.ecommerce.user.domain.auth.InvalidRefreshTokenException;
import io.d4tzz.ecommerce.user.domain.auth.OtpInvalidCodeException;
import io.d4tzz.ecommerce.user.domain.auth.OtpInvalidException;
import io.d4tzz.ecommerce.user.domain.auth.OtpRateLimitedException;
import io.d4tzz.ecommerce.user.domain.auth.RefreshTokenReusedException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
    }

    /** Bean Validation errors from {@link SpringValidation}, with one entry per invalid field. */
    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidation(ValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        List<Map<String, String>> errors = ex
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error ->
                Map.of(
                    "field",
                    error.getField(),
                    "message",
                    error.getDefaultMessage() != null ? error.getDefaultMessage() : "is invalid"
                )
            )
            .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
    }

    @ExceptionHandler({
        InvalidArgumentException.class,
        InvalidOperationException.class,
        IllegalArgumentException.class,
        IllegalStateException.class
    })
    public ProblemDetail handleBadRequest(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(OutOfStockException.class)
    public ProblemDetail handleOutOfStock(OutOfStockException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailConflict(EmailAlreadyExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler({InvalidRefreshTokenException.class, RefreshTokenReusedException.class})
    public ProblemDetail handleInvalidRefreshToken(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccountBlockedException.class)
    public ProblemDetail handleAccountBlocked(AccountBlockedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler({OtpInvalidCodeException.class, OtpInvalidException.class})
    public ProblemDetail handleOtpInvalid(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(OtpRateLimitedException.class)
    public ProblemDetail handleOtpRateLimit(OtpRateLimitedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }
}
