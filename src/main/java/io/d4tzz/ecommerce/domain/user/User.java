package io.d4tzz.ecommerce.domain.user;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import java.time.Instant;
import java.util.regex.Pattern;

public class User {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,128}$");

    private UserId id;
    private Email email;
    private HashedPassword password;
    private FullName fullName;
    private PhoneNumber phoneNumber;
    private String avatarUrl;
    private Address address;
    private UserRole role;
    private UserStatus status;
    private boolean emailVerified;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Full constructor for reconstituting from persistence
    public User(
        UserId id,
        Email email,
        HashedPassword password,
        FullName fullName,
        PhoneNumber phoneNumber,
        String avatarUrl,
        Address address,
        UserRole role,
        UserStatus status,
        boolean emailVerified,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.address = address;
        this.role = role;
        this.status = status;
        this.emailVerified = emailVerified;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static User register(
        Email email,
        String rawPassword,
        FullName fullName,
        UserEmailChecker checker,
        PasswordHasher hasher
    ) {
        validateRawPassword(rawPassword);
        if (checker.exists(email)) {
            throw new ConflictException("Email '" + email.value() + "' is already registered");
        }
        HashedPassword hashedPassword = hasher.hash(rawPassword);
        Instant now = Instant.now();
        return new User(
            null,
            email,
            hashedPassword,
            fullName,
            null,
            null,
            null,
            UserRole.ROLE_CUSTOMER,
            UserStatus.ACTIVE,
            false,
            null,
            now,
            now
        );
    }

    public void changePassword(String oldPassword, String newPassword, PasswordHasher hasher) {
        if (!checkPassword(oldPassword, hasher)) {
            throw new InvalidArgumentException("Incorrect old password");
        }
        validateRawPassword(newPassword);

        this.password = hasher.hash(newPassword);
        this.updatedAt = Instant.now();
    }

    public void resetPassword(String newPassword, PasswordHasher hasher) {
        validateRawPassword(newPassword);

        this.password = hasher.hash(newPassword);
        this.updatedAt = Instant.now();
    }

    public boolean checkPassword(String rawPassword, PasswordHasher hasher) {
        return hasher.validate(rawPassword, this.password);
    }

    public void login(String rawPassword, PasswordHasher hasher) {
        if (!checkPassword(rawPassword, hasher)) {
            throw new InvalidArgumentException("Invalid email or password");
        }

        if (this.status == UserStatus.BLOCKED) {
            throw new InvalidOperationException("Account has been blocked");
        }

        recordLogin(Instant.now());
    }

    private static void validateRawPassword(String rawPassword) {
        if (!PASSWORD_PATTERN.matcher(rawPassword).matches()) {
            throw new InvalidArgumentException(
                "Password must be between 8 and 128 characters, containing at least one uppercase letter, one lowercase letter, and one number"
            );
        }
    }

    public void updateProfile(FullName fullName, PhoneNumber phoneNumber, String avatarUrl) {
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.updatedAt = Instant.now();
    }

    public void updateAddress(Address address) {
        this.address = address;
        this.updatedAt = Instant.now();
    }

    public void clearAddress() {
        this.address = null;
        this.updatedAt = Instant.now();
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.updatedAt = Instant.now();
    }

    public void recordLogin(Instant loginTime) {
        this.lastLoginAt = loginTime != null ? loginTime : Instant.now();
    }

    public void block() {
        if (this.role == UserRole.ROLE_ADMIN) {
            throw new InvalidOperationException("Cannot block an admin account");
        }
        this.status = UserStatus.BLOCKED;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    // Getters
    public UserId getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public HashedPassword getPassword() {
        return password;
    }

    public FullName getFullName() {
        return fullName;
    }

    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Address getAddress() {
        return address;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
