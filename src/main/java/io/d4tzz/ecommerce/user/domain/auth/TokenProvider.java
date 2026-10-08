package io.d4tzz.ecommerce.user.domain.auth;

import io.d4tzz.ecommerce.user.domain.User;

public interface TokenProvider {
    TokenPair generateTokens(User user);

    TokenPair rotate(RefreshToken currentToken, User user);
}
