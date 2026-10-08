package io.d4tzz.ecommerce.spring.security;

import io.d4tzz.ecommerce.user.infrastructure.security.JwtTokenParser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the Bearer access token and, when valid, authenticates the request with the user id as principal and the
 * user's role (e.g. ROLE_ADMIN) as the only authority. Requests without a valid token stay anonymous and are
 * rejected later by the authorization rules in {@link WebSecurityConfig}.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

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
                                List.of(new SimpleGrantedAuthority(claims.role().name()))
                            )
                        )
                );
        }
        chain.doFilter(request, response);
    }
}
