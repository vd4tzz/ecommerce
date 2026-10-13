package io.d4tzz.ecommerce.spring.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserFixtures;
import io.d4tzz.ecommerce.user.domain.UserRole;
import io.d4tzz.ecommerce.user.domain.UserStatus;
import io.d4tzz.ecommerce.user.infrastructure.security.JwtProperties;
import io.d4tzz.ecommerce.user.infrastructure.security.JwtTokenParser;
import io.d4tzz.ecommerce.user.infrastructure.security.JwtTokenProvider;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The route rules of {@link WebSecurityConfig}, with real tokens, against a controller that answers 200 to every path. */
@WebMvcTest(controllers = EmailVerifiedAccessTest.ProbeController.class)
@Import(
    {
        WebSecurityConfig.class,
        JwtTokenParser.class,
        EmailVerifiedAccessTest.TokenConfig.class,
        EmailVerifiedAccessTest.ProbeController.class,
    }
)
class EmailVerifiedAccessTest {

    private static final JwtProperties PROPERTIES = new JwtProperties(
        "unit-test-secret-key-with-at-least-32-bytes!",
        Duration.ofMinutes(15),
        Duration.ofDays(7)
    );

    @RestController
    static class ProbeController {

        @GetMapping({ "/cart", "/checkout", "/orders", "/orders/ORD1", "/users/me", "/admin/orders" })
        String ok() {
            return "ok";
        }
    }

    @TestConfiguration
    static class TokenConfig {

        @Bean
        JwtProperties jwtProperties() {
            return PROPERTIES;
        }
    }

    @Autowired MockMvc mockMvc;

    private static String bearerTokenOf(UserRole role, boolean emailVerified) {
        User user = UserFixtures.user(role, UserStatus.ACTIVE, emailVerified);
        return "Bearer " + new JwtTokenProvider(PROPERTIES).generateTokens(user).accessToken().value();
    }

    private int statusOf(String path, String token) throws Exception {
        var request = get(path);
        if (token != null) {
            request = request.header("Authorization", token);
        }
        return mockMvc.perform(request).andReturn().getResponse().getStatus();
    }

    @Test
    void aVerifiedCustomerCanUseCartCheckoutAndOrders() throws Exception {
        String token = bearerTokenOf(UserRole.ROLE_CUSTOMER, true);

        for (String path : new String[] { "/cart", "/checkout", "/orders", "/orders/ORD1" }) {
            assertEquals(200, statusOf(path, token), path);
        }
    }

    @Test
    void anUnverifiedCustomerIsRefusedOnCartCheckoutAndOrders() throws Exception {
        String token = bearerTokenOf(UserRole.ROLE_CUSTOMER, false);

        for (String path : new String[] { "/cart", "/checkout", "/orders", "/orders/ORD1" }) {
            assertEquals(403, statusOf(path, token), path);
        }
    }

    @Test
    void anUnverifiedCustomerCanStillUseTheirAccount() throws Exception {
        assertEquals(200, statusOf("/users/me", bearerTokenOf(UserRole.ROLE_CUSTOMER, false)));
    }

    @Test
    void withoutATokenTheseRoutesAreRefusedAsUnauthenticated() throws Exception {
        for (String path : new String[] { "/cart", "/checkout", "/orders", "/users/me" }) {
            assertEquals(401, statusOf(path, null), path);
        }
    }

    @Test
    void adminRoutesDoNotDependOnAVerifiedEmail() throws Exception {
        assertEquals(200, statusOf("/admin/orders", bearerTokenOf(UserRole.ROLE_ADMIN, false)));
        assertEquals(403, statusOf("/admin/orders", bearerTokenOf(UserRole.ROLE_CUSTOMER, true)));
    }
}
