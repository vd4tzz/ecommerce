package io.d4tzz.ecommerce.user.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.user.domain.Email;
import io.d4tzz.ecommerce.user.domain.FullName;
import io.d4tzz.ecommerce.user.domain.HashedPassword;
import io.d4tzz.ecommerce.user.domain.PhoneNumber;
import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserId;
import io.d4tzz.ecommerce.user.domain.UserRepository;
import io.d4tzz.ecommerce.user.domain.UserRole;
import io.d4tzz.ecommerce.user.domain.UserStatus;
import io.d4tzz.ecommerce.user.infrastructure.persistence.entity.JpaAddress;
import io.d4tzz.ecommerce.user.infrastructure.persistence.entity.JpaUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface SpringUserRepository extends JpaRepository<JpaUser, Long> {
    Optional<JpaUser> findByEmail(String email);

    boolean existsByEmail(String email);
}

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final SpringUserRepository springRepo;

    public UserRepositoryImpl(SpringUserRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<User> find(UserId id) {
        return springRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return springRepo.findByEmail(email.value()).map(this::toDomain);
    }

    @Override
    public User save(User user) {
        JpaUser jpaUser;
        if (user.getId() == null) {
            jpaUser = toJpa(user);
        } else {
            jpaUser = springRepo.findById(user.getId().value()).orElseThrow();
            updateJpa(jpaUser, user);
        }
        JpaUser saved = springRepo.save(jpaUser);
        return user.getId() == null ? toDomain(saved) : user;
    }

    @Override
    public void delete(UserId id) {
        springRepo.deleteById(id.value());
    }

    private User toDomain(JpaUser jpa) {
        Address address = null;
        if (jpa.getAddress() != null && jpa.getAddress().getProvince() != null) {
            address = Address.of(
                jpa.getAddress().getProvince(),
                jpa.getAddress().getDistrict(),
                jpa.getAddress().getWard(),
                jpa.getAddress().getDetailAddress()
            );
        }

        return new User(
            UserId.of(jpa.getId()),
            Email.of(jpa.getEmail()),
            HashedPassword.of(jpa.getPassword()),
            FullName.of(jpa.getFullName()),
            jpa.getPhoneNumber() != null ? PhoneNumber.of(jpa.getPhoneNumber()) : null,
            jpa.getAvatarUrl(),
            address,
            UserRole.valueOf(jpa.getRole()),
            UserStatus.valueOf(jpa.getStatus()),
            jpa.isEmailVerified(),
            jpa.getLastLoginAt(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt()
        );
    }

    private JpaUser toJpa(User user) {
        JpaUser jpa = new JpaUser();
        updateJpa(jpa, user);
        jpa.setCreatedAt(user.getCreatedAt());
        return jpa;
    }

    private void updateJpa(JpaUser jpa, User user) {
        jpa.setEmail(user.getEmail().value());
        jpa.setPassword(user.getPassword().hash());
        jpa.setFullName(user.getFullName().value());
        jpa.setPhoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber().value() : null);
        jpa.setAvatarUrl(user.getAvatarUrl());
        if (user.getAddress() != null) {
            jpa.setAddress(
                new JpaAddress(
                    user.getAddress().province(),
                    user.getAddress().district(),
                    user.getAddress().ward(),
                    user.getAddress().detailAddress()
                )
            );
        } else {
            jpa.setAddress(null);
        }
        jpa.setRole(user.getRole().name());
        jpa.setStatus(user.getStatus().name());
        jpa.setEmailVerified(user.isEmailVerified());
        jpa.setLastLoginAt(user.getLastLoginAt());
        jpa.setUpdatedAt(user.getUpdatedAt());
    }
}
