package io.d4tzz.ecommerce.spring.security;

import io.d4tzz.ecommerce.user.infrastructure.security.JwtTokenParser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the Bearer access token and, when valid, authenticates the request with the user id as principal and these
 * authorities: the user's role (e.g. ROLE_ADMIN), and {@link #EMAIL_VERIFIED_AUTHORITY} when the token says the email
 * was verified. Requests without a valid token stay anonymous and are rejected later by the authorization rules in
 * {@link WebSecurityConfig}.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Granted when the access token says the user's email is verified. */
    public static final String EMAIL_VERIFIED_AUTHORITY = "EMAIL_VERIFIED";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenParser tokenParser;

    public JwtAuthenticationFilter(JwtTokenParser tokenParser) {
        this.tokenParser = tokenParser;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            tokenParser
                .parse(header.substring(BEARER_PREFIX.length()).trim())
                .ifPresent(claims ->
                    SecurityContextHolder.getContext()
                        .setAuthentication(
                            UsernamePasswordAuthenticationToken.authenticated(
                                claims.userId(),
                                null,
                                authoritiesOf(claims)
                            )
                        )
                );
        }
        chain.doFilter(request, response);
    }

    private static List<GrantedAuthority> authoritiesOf(JwtTokenParser.TokenClaims claims) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(claims.role().name()));
        if (claims.emailVerified()) {
            authorities.add(new SimpleGrantedAuthority(EMAIL_VERIFIED_AUTHORITY));
        }
        return authorities;
    }
}
