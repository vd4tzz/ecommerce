package io.d4tzz.ecommerce.user.domain.auth;

public record OtpId(Long value) {

    public static OtpId of(Long value) {
        return new OtpId(value);
    }
}
