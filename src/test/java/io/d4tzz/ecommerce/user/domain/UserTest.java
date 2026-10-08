package io.d4tzz.ecommerce.user.domain;

import static io.d4tzz.ecommerce.user.domain.UserFixtures.PASSWORD;
import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class UserTest {

    private final PasswordHasher hasher = new FakePasswordHasher();

    @Test
    void register_createsActiveUnverifiedCustomerWithHashedPassword() {
        User user = User.register(Email.of("new@example.com"), PASSWORD, FullName.of("New User"), e -> false, hasher);

        assertNull(user.getId());
        assertEquals(UserRole.ROLE_CUSTOMER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertFalse(user.isEmailVerified());
        assertEquals(hasher.hash(PASSWORD), user.getPassword());
    }

    @Test
    void register_rejectsExistingEmail() {
        assertThrows(
            EmailAlreadyExistsException.class,
            () -> User.register(Email.of("dup@example.com"), PASSWORD, FullName.of("Dup"), e -> true, hasher)
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "Sh0rt", "alllowercase1", "ALLUPPERCASE1", "NoDigitsHere", "A1a" })
    void register_rejectsWeakPassword(String password) {
        assertThrows(
            InvalidArgumentException.class,
            () -> User.register(Email.of("a@b.com"), password, FullName.of("A"), e -> false, hasher)
        );
    }

    @Test
    void login_succeedsAndRecordsLastLogin() {
        User user = UserFixtures.activeCustomer();

        user.login(PASSWORD, hasher);

        assertNotNull(user.getLastLoginAt());
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = UserFixtures.activeCustomer();

        assertThrows(InvalidCredentialsException.class, () -> user.login("Wrong123", hasher));
        assertNull(user.getLastLoginAt());
    }

    @Test
    void login_rejectsBlockedAccount() {
        User user = UserFixtures.user(UserRole.ROLE_CUSTOMER, UserStatus.BLOCKED, true);

        assertThrows(AccountBlockedException.class, () -> user.login(PASSWORD, hasher));
    }

    @Test
    void changePassword_replacesHash() {
        User user = UserFixtures.activeCustomer();

        user.changePassword(PASSWORD, "NewSecret456", hasher);

        assertEquals(hasher.hash("NewSecret456"), user.getPassword());
    }

    @Test
    void changePassword_rejectsWrongOldPassword() {
        User user = UserFixtures.activeCustomer();

        assertThrows(InvalidArgumentException.class, () -> user.changePassword("Wrong123", "NewSecret456", hasher));
        assertEquals(hasher.hash(PASSWORD), user.getPassword());
    }

    @Test
    void changePassword_rejectsWeakNewPassword() {
        User user = UserFixtures.activeCustomer();

        assertThrows(InvalidArgumentException.class, () -> user.changePassword(PASSWORD, "weak", hasher));
    }

    @Test
    void resetPassword_rejectsWeakPassword() {
        assertThrows(
            InvalidArgumentException.class,
            () -> UserFixtures.activeCustomer().resetPassword("weak", hasher)
        );
    }

    @Test
    void verifyEmail_marksVerified() {
        User user = UserFixtures.user(UserRole.ROLE_CUSTOMER, UserStatus.ACTIVE, false);

        user.verifyEmail();

        assertTrue(user.isEmailVerified());
    }

    @Test
    void block_changesStatusForCustomer() {
        User user = UserFixtures.activeCustomer();

        user.block();
        assertEquals(UserStatus.BLOCKED, user.getStatus());

        user.activate();
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void block_rejectsAdmin() {
        User admin = UserFixtures.user(UserRole.ROLE_ADMIN, UserStatus.ACTIVE, true);

        assertThrows(InvalidOperationException.class, admin::block);
        assertEquals(UserStatus.ACTIVE, admin.getStatus());
    }

    @Test
    void updateProfile_keepsFullNameWhenNullButOverwritesPhoneAndAvatar() {
        User user = UserFixtures.activeCustomer();
        user.updateProfile(null, PhoneNumber.of("0912345678"), "http://img/1.png");
        assertEquals(FullName.of("John Doe"), user.getFullName());
        assertEquals("0912345678", user.getPhoneNumber().value());

        user.updateProfile(FullName.of("Jane"), null, null);

        assertEquals(FullName.of("Jane"), user.getFullName());
        assertNull(user.getPhoneNumber());
        assertNull(user.getAvatarUrl());
    }
}
