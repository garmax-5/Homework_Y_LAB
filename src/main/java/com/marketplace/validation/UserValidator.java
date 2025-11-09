package com.marketplace.validation;

import com.marketplace.model.User;
import com.marketplace.service.AuditService;

/**
 * Валидатор для объектов {@link User}.
 * Проверяет корректность данных пользователя перед регистрацией или обновлением.
 * Логирует ошибки через {@link AuditService} и выбрасывает исключения при нарушении правил.
 */
public class UserValidator {
    private final AuditService auditService;

    /**
     * Создает валидатор для проверки пользователей.
     *
     * @param auditService сервис для логирования ошибок валидации
     */
    public UserValidator(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Проверяет валидность пользователя.
     * <p>
     * Проверяет, что пользователь не равен {@code null}, имя пользователя не пустое,
     * а пароль содержит не менее 4 символов.
     * В случае ошибки логирует через {@link AuditService} и выбрасывает {@link IllegalArgumentException}.
     *
     * @param user пользователь для проверки
     * @throws IllegalArgumentException если пользователь {@code null}, имя пустое или пароль слишком короткий
     */
    public void validate(User user) {
        if (user == null) {
            auditService.logError(null, "VALIDATION_ERROR", "User cannot be null");
            throw new IllegalArgumentException("User cannot be null");
        }
        if (isBlank(user.getUserName())) {
            auditService.logError(user.getId(), "VALIDATION_ERROR", "Username cannot be empty");
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (user.getPassword() == null || user.getPassword().length() < 4) {
            auditService.logError(user.getId(), "VALIDATION_ERROR", "Password is too short");
            throw new IllegalArgumentException("Password must have at least 4 characters");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
