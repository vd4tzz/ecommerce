package io.d4tzz.ecommerce.domain.security;

public record OtpId(Long value) {

    public static OtpId of(Long value) {
        return new OtpId(value);
    }
}
