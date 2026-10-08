package io.d4tzz.ecommerce.user.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserFixtures;
import io.d4tzz.ecommerce.user.domain.auth.TokenPair;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JwtTokenTest {

    private static final String SECRET = "unit-test-secret-key-with-at-least-32-bytes!";

    private final JwtProperties properties = new JwtProperties(SECRET, Duration.ofMinutes(15), Duration.ofDays(7));
    private final JwtTokenProvider provider = new JwtTokenProvider(properties);
    private final JwtTokenParser parser = new JwtTokenParser(properties);
    private final User user = UserFixtures.activeCustomer();

    @Test
    void accessToken_isAcceptedByParser() {
        TokenPair pair = provider.generateTokens(user);

        assertEquals(Optional.of(1L), parser.extractUserId(pair.accessToken().value()));
    }

    @Test
    void refreshToken_isNotAcceptedAsAccessToken() {
        TokenPair pair = provider.generateTokens(user);

        assertEquals(Optional.empty(), parser.extractUserId(pair.refreshToken().getRawToken()));
    }

    @Test
    void rotate_keepsFamilyAndRevokesCurrent() {
        TokenPair first = provider.generateTokens(user);

        TokenPair second = provider.rotate(first.refreshToken(), user);

        assertTrue(first.refreshToken().isRevoked());
        assertEquals(first.refreshToken().getFamilyId(), second.refreshToken().getFamilyId());
        assertNotEquals(first.refreshToken().getRawToken(), second.refreshToken().getRawToken());
    }

    @Test
    void parser_rejectsBlankGarbageAndForeignSignature() {
        String foreign = Jwts.builder()
            .subject("1")
            .claim(JwtTokenProvider.TOKEN_TYPE_CLAIM, JwtTokenProvider.ACCESS_TOKEN_TYPE)
            .signWith(Keys.hmacShaKeyFor("another-secret-key-with-at-least-32-bytes!".getBytes(StandardCharsets.UTF_8)))
            .compact();

        assertEquals(Optional.empty(), parser.extractUserId(null));
        assertEquals(Optional.empty(), parser.extractUserId(" "));
        assertEquals(Optional.empty(), parser.extractUserId("not-a-jwt"));
        assertEquals(Optional.empty(), parser.extractUserId(foreign));
    }

    @Test
    void parser_rejectsExpiredToken() {
        String expired = Jwts.builder()
            .subject("1")
            .claim(JwtTokenProvider.TOKEN_TYPE_CLAIM, JwtTokenProvider.ACCESS_TOKEN_TYPE)
            .expiration(new Date(System.currentTimeMillis() - 1000))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();

        assertEquals(Optional.empty(), parser.extractUserId(expired));
    }
}
