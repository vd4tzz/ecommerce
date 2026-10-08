package io.d4tzz.ecommerce.user.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.user.domain.auth.OtpCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ValueObjectsTest {

    @Test
    void email_isTrimmedAndLowercased() {
        assertEquals("john@example.com", Email.of("  John@Example.COM ").value());
        assertEquals(Email.of("a@b.com"), Email.of("A@B.com"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "plain", "a@b", "a@b.c", "@b.com", "a b@c.com" })
    void email_rejectsInvalid(String value) {
        assertThrows(InvalidArgumentException.class, () -> Email.of(value));
    }

    @Test
    void phoneNumber_stripsSpacesDotsAndDashes() {
        assertEquals("0912345678", PhoneNumber.of("0912 345.678").value());
        assertEquals("+84912345678", PhoneNumber.of("+84-912-345-678").value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "12345", "abcdefghij", "0912345678901234" })
    void phoneNumber_rejectsInvalid(String value) {
        assertThrows(InvalidArgumentException.class, () -> PhoneNumber.of(value));
    }

    @Test
    void hashedPassword_rejectsBlank() {
        assertThrows(InvalidArgumentException.class, () -> HashedPassword.of(" "));
        assertThrows(InvalidArgumentException.class, () -> HashedPassword.of(null));
    }

    @Test
    void otpCode_acceptsSixDigits() {
        assertEquals("012345", OtpCode.of("012345").value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "12345", "1234567", "12345a", " 12345" })
    void otpCode_rejectsInvalid(String value) {
        assertThrows(InvalidArgumentException.class, () -> OtpCode.of(value));
    }

    @Test
    void otpCode_generateAlwaysProducesSixDigits() {
        for (int i = 0; i < 200; i++) {
            assertTrue(OtpCode.generate().value().matches("\\d{6}"));
        }
    }
}
