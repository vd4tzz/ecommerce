package io.d4tzz.ecommerce.user.domain;

public final class UserFixtures {

    public static final String PASSWORD = "Secret123";

    private UserFixtures() {}

    public static User user(UserRole role, UserStatus status, boolean emailVerified) {
        return new User(
            UserId.of(1L),
            Email.of("john@example.com"),
            new FakePasswordHasher().hash(PASSWORD),
            FullName.of("John Doe"),
            null,
            null,
            null,
            role,
            status,
            emailVerified,
            null,
            null,
            null
        );
    }

    public static User activeCustomer() {
        return user(UserRole.ROLE_CUSTOMER, UserStatus.ACTIVE, true);
    }
}
