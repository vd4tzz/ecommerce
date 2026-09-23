package io.d4tzz.ecommerce.domain.security;

import io.d4tzz.ecommerce.domain.user.User;

public interface TokenProvider {

    TokenPair generateTokens(User user);

    TokenPair rotate(RefreshToken currentToken, User user);
}
