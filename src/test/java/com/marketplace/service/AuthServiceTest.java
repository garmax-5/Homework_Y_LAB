package com.marketplace.service;

import com.marketplace.model.User;
import com.marketplace.out.repository.UserRepository;
import com.marketplace.validation.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private AuditService auditService;
    private MetricsService metricsService;
    private UserValidator validator;
    private AuthService authService;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        auditService = mock(AuditService.class);
        metricsService = mock(MetricsService.class);
        validator = mock(UserValidator.class);

        authService = new AuthService(userRepository, auditService, metricsService, validator);

        testUser = new User(null, "john", "1234", User.Role.USER);
        adminUser = new User(null, "admin", "12345", User.Role.ADMIN);
    }

    // Успешные сценарии
    @Test
    void register_shouldRegisterUser_whenValidAndUnique() {
        when(metricsService.startTimer()).thenReturn(100L);
        when(userRepository.findByUserName(testUser.getUserName())).thenReturn(Optional.empty());
        when(userRepository.save(testUser))
                .thenReturn(new User(1L, testUser.getUserName(), testUser.getPassword(), testUser.getRole()));

        authService.register(testUser);

        verify(validator).validate(testUser);
        verify(userRepository).save(testUser);
        verify(auditService).logInfo(1L, "REGISTER", "User successfully registered");
        verify(metricsService).increment("register.success");
        verify(metricsService).stopTimer("register", 100L);
    }

    @Test
    void login_shouldAuthenticateUser_whenCredentialsCorrect() {
        User storedUser = new User(1L, "john", "1234", User.Role.USER);
        when(metricsService.startTimer()).thenReturn(200L);
        when(userRepository.findByUserName("john")).thenReturn(Optional.of(storedUser));

        authService.login("john", "1234");

        verify(auditService).logInfo(1L, "LOGIN", "User logged in");
        verify(metricsService).increment("login.success");
        verify(metricsService).stopTimer("login", 200L);

        assertThat(authService.getCurrentUser()).isEqualTo(storedUser);
        assertThat(authService.isAuthenticated()).isTrue();
    }

    @Test
    void logout_shouldClearCurrentUser_whenUserIsLoggedIn() {
        User storedUser = new User(1L, "john", "1234", User.Role.USER);
        when(metricsService.startTimer()).thenReturn(0L);
        when(userRepository.findByUserName("john")).thenReturn(Optional.of(storedUser));

        authService.login("john", "1234");
        authService.logout();

        verify(auditService).logInfo(1L, "LOGOUT", "User logged out");
        verify(metricsService).increment("logout.success");
        assertThat(authService.getCurrentUser()).isNull();
        assertThat(authService.isAuthenticated()).isFalse();
    }

    @Test
    void isAdmin_shouldReturnTrueForAdminUser() {
        User storedAdmin = new User(2L, "admin", "12345", User.Role.ADMIN);
        when(metricsService.startTimer()).thenReturn(0L);
        when(userRepository.findByUserName("admin")).thenReturn(Optional.of(storedAdmin));

        authService.login("admin", "12345");

        assertThat(authService.isAdmin()).isTrue();
    }

    // Неуспешные сценарии
    @Test
    void register_shouldFail_whenUserAlreadyExistsByUsername() {
        final User userWithoutId = new User("john", "1234", User.Role.USER);
        when(metricsService.startTimer()).thenReturn(20L);
        when(userRepository.findByUserName(userWithoutId.getUserName()))
                .thenReturn(Optional.of(new User(1L, "john", "1234", User.Role.USER)));

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        authService.register(userWithoutId);
                    }
                }
        );
        assertThat(thrown.getMessage()).contains("существует");

        verify(auditService).logError(null, "REGISTER_FAILED", "Username already exists");
        verify(metricsService).increment("register.failed");
        verify(metricsService).stopTimer("register", 20L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldFail_whenIdIsProvided() {
        final User invalidUser = new User(999L, "newuser", "pass", User.Role.USER);
        when(metricsService.startTimer()).thenReturn(30L);

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        authService.register(invalidUser);
                    }
                }
        );
        assertThat(thrown.getMessage()).contains("не указывайте ID");

        verify(auditService, never()).logInfo(anyLong(), anyString(), anyString());
        verify(metricsService, never()).increment("register.success");
        verify(metricsService).stopTimer("register", 30L);
    }

    @Test
    void login_shouldFail_whenUserDoesNotExist() {
        when(metricsService.startTimer()).thenReturn(40L);
        when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        authService.login("ghost", "123");
                    }
                }
        );
        assertThat(thrown.getMessage()).contains("Неверное имя пользователя");

        verify(auditService).logError(null, "LOGIN_FAILED", "Invalid username or password: ghost");
        verify(metricsService).increment("login.failed");
        verify(metricsService).stopTimer("login", 40L);
    }

    @Test
    void login_shouldFail_whenPasswordIncorrect() {
        final User storedUser = new User(1L, "john", "1234", User.Role.USER);
        when(metricsService.startTimer()).thenReturn(50L);
        when(userRepository.findByUserName("john")).thenReturn(Optional.of(storedUser));

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        authService.login("john", "wrong");
                    }
                }
        );
        assertThat(thrown.getMessage()).contains("Неверное имя пользователя");

        verify(auditService).logError(null, "LOGIN_FAILED", "Invalid username or password: john");
        verify(metricsService).increment("login.failed");
        verify(metricsService).stopTimer("login", 50L);
    }

    @Test
    void logout_shouldFail_whenNoUserLoggedIn() {
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        authService.logout();
                    }
                }
        );
        assertThat(thrown.getMessage()).contains("Нет активного пользователя");

        verify(auditService).logError(null, "LOGOUT_FAILED", "No user is currently logged in");
        verify(metricsService).increment("logout.failed");
    }
}
