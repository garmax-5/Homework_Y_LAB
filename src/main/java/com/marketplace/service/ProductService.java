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
 * а также выполняет аудит действий и обновление метрик.
 *
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
    public ProductService(ProductRepository repository, AuditService auditService, AuthService authService, MetricsService metricsService, ProductValidator validator) {
        this.repository = repository;
        this.auditService = auditService;
        this.authService = authService;
        this.metricsService = metricsService;
        this.validator = validator;
    }

    /**
     * Сохраняет новый продукт в хранилище.
     * Доступно только пользователю с ролью ADMIN.
     *
     * @param product продукт, который нужно сохранить
     * @return сохранённый продукт
     * @throws SecurityException        если пользователь не имеет прав администратора
     * @throws IllegalArgumentException если продукт уже существует или не прошёл валидацию
     */
    public Product save(Product product) {
        requireAdmin();
        Long userId = authService.getCurrentUser() != null ? authService.getCurrentUser().getId() : null;
        validator.validate(product, userId);

        if (repository.findById(product.getId()).isPresent()) {
            auditService.logError(userId,
                    "DUPLICATE_PRODUCT_ID",
                    "Product with id=" + product.getId() + " already exists");
            throw new IllegalArgumentException("Продукт с ID " + product.getId() + " уже существует");
        }

        Product saved = repository.save(product);
        auditService.logInfo(userId,
                "ADD_PRODUCT",
                "Added product id=" + product.getId() + " name=" + product.getName());
        metricsService.increment("product.added");
        metricsService.setGauge("product.count", repository.count());

        return saved;
    }

    /**
     * Обновляет существующий продукт.
     *
     * @param product изменённые данные продукта
     * @return обновлённый продукт
     * @throws SecurityException        если пользователь не является администратором
     * @throws IllegalArgumentException если продукт не найден или не прошёл валидацию
     */
    public Product update(Product product) {
        requireAdmin();
        Long userId = authService.getCurrentUser() != null ? authService.getCurrentUser().getId() : null;
        validator.validate(product, userId);

        Optional<Product> existingOpt = repository.findById(product.getId());
        if (existingOpt.isEmpty()) {
            auditService.logError(userId,
                    "UPDATE_FAILED",
                    "Product with id=" + product.getId() + " does not exist");
            throw new IllegalArgumentException("Продукт с ID " + product.getId() + " не найден");
        }

        Product existing = existingOpt.get();
        existing.setName(product.getName());
        existing.setBrand(product.getBrand());
        existing.setCategory(product.getCategory());
        existing.setPrice(product.getPrice());

        Product saved = repository.save(existing);

        auditService.logInfo(userId,
                "UPDATE_PRODUCT",
                "Updated product id=" + product.getId() + " name=" + product.getName());
        metricsService.increment("product.updated");
        metricsService.setGauge("product.count", repository.count());

        return saved;
    }

    /**
     * Возвращает продукт по его идентификатору.
     *
     * @param id идентификатор продукта
     * @return {@link Optional}, содержащий продукт, если он найден
     */
    public Optional<Product> findById(long id) {
        return repository.findById(id);
    }

    /**
     * Удаляет продукт по его идентификатору.
     * Доступно только пользователям с ролью ADMIN.
     *
     * @param id идентификатор продукта
     * @return {@code true}, если продукт был успешно удалён, иначе {@code false}
     * @throws SecurityException если текущий пользователь не имеет прав администратора
     */
    public boolean deleteById(long id) {
        requireAdmin();
        boolean deleted = repository.deleteById(id);
        if (deleted) {
            auditService.logInfo(authService.getCurrentUser().getId(),
                    "DELETE_PRODUCT",
                    "Product id=" + id);

            // Метрики
            metricsService.increment("product.deleted");
            metricsService.setGauge("product.count", repository.count());
        } else {
            auditService.logError(authService.getCurrentUser().getId(),
                    "DELETE_PRODUCT_FAILED",
                    "Attempted to delete non-existing product id=" + id);
        }
        return deleted;
    }

    /**
     * Возвращает список всех продуктов.
     *
     * @return список всех продуктов из хранилища
     */
    public List<Product> findAll() {
        return repository.findAll();
    }

    /**
     * Возвращает список продуктов, соответствующих заданному бренду.
     *
     * @param brand название бренда
     * @return список продуктов указанного бренда
     */
    public List<Product> findByBrand(String brand) {
        long start = metricsService.startTimer();

        List<Product> result = repository.findByBrand(brand);

        metricsService.stopTimer("findByBrand", start);
        auditService.logInfo(authService.getCurrentUser().getId(),
                "FILTER_PRODUCTS", "Filter by brand=" + brand);
        return result;
    }

    /**
     * Возвращает список продуктов, принадлежащих указанной категории.
     *
     * @param category категория продукта
     * @return список продуктов указанной категории
     */
    public List<Product> findByCategory(String category) {
        long start = metricsService.startTimer();

        List<Product> result = repository.findByCategory(category);

        metricsService.stopTimer("findByCategory", start);
        auditService.logInfo(authService.getCurrentUser().getId(),
                "FILTER_PRODUCTS", "Filter by category=" + category);
        return result;
    }

    /**
     * Возвращает список продуктов, находящихся в указанном диапазоне цен.
     *
     * @param min минимальная цена
     * @param max максимальная цена
     * @return список продуктов, цена которых находится в указанном диапазоне
     * @throws IllegalArgumentException если минимальная цена больше максимальной
     */
    public List<Product> findByPriceRange(double min, double max) {
        // Проверка диапазона
        if (min > max) {
            auditService.logError(authService.getCurrentUser().getId(),
                    "INVALID_PRICE_RANGE",
                    "Invalid price range [" + min + ", " + max + "]");
            throw new IllegalArgumentException("Минимальная цена не может быть больше максимальной");
        }

        long start = metricsService.startTimer();

        List<Product> result = repository.findByPriceRange(min, max);

        metricsService.stopTimer("findByPriceRange", start);
        auditService.logInfo(authService.getCurrentUser().getId(),
                "FILTER_PRODUCTS", "Filter by price range=[" + min + ", " + max + "]");

        return result;
    }

    /**
     * Возвращает общее количество продуктов.
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
            Long userId = authService.getCurrentUser() != null ? authService.getCurrentUser().getId() : null;
            auditService.logError(userId, "ACCESS_DENIED", "Admin role required");
            throw new SecurityException("Доступ запрещен: требуется роль ADMIN");
        }
    }
}

