package io.d4tzz.ecommerce.user.infrastructure.security;

import io.d4tzz.ecommerce.user.domain.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenParser {

    public record TokenClaims(Long userId, UserRole role) {}

    private final SecretKey key;

    public JwtTokenParser(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secretKey().getBytes(StandardCharsets.UTF_8));
    }

    public Optional<Long> extractUserId(String rawToken) {
        return parse(rawToken).map(TokenClaims::userId);
    }

    public Optional<TokenClaims> parse(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        try {
            var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(rawToken.trim()).getPayload();
            if (!JwtTokenProvider.ACCESS_TOKEN_TYPE.equals(claims.get(JwtTokenProvider.TOKEN_TYPE_CLAIM, String.class))) {
                return Optional.empty();
            }
            UserRole role = UserRole.valueOf(claims.get("role", String.class));
            return Optional.of(new TokenClaims(Long.parseLong(claims.getSubject()), role));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
