package io.d4tzz.ecommerce.spring.security;

import io.d4tzz.ecommerce.user.infrastructure.security.JwtTokenParser;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenParser tokenParser) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth ->
                auth
                    .requestMatchers("/auth/**", "/error")
                    .permitAll()
                    // Authenticated by the shared secret in the X-Secret-Key header, not by a user token
                    .requestMatchers("/payment/sepay/ipn")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/products/**", "/categories/**", "/payment/gateways")
                    .permitAll()
                    .requestMatchers("/admin/users/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/admin/**")
                    .hasAnyRole("ADMIN", "STAFF")
                    .anyRequest()
                    .authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(tokenParser), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(handling ->
                handling
                    .authenticationEntryPoint((request, response, ex) ->
                        writeProblem(response, HttpStatus.UNAUTHORIZED, "Authentication required")
                    )
                    .accessDeniedHandler((request, response, ex) ->
                        writeProblem(
                            response,
                            HttpStatus.FORBIDDEN,
                            "You do not have permission to access this resource"
                        )
                    )
            );
        return http.build();
    }

    /** Users authenticate with JWTs, not username/password, so stop Spring Boot from creating a default user. */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }

    private static void writeProblem(HttpServletResponse response, HttpStatus status, String detail)
        throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response
            .getWriter()
            .write(
                "{\"type\":\"about:blank\",\"title\":\"" +
                    status.getReasonPhrase() +
                    "\",\"status\":" +
                    status.value() +
                    ",\"detail\":\"" +
                    detail +
                    "\"}"
            );
    }
}
