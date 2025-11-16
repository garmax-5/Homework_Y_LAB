package com.marketplace.service;

import com.marketplace.model.Product;
import com.marketplace.out.repository.ProductRepository;
import com.marketplace.validation.ProductValidator;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления товарами.
 * Реализует бизнес-логику CRUD-операций и методы фильтрации товаров по различным критериям.
 * <p>
 * Взаимодействует с {@link ProductRepository}, проводит валидацию данных через {@link ProductValidator},
 * выполняет аудит действий пользователей и обновляет метрики через {@link MetricsService}.
 */
public class ProductService {
    private final ProductRepository repository;
    private final AuditService auditService;
    private final AuthService authService;
    private final MetricsService metricsService;
    private final ProductValidator validator;

    /**
     * Конструктор сервиса для управления товарами.
     *
     * @param repository     репозиторий для доступа к данным о продуктах
     * @param auditService   сервис для логирования действий пользователей
     * @param authService    сервис аутентификации и авторизации
     * @param metricsService сервис для сбора и обновления метрик
     * @param validator      валидатор для проверки корректности данных о товаре
     */
    public ProductService(ProductRepository repository, AuditService auditService, AuthService authService,
                          MetricsService metricsService, ProductValidator validator) {
        this.repository = repository;
        this.auditService = auditService;
        this.authService = authService;
        this.metricsService = metricsService;
        this.validator = validator;
    }

    /**
     * Сохраняет новый продукт в хранилище.
     * <p>
     * Доступно только пользователю с ролью ADMIN.
     *
     * @param product продукт для сохранения
     * @return сохранённый продукт с назначенным идентификатором
     * @throws SecurityException        если текущий пользователь не имеет прав администратора
     * @throws IllegalArgumentException если продукт уже существует (id != null) или не прошёл валидацию
     */
    public Product save(Product product) {
        requireAdmin();
        Long userId = currentUserId();
        validator.validate(product, userId);

        if (product.getId() != null) {
            auditService.logError(userId, "SAVE_FAILED", "Use update() for existing products");
            throw new IllegalArgumentException("Для создания товара не указывайте ID (id должен быть 0)");
        }

        Product saved = repository.save(product);

        auditService.logInfo(userId, "ADD_PRODUCT", "Added product id=" + saved.getId() + " name=" + saved.getName());
        metricsService.increment("product.added");
        metricsService.setGauge("product.count", repository.count());

        return saved;
    }

    /**
     * Обновляет существующий продукт.
     * <p>
     * Доступно только пользователю с ролью ADMIN.
     *
     * @param product изменённые данные продукта
     * @return обновлённый продукт с актуальными данными из базы
     * @throws SecurityException        если текущий пользователь не является администратором
     * @throws IllegalArgumentException если продукт не найден по ID или не прошёл валидацию
     * @throws RuntimeException         если обновление в базе данных завершилось неудачно
     */
    public Product update(Product product) {
        requireAdmin();
        Long userId = currentUserId();
        validator.validate(product, userId);

        if (product.getId() == null || !repository.existsById(product.getId())) {
            auditService.logError(userId, "UPDATE_FAILED", "Product id=" + product.getId() + " does not exist");
            throw new IllegalArgumentException("Продукт с ID " + product.getId() + " не найден");
        }

        boolean ok = repository.update(product);
        if (!ok) {
            auditService.logError(userId, "UPDATE_FAILED", "DB update returned false for id=" + product.getId());
            throw new RuntimeException("Не удалось обновить продукт");
        }

        auditService.logInfo(userId, "UPDATE_PRODUCT", "Updated product id=" + product.getId() + " name=" + product.getName());
        metricsService.increment("product.updated");
        metricsService.setGauge("product.count", repository.count());

        return repository.findById(product.getId()).orElse(product);
    }

    /**
     * Возвращает продукт по его идентификатору.
     *
     * @param id идентификатор продукта
     * @return {@link Optional}, содержащий продукт, если он найден; пустой Optional в противном случае
     */
    public Optional<Product> findById(long id) {
        return repository.findById(id);
    }

