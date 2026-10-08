package io.d4tzz.ecommerce.user.domain.auth;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.user.domain.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    @Test
    void hash_isSha256Hex() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", RefreshToken.hash("abc"));
    }

    @Test
    void hash_rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> RefreshToken.hash(" "));
        assertThrows(IllegalArgumentException.class, () -> RefreshToken.hash(null));
    }

    @Test
    void create_storesOnlyHashAndStartsValid() {
        RefreshToken token = RefreshToken.create(UserId.of(1L), "raw", Duration.ofDays(7));

        assertEquals(RefreshToken.hash("raw"), token.getTokenHash());
        assertEquals("raw", token.getRawToken());
        assertNotNull(token.getFamilyId());
        assertTrue(token.isValid());
    }

    @Test
    void rotate_revokesCurrentAndKeepsFamily() {
        RefreshToken current = RefreshToken.create(UserId.of(1L), "old", Duration.ofDays(7));

        RefreshToken next = current.rotate("new", Duration.ofDays(7));

        assertTrue(current.isRevoked());
        assertFalse(current.isValid());
        assertEquals(current.getFamilyId(), next.getFamilyId());
        assertEquals(RefreshToken.hash("new"), next.getTokenHash());
        assertTrue(next.isValid());
    }

    @Test
    void expiredOrRevokedTokenIsInvalid() {
        RefreshToken expired = new RefreshToken(
            RefreshTokenId.of(1L), UserId.of(1L), "h", UUID.randomUUID(), false,
            Instant.now().minusSeconds(1), Instant.now().minusSeconds(60)
        );

        assertTrue(expired.isExpired());
        assertFalse(expired.isValid());
    }
}
