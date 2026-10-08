package io.d4tzz.ecommerce.user.application;

import static io.d4tzz.ecommerce.user.domain.UserFixtures.PASSWORD;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.user.application.command.ChangePasswordCommand;
import io.d4tzz.ecommerce.user.application.command.RegisterUserCommand;
import io.d4tzz.ecommerce.user.application.command.UpdateProfileCommand;
import io.d4tzz.ecommerce.user.domain.*;
import io.d4tzz.ecommerce.user.domain.auth.*;
import io.d4tzz.ecommerce.user.domain.event.UserRegisteredEvent;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class AccountUseCasesTest {

    private UserRepository userRepository;
    private OtpRepository otpRepository;
    private OtpGenerator otpGenerator;
    private RefreshTokenRepository refreshTokenRepository;
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        otpRepository = mock(OtpRepository.class);
        otpGenerator = mock(OtpGenerator.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
    }

    // ---- RegisterUserUseCase ----

    private RegisterUserUseCase register() {
        return new RegisterUserUseCase(
            userRepository,
            otpRepository,
            otpGenerator,
            new FakePasswordHasher(),
            eventPublisher
        );
    }

    @Test
    void register_savesUserGeneratesOtpAndPublishesEvent() {
        User saved = UserFixtures.user(UserRole.ROLE_CUSTOMER, UserStatus.ACTIVE, false);
        Otp otp = Otp.generate(saved.getId(), OtpType.EMAIL_VERIFICATION, Duration.ofMinutes(5), 5);
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(otpGenerator.generateOtp(saved.getId(), OtpType.EMAIL_VERIFICATION)).thenReturn(otp);

        UserId id = register().execute(new RegisterUserCommand("John@Example.com", PASSWORD, "John Doe"));

        assertEquals(saved.getId(), id);
        verify(otpRepository).save(otp);
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        UserRegisteredEvent registered = assertInstanceOf(UserRegisteredEvent.class, event.getValue());
        assertEquals(otp.getCode(), registered.otpCode());
    }

    @Test
    void register_duplicateEmailSavesNothing() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(UserFixtures.activeCustomer()));

        assertThrows(EmailAlreadyExistsException.class, () ->
            register().execute(new RegisterUserCommand("john@example.com", PASSWORD, "John"))
        );
        verify(userRepository, never()).save(any());
        verifyNoInteractions(otpGenerator, eventPublisher);
    }

    // ---- ChangePasswordUseCase ----

    private ChangePasswordUseCase changePassword() {
        return new ChangePasswordUseCase(userRepository, new FakePasswordHasher(), refreshTokenRepository);
    }

    @Test
    void changePassword_updatesHashAndRevokesSessions() {
        User user = UserFixtures.activeCustomer();
        when(userRepository.find(user.getId())).thenReturn(Optional.of(user));

        changePassword().execute(new ChangePasswordCommand(1L, PASSWORD, "NewSecret456"));

        assertEquals(new FakePasswordHasher().hash("NewSecret456"), user.getPassword());
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
    }

    @Test
    void changePassword_wrongOldPasswordRevokesNothing() {
        User user = UserFixtures.activeCustomer();
        when(userRepository.find(user.getId())).thenReturn(Optional.of(user));

        assertThrows(InvalidArgumentException.class, () ->
            changePassword().execute(new ChangePasswordCommand(1L, "Wrong123", "NewSecret456"))
        );
        verify(userRepository, never()).save(any());
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void changePassword_unknownUser() {
        when(userRepository.find(any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            changePassword().execute(new ChangePasswordCommand(99L, PASSWORD, "NewSecret456"))
        );
    }

    // ---- UpdateProfileUseCase ----

    @Test
    void updateProfile_appliesValueObjects() {
        User user = UserFixtures.activeCustomer();
        when(userRepository.find(user.getId())).thenReturn(Optional.of(user));

        new UpdateProfileUseCase(userRepository).execute(
            new UpdateProfileCommand(1L, "Jane Doe", "0912 345 678", "http://img/1.png")
        );

        assertEquals(FullName.of("Jane Doe"), user.getFullName());
        assertEquals("0912345678", user.getPhoneNumber().value());
        assertEquals("http://img/1.png", user.getAvatarUrl());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_invalidPhoneIsRejectedBeforeSave() {
        User user = UserFixtures.activeCustomer();
        when(userRepository.find(user.getId())).thenReturn(Optional.of(user));

        assertThrows(InvalidArgumentException.class, () ->
            new UpdateProfileUseCase(userRepository).execute(new UpdateProfileCommand(1L, null, "abc", null))
        );
        verify(userRepository, never()).save(any());
    }
}
