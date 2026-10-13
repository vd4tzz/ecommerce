package io.d4tzz.ecommerce.spring.security;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserFixtures;
import io.d4tzz.ecommerce.user.domain.UserRole;
import io.d4tzz.ecommerce.user.domain.UserStatus;
import io.d4tzz.ecommerce.user.infrastructure.security.JwtProperties;
import io.d4tzz.ecommerce.user.infrastructure.security.JwtTokenParser;
import io.d4tzz.ecommerce.user.infrastructure.security.JwtTokenProvider;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "unit-test-secret-key-with-at-least-32-bytes!";

    private final JwtProperties properties = new JwtProperties(SECRET, Duration.ofMinutes(15), Duration.ofDays(7));
    private final JwtTokenProvider provider = new JwtTokenProvider(properties);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtTokenParser(properties));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Authentication filterRequestWith(String authorizationHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private String bearerTokenOf(User user) {
        return "Bearer " + provider.generateTokens(user).accessToken().value();
    }

    private static Set<String> authoritiesOf(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    @Test
    void aVerifiedUserGetsTheirRoleAndTheEmailVerifiedAuthority() throws Exception {
        Authentication authentication = filterRequestWith(bearerTokenOf(UserFixtures.activeCustomer()));

        assertEquals(1L, authentication.getPrincipal());
        assertEquals(Set.of("ROLE_CUSTOMER", JwtAuthenticationFilter.EMAIL_VERIFIED_AUTHORITY), authoritiesOf(authentication));
    }

    @Test
    void anUnverifiedUserOnlyGetsTheirRole() throws Exception {
        User unverified = UserFixtures.user(UserRole.ROLE_CUSTOMER, UserStatus.ACTIVE, false);

        Authentication authentication = filterRequestWith(bearerTokenOf(unverified));

        assertTrue(authentication.isAuthenticated());
        assertEquals(Set.of("ROLE_CUSTOMER"), authoritiesOf(authentication));
    }

    @Test
    void theRoleOfAnAdminIsKept() throws Exception {
        User admin = UserFixtures.user(UserRole.ROLE_ADMIN, UserStatus.ACTIVE, true);

        Authentication authentication = filterRequestWith(bearerTokenOf(admin));

        assertTrue(authoritiesOf(authentication).contains("ROLE_ADMIN"));
    }

    @Test
    void aRequestWithoutATokenStaysAnonymous() throws Exception {
        assertNull(filterRequestWith(null));
    }

    @Test
    void aGarbageTokenStaysAnonymous() throws Exception {
        assertNull(filterRequestWith("Bearer not-a-jwt"));
    }

    @Test
    void aHeaderThatIsNotBearerStaysAnonymous() throws Exception {
        assertNull(filterRequestWith("Basic abc"));
    }
}
