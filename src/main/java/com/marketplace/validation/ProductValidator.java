package com.marketplace.validation;

import com.marketplace.model.Product;
import com.marketplace.service.AuditService;

/**
 * Валидатор для объектов {@link Product}.
 * Проверяет корректность данных продукта перед сохранением или обновлением.
 * Логирует ошибки через {@link AuditService} и выбрасывает исключения при нарушении правил.
 */
public class ProductValidator {
    private final AuditService auditService;

    /**
     * Создает валидатор для проверки продуктов.
     *
     * @param auditService сервис для логирования ошибок валидации
     */
    public ProductValidator(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Проверяет валидность продукта.
     * <p>
     * Проверяет, что продукт не равен {@code null}, название, бренд и категория не пустые,
     * а цена положительная.
     * В случае ошибки логирует через {@link AuditService} и выбрасывает {@link IllegalArgumentException}.
     *
     * @param product продукт для проверки
     * @param userId  идентификатор пользователя, инициирующего операцию (для логирования)
     * @throws IllegalArgumentException если продукт {@code null}, поля пустые или цена отрицательная
     */
    public void validate(Product product, Long userId) {
        if (product == null) {
            logError(userId, "Product cannot be null");
            throw new IllegalArgumentException("Product cannot be null");
        }
        if (isBlank(product.getName())) {
            logError(userId, "Product name cannot be empty");
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        if (isBlank(product.getBrand())) {
            logError(userId, "Product brand cannot be empty");
            throw new IllegalArgumentException("Product brand cannot be empty");
        }
        if (isBlank(product.getCategory())) {
            logError(userId, "Product category cannot be empty");
            throw new IllegalArgumentException("Product category cannot be empty");
        }
        if (product.getPrice() <= 0) {
            logError(userId, "Product price must be positive");
            throw new IllegalArgumentException("Product price must be positive");
        }
    }

    private void logError(Long userId, String message) {
        auditService.logError(userId, "VALIDATION_ERROR", message);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}