    /**
     * Удаляет продукт по его идентификатору.
     * <p>
     * Доступно только пользователям с ролью ADMIN.
     *
     * @param id идентификатор продукта
     * @return {@code true}, если продукт был успешно удалён, иначе {@code false}
     * @throws SecurityException если текущий пользователь не имеет прав администратора
     */
    public boolean deleteById(long id) {
        requireAdmin();
        Long userId = currentUserId();

        boolean deleted = repository.deleteById(id);
        if (deleted) {
            auditService.logInfo(userId, "DELETE_PRODUCT", "Product id=" + id);
            metricsService.increment("product.deleted");
            metricsService.setGauge("product.count", repository.count());
        } else {
            auditService.logError(userId, "DELETE_PRODUCT_FAILED", "Attempted to delete non-existing product id=" + id);
        }
        return deleted;
    }

    /**
     * Возвращает список всех продуктов.
     *
     * @return список всех продуктов в базе
     */
    public List<Product> findAll() {
        return repository.findAll();
    }

    /**
     * Возвращает список продуктов указанного бренда.
     *
     * @param brand название бренда
     * @return список продуктов указанного бренда; пустой список, если совпадений нет
     */
    public List<Product> findByBrand(String brand) {
        long start = metricsService.startTimer();
        List<Product> result = repository.findByBrand(brand);
        metricsService.stopTimer("findByBrand", start);

        auditService.logInfo(currentUserId(), "FILTER_PRODUCTS", "Filter by brand=" + brand);
        return result;
    }

    /**
     * Возвращает список продуктов указанной категории.
     *
     * @param category категория продукта
     * @return список продуктов указанной категории; пустой список, если совпадений нет
     */
    public List<Product> findByCategory(String category) {
        long start = metricsService.startTimer();
        List<Product> result = repository.findByCategory(category);
        metricsService.stopTimer("findByCategory", start);

        auditService.logInfo(currentUserId(), "FILTER_PRODUCTS", "Filter by category=" + category);
        return result;
    }

    /**
     * Возвращает список продуктов, цена которых находится в указанном диапазоне.
     *
     * @param min минимальная цена
     * @param max максимальная цена
     * @return список продуктов с ценой в диапазоне [min, max]; пустой список, если совпадений нет
     * @throws IllegalArgumentException если минимальная цена больше максимальной
     */
    public List<Product> findByPriceRange(double min, double max) {
        if (min > max) {
            auditService.logError(currentUserId(), "INVALID_PRICE_RANGE", "Invalid price range [" + min + ", " + max + "]");
            throw new IllegalArgumentException("Минимальная цена не может быть больше максимальной");
        }

        long start = metricsService.startTimer();
        List<Product> result = repository.findByPriceRange(min, max);
        metricsService.stopTimer("findByPriceRange", start);

        auditService.logInfo(currentUserId(), "FILTER_PRODUCTS", "Filter by price range=[" + min + ", " + max + "]");
        return result;
    }

    /**
     * Возвращает общее количество продуктов в базе.
     *
     * @return количество всех продуктов
     */
    public long count() {
        long total = repository.count();
        metricsService.setGauge("product.count", total);
        return total;
    }

    /**
     * Проверяет, что текущий пользователь имеет права администратора.
     *
     * @throws SecurityException если пользователь не является администратором
     */
    private void requireAdmin() {
        if (!authService.isAdmin()) {
            Long userId = currentUserId();
            auditService.logError(userId, "ACCESS_DENIED", "Admin role required");
            throw new SecurityException("Доступ запрещен: требуется роль ADMIN");
        }
    }

    /**
     * Возвращает идентификатор текущего пользователя.
     *
     * @return ID текущего пользователя или {@code null}, если пользователь не аутентифицирован
     */
    private Long currentUserId() {
        return authService.getCurrentUser() != null ? authService.getCurrentUser().getId() : null;
    }
}
