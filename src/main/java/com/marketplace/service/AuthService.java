package com.marketplace.service;

import com.marketplace.model.User;
import com.marketplace.out.repository.UserRepository;
import com.marketplace.validation.UserValidator;

import java.util.Optional;

/**
 * Сервис аутентификации и авторизации пользователей.
 * <p>
 * Отвечает за регистрацию, вход, выход и проверку прав доступа пользователя.
 * Также интегрируется с системами аудита ({@link AuditService}) и метрик ({@link MetricsService})
 * для фиксации действий и мониторинга производительности.
 * <p>
 * Поддерживает хранение текущего активного пользователя в контексте {@code currentUser}.
 */
public class AuthService {
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final MetricsService metricsService;
    private final UserValidator validator;
    private User currentUser;

    /**
     * Конструктор сервиса аутентификации.
     *
     * @param userRepository репозиторий пользователей
     * @param auditService   сервис аудита для логирования событий
     * @param metricsService сервис метрик для сбора статистики
     * @param validator      валидатор для проверки корректности данных пользователя
     */
    public AuthService(UserRepository userRepository, AuditService auditService, MetricsService metricsService, UserValidator validator) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.metricsService = metricsService;
        this.validator = validator;
    }

    /**
     * Регистрирует нового пользователя в системе.
     * <p>
     * Выполняет валидацию данных пользователя через {@link UserValidator}, проверяет уникальность имени пользователя
     * и отсутствие заранее указанного ID.
     * В случае успеха сохраняет пользователя в {@link UserRepository} и фиксирует событие в системе аудита и метрик.
     *
     * @param user объект пользователя для регистрации
     * @throws IllegalArgumentException если пользователь уже существует по имени или указан ID
     */
    public void register(User user) {
        long start = metricsService.startTimer();
        try {
            validator.validate(user);

            if (user.getId() != null) {
                throw new IllegalArgumentException("При регистрации не указывайте ID");
            }

            if (userRepository.findByUserName(user.getUserName()).isPresent()) {
                auditService.logError(null, "REGISTER_FAILED", "Username already exists");
                metricsService.increment("register.failed");
                throw new IllegalArgumentException("Пользователь с таким именем уже существует");
            }

            User saved = userRepository.save(user);
            auditService.logInfo(saved.getId(), "REGISTER", "User successfully registered");
            metricsService.increment("register.success");

        } finally {
            metricsService.stopTimer("register", start);
        }
    }

    /**
     * Выполняет вход пользователя в систему.
     * <p>
     * Проверяет соответствие имени пользователя и пароля с данными из {@link UserRepository}.
     * При успешной аутентификации сохраняет пользователя в контексте {@code currentUser}.
     *
     * @param userName имя пользователя
     * @param password пароль пользователя
     * @throws IllegalArgumentException если имя пользователя или пароль неверные
     */
    public void login(String userName, String password) {
        long start = metricsService.startTimer();
        try {
            Optional<User> found = userRepository.findByUserName(userName);
            if (found.isEmpty() || !found.get().getPassword().equals(password)) {
                auditService.logError(null, "LOGIN_FAILED", "Invalid username or password: " + userName);
                metricsService.increment("login.failed");
                throw new IllegalArgumentException("Неверное имя пользователя или пароль");
            }

            currentUser = found.get();
            auditService.logInfo(currentUser.getId(), "LOGIN", "User logged in");
            metricsService.increment("login.success");
        } finally {
            metricsService.stopTimer("login", start);
        }
    }

    /**
     * Выполняет выход текущего пользователя из системы.
     * <p>
     * Сбрасывает {@code currentUser} и фиксирует событие в системе аудита и метрик.
     *
     * @throws IllegalStateException если попытка выхода совершается без активного пользователя
     */
    public void logout() {
        if (currentUser != null) {
            auditService.logInfo(currentUser.getId(), "LOGOUT", "User logged out");
            metricsService.increment("logout.success");
            currentUser = null;
        } else {
            auditService.logError(null, "LOGOUT_FAILED", "No user is currently logged in");
            metricsService.increment("logout.failed");
            throw new IllegalStateException("Нет активного пользователя для выхода");
        }
    }

    /**
     * Возвращает текущего аутентифицированного пользователя.
     *
     * @return текущий пользователь или {@code null}, если пользователь не аутентифицирован
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Проверяет, обладает ли текущий пользователь правами администратора.
     *
     * @return {@code true}, если текущий пользователь имеет роль {@link User.Role#ADMIN}, иначе {@code false}
     */
    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == User.Role.ADMIN;
    }

    /**
     * Проверяет, аутентифицирован ли текущий пользователь.
     *
     * @return {@code true}, если пользователь вошёл в систему, иначе {@code false}
     */
    public boolean isAuthenticated() {
        return currentUser != null;
    }
}
