package com.marketplace.service;

import com.marketplace.model.AuditEvent;
import com.marketplace.out.repository.AuditRepository;

import java.util.List;

/**
 * Сервис аудита действий пользователей и системы.
 * <p>
 * Предоставляет методы для логирования событий различных уровней (INFO, ERROR)
 * и получения истории событий. Использует {@link AuditRepository} для хранения
 * и извлечения событий аудита.
 */
public class AuditService {
    private final AuditRepository auditRepository;

    /**
     * Конструктор сервиса аудита.
     *
     * @param auditRepository репозиторий для хранения и извлечения событий аудита
     */
    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Логирует событие с указанным уровнем.
     *
     * @param userId  идентификатор пользователя, совершившего действие; может быть {@code null} для системных событий
     * @param action  краткое название действия
     * @param details подробное описание события
     * @param level   уровень события ({@link AuditEvent.Level#INFO} или {@link AuditEvent.Level#ERROR})
     * @return сохранённый объект события аудита с присвоенным идентификатором и отметкой времени
     */
    public AuditEvent log(Long userId, String action, String details, AuditEvent.Level level) {
        AuditEvent event = new AuditEvent(userId, action, details, level);
        AuditEvent saved = auditRepository.save(event);
        System.out.println(saved); // Для отладки, можно убрать в продакшн
        return saved;
    }

    /**
     * Логирует информационное событие (уровень INFO).
     *
     * @param userId  идентификатор пользователя, совершившего действие; может быть {@code null} для системных событий
     * @param action  краткое название действия
     * @param details подробное описание события
     */
    public void logInfo(Long userId, String action, String details) {
        log(userId, action, details, AuditEvent.Level.INFO);
    }

    /**
     * Логирует событие ошибки (уровень ERROR).
     *
     * @param userId  идентификатор пользователя, совершившего действие; может быть {@code null} для системных событий
     * @param action  краткое название действия
     * @param details подробное описание ошибки
     */
    public void logError(Long userId, String action, String details) {
        log(userId, action, details, AuditEvent.Level.ERROR);
    }

    /**
     * Получает все события аудита из репозитория.
     *
     * @return список всех событий аудита, отсортированный по времени (новые события первыми)
     */
    public List<AuditEvent> getAllEvents() {
        return auditRepository.findAll();
    }
}
