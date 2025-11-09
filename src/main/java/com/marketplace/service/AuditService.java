package com.marketplace.service;

import com.marketplace.model.AuditEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис аудита действий пользователей и системы.
 * <p>
 * Позволяет логировать события различных уровней (INFO, ERROR) и хранить историю событий.
 */
public class AuditService {
    private final List<AuditEvent> events = new ArrayList<>();

    /**
     * Логирует событие с указанным уровнем.
     *
     * @param userId  идентификатор пользователя (может быть {@code null})
     * @param action  название действия
     * @param details подробное описание события
     * @param level   уровень события (INFO, ERROR)
     */
    public void log(Long userId, String action, String details, AuditEvent.Level level) {
        AuditEvent event = new AuditEvent(userId, action, details, level);
        events.add(event);
        System.out.println(event);
    }

    /**
     * Логирует информационное событие.
     *
     * @param userId  идентификатор пользователя (может быть {@code null})
     * @param action  название действия
     * @param details подробное описание события
     */
    public void logInfo(Long userId, String action, String details) {
        log(userId, action, details, AuditEvent.Level.INFO);
    }

    /**
     * Логирует событие ошибки.
     *
     * @param userId  идентификатор пользователя (может быть {@code null})
     * @param action  название действия
     * @param details подробное описание ошибки
     */
    public void logError(Long userId, String action, String details) {
        log(userId, action, details, AuditEvent.Level.ERROR);
    }

    /**
     * Получает все события аудита.
     *
     * @return список всех событий аудита
     */
    public List<AuditEvent> getAllEvents() {
        return new ArrayList<>(events);
    }
}