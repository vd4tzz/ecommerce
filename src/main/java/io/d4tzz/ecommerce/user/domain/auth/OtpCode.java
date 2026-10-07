package io.d4tzz.ecommerce.user.domain.auth;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.security.SecureRandom;
import java.util.regex.Pattern;

public record OtpCode(String value) {

    private static final Pattern PATTERN = Pattern.compile("^\\d{6}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    public OtpCode {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new InvalidArgumentException("OTP code must be a 6-digit number");
        }
    }

    public static OtpCode of(String value) {
        return new OtpCode(value);
    }

    public static OtpCode generate() {
        int code = RANDOM.nextInt(1_000_000);
        return new OtpCode(String.format("%06d", code));
    }
}
