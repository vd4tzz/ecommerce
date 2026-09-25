package io.d4tzz.ecommerce.rest.security;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.infrastructure.security.JwtTokenParser;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtTokenParser tokenParser;

    public CurrentUserArgumentResolver(JwtTokenParser tokenParser) {
        this.tokenParser = tokenParser;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return (
            parameter.hasParameterAnnotation(CurrentUser.class) &&
            (Long.class.isAssignableFrom(parameter.getParameterType()) ||
                long.class.isAssignableFrom(parameter.getParameterType()))
        );
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        boolean required = annotation == null || annotation.required();

        // 1. Authorization header: Bearer <jwt>
        String authHeader = webRequest.getHeader("Authorization");
        if (authHeader != null && !authHeader.isBlank()) {
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader.trim();
            var userId = tokenParser.extractUserId(token);
            if (userId.isPresent()) {
                return userId.get();
            }
        }

        // 2. Fallback: X-User-Id header (useful for testing/internal calls)
        String headerUserId = webRequest.getHeader("X-User-Id");
        if (headerUserId != null && !headerUserId.isBlank()) {
            try {
                return Long.parseLong(headerUserId.trim());
            } catch (NumberFormatException ignored) {}
        }

        // 3. Fallback: userId query parameter
        String queryUserId = webRequest.getParameter("userId");
        if (queryUserId != null && !queryUserId.isBlank()) {
            try {
                return Long.parseLong(queryUserId.trim());
            } catch (NumberFormatException ignored) {}
        }

        if (required) {
            throw new InvalidArgumentException(
                "User authentication required (provide Bearer token or X-User-Id header)"
            );
        }

        return null;
    }
}
