package io.d4tzz.ecommerce.user.query;

public record UserSearchCriteria(
    String keyword,
    String role,
    String status,
    Boolean emailVerified,
    int page,
    int size
) {}
