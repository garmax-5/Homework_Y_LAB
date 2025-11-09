package com.marketplace.service;

import com.marketplace.model.User;
import com.marketplace.out.repository.UserRepository;
import com.marketplace.validation.UserValidator;

import java.util.Optional;

/**
 * Сервис аутентификации и авторизации пользователей.
 * <p>
 * Отвечает за регистрацию, вход, выход и проверку прав доступа пользователя.
 * Также интегрируется с системами аудита и метрик для фиксации действий и производительности.
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
     * Выполняет проверку корректности данных и уникальности идентификатора и имени пользователя.
     * В случае успеха - сохраняет нового пользователя в репозитории.
     *
     * @param user объект пользователя для регистрации
     * @throws IllegalArgumentException если пользователь с таким ID или именем уже существует
     */
    public void register(User user) {
        long start = metricsService.startTimer();
        try {
            validator.validate(user);

            // Проверка уникальности ID
            if (userRepository.existsById(user.getId())) {
                auditService.logError(user.getId(), "REGISTER_FAILED", "User ID already exists");
                metricsService.increment("register.failed");
                throw new IllegalArgumentException("Пользователь с таким ID уже существует");
            }

            // Проверка уникальности username
            if (userRepository.findByUserName(user.getUserName()).isPresent()) {
                auditService.logError(user.getId(), "REGISTER_FAILED", "Username already exists");
                metricsService.increment("register.failed");
                throw new IllegalArgumentException("Пользователь с таким именем уже существует");
            }

            // Сохраняем пользователя
            userRepository.save(user);
            auditService.logInfo(user.getId(), "REGISTER", "User successfully registered");
            metricsService.increment("register.success");

        } finally {
            metricsService.stopTimer("register", start);
        }
    }


    /**
     * Выполняет вход пользователя по имени и паролю.
     * <p>
     * При успешной аутентификации сохраняет текущего пользователя в контексте {@code currentUser}.
     *
     * @param userName имя пользователя
     * @param password пароль
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

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == User.Role.ADMIN;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }
}