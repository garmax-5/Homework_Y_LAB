import com.marketplace.model.User;
import com.marketplace.out.repository.UserRepository;
import com.marketplace.service.AuditService;
import com.marketplace.service.AuthService;
import com.marketplace.service.MetricsService;
import com.marketplace.validation.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private AuditService auditService;
    private MetricsService metricsService;
    private UserValidator validator;
    private AuthService authService;

    private final User testUser = new User(1L, "john", "1234", User.Role.USER);
    private final User adminUser = new User(2L, "admin", "12345", User.Role.ADMIN);

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        auditService = mock(AuditService.class);
        metricsService = mock(MetricsService.class);
        validator = mock(UserValidator.class);

        authService = new AuthService(userRepository, auditService, metricsService, validator);
    }

    // Успешные сценарии
    @Test
    void register_shouldRegisterUser_whenValidAndUnique() {
        when(metricsService.startTimer()).thenReturn(100L);
        when(userRepository.existsById(testUser.getId())).thenReturn(false);
        when(userRepository.findByUserName(testUser.getUserName())).thenReturn(Optional.empty());

        authService.register(testUser);

        verify(validator).validate(testUser);
        verify(userRepository).save(testUser);
        verify(auditService).logInfo(eq(testUser.getId()), eq("REGISTER"), contains("User successfully registered"));
        verify(metricsService).increment("register.success");
        verify(metricsService).stopTimer(eq("register"), eq(100L));
    }

    @Test
    void login_shouldAuthenticateUser_whenCredentialsCorrect() {
        when(metricsService.startTimer()).thenReturn(200L);
        when(userRepository.findByUserName("john")).thenReturn(Optional.of(testUser));

        authService.login("john", "1234");

        verify(auditService).logInfo(eq(testUser.getId()), eq("LOGIN"), anyString());
        verify(metricsService).increment("login.success");
        verify(metricsService).stopTimer(eq("login"), eq(200L));

        assertThat(authService.getCurrentUser()).isEqualTo(testUser);
        assertThat(authService.isAuthenticated()).isTrue();
    }

    @Test
    void logout_shouldClearCurrentUser_whenUserIsLoggedIn() {
        when(metricsService.startTimer()).thenReturn(0L);
        when(userRepository.findByUserName("john")).thenReturn(Optional.of(testUser));
        authService.login("john", "1234");

        authService.logout();

        verify(auditService).logInfo(eq(testUser.getId()), eq("LOGOUT"), anyString());
        verify(metricsService).increment("logout.success");
        assertThat(authService.getCurrentUser()).isNull();
        assertThat(authService.isAuthenticated()).isFalse();
    }

    @Test
    void isAdmin_shouldReturnTrueForAdminUser() {
        when(metricsService.startTimer()).thenReturn(0L);
        when(userRepository.findByUserName("admin")).thenReturn(Optional.of(adminUser));
        authService.login("admin", "12345");

        assertThat(authService.isAdmin()).isTrue();
    }

    // Неуспешные сценарии
    @Test
    void register_shouldFail_whenUserAlreadyExistsById() {
        when(metricsService.startTimer()).thenReturn(10L);
        when(userRepository.existsById(testUser.getId())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("существует");

        verify(auditService).logError(eq(testUser.getId()), eq("REGISTER_FAILED"), contains("User ID already exists"));
        verify(metricsService).increment("register.failed");
        verify(metricsService).stopTimer(eq("register"), eq(10L));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldFail_whenUserAlreadyExistsByUsername() {
        when(metricsService.startTimer()).thenReturn(20L);
        when(userRepository.existsById(testUser.getId())).thenReturn(false);
        when(userRepository.findByUserName(testUser.getUserName())).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.register(testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("существует");

        verify(auditService).logError(eq(testUser.getId()), eq("REGISTER_FAILED"), contains("Username already exists"));
        verify(metricsService).increment("register.failed");
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldFail_whenUserDoesNotExist() {
        when(metricsService.startTimer()).thenReturn(30L);
        when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost", "123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Неверное имя пользователя");

        verify(auditService).logError(isNull(), eq("LOGIN_FAILED"), contains("ghost"));
        verify(metricsService).increment("login.failed");
        verify(metricsService).stopTimer(eq("login"), eq(30L));
    }

    @Test
    void login_shouldFail_whenPasswordIncorrect() {
        when(metricsService.startTimer()).thenReturn(40L);
        when(userRepository.findByUserName("john")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login("john", "wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Неверное имя пользователя");

        verify(auditService).logError(isNull(), eq("LOGIN_FAILED"), contains("john"));
        verify(metricsService).increment("login.failed");
        verify(metricsService).stopTimer(eq("login"), eq(40L));
    }

    @Test
    void logout_shouldFail_whenNoUserLoggedIn() {
        assertThatThrownBy(() -> authService.logout())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Нет активного пользователя");

        verify(auditService).logError(isNull(), eq("LOGOUT_FAILED"), contains("No user is currently logged in"));
        verify(metricsService).increment("logout.failed");
    }
}
