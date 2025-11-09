package com.marketplace.in;

import com.marketplace.model.Product;
import com.marketplace.model.User;
import com.marketplace.out.filestore.ProductFileStore;
import com.marketplace.out.filestore.UserFileStore;
import com.marketplace.out.repository.ProductRepositoryImpl;
import com.marketplace.out.repository.UserRepositoryImpl;
import com.marketplace.service.*;
import com.marketplace.validation.ProductValidator;
import com.marketplace.validation.UserValidator;

import java.util.Optional;
import java.util.Scanner;

public class ConsoleApp {

    private final Scanner scanner = new Scanner(System.in);
    private final AuditService auditService = new AuditService();
    private final MetricsService metricsService = new MetricsService();
    private final ConsolePrinter printer = new ConsolePrinterImpl();

//    private final UserRepositoryImpl userRepository = new UserRepositoryImpl();
//    private final ProductRepositoryImpl productRepository = new ProductRepositoryImpl();

    private final UserFileStore userRepository = new UserFileStore("src/main/resources/data/users.txt");
    private final ProductFileStore productRepository = new ProductFileStore("src/main/resources/data/products.txt");

    private final UserValidator userValidator = new UserValidator(auditService);
    private final ProductValidator productValidator = new ProductValidator(auditService);

    private final AuthService authService = new AuthService(userRepository, auditService, metricsService, userValidator);
    private final ProductService productService = new ProductService(productRepository, auditService, authService, metricsService, productValidator);

    public void start() {
        printer.printMessage("=== Marketplace Product Catalog ===");
        boolean exit = false;

        while (!exit) {
            if (!authService.isAuthenticated()) {
                exit = showAuthMenu();
            } else {
                exit = showMainMenu();
            }
        }
        printer.printMessage("Программа завершена.");
    }

    // Меню авторизации
    private boolean showAuthMenu() {
        printer.printMessage("\n1. Войти\n2. Зарегистрироваться\n0. Выход");
        printer.printMessage("Выберите действие: ");
        String choice = scanner.nextLine();
        switch (choice) {
            case "1": login(); break;
            case "2": register(); break;
            case "0": return true;
            default: printer.printMessage("Некорректный выбор.");
        }
        return false;
    }

    // Главное меню
    private boolean showMainMenu() {
        printer.printMessage("\n=== Главное меню ===");
        printer.printMessage(
                "1. Просмотреть все товары\n" +
                        "2. Найти товар по ID\n" +
                        "3. Найти товары по бренду\n" +
                        "4. Найти товары по категории\n" +
                        "5. Найти товары по диапазону цен"
        );
        if (authService.isAdmin()) {
            printer.printMessage("6. Добавить или обновить товар\n7. Удалить товар");
        }
        printer.printMessage(
                "8. Показать журнал аудита\n" +
                        "9. Показать метрики\n" +
                        "10. Выйти из аккаунта\n" +
                        "0. Завершить программу"
        );
        printer.printMessage("Выберите действие: ");

        String choice = scanner.nextLine();
        switch (choice) {
            case "1": listProducts(); break;
            case "2": findProductById(); break;
            case "3": findByBrand(); break;
            case "4": findByCategory(); break;
            case "5": findByPriceRange(); break;
            case "6": if (authService.isAdmin()) addOrUpdateProduct(); else printer.printMessage("Доступ запрещен."); break;
            case "7": if (authService.isAdmin()) deleteProduct(); else printer.printMessage("Доступ запрещен."); break;
            case "8": showAuditLog(); break;
            case "9": printer.printMetrics(metricsService); break;
            case "10": authService.logout(); break;
            case "0": return true;
            default: printer.printMessage("Некорректный выбор.");
        }
        return false;
    }

    // Авторизация
    private void login() {
        String username = readNonEmptyString("Введите имя пользователя: ", 30);
        String password = readNonEmptyString("Введите пароль: ", 30);

        try {
            authService.login(username, password);
            printer.printMessage("Добро пожаловать, " + username + "!");
        } catch (IllegalArgumentException e) {
            printer.printMessage("Ошибка авторизации: " + e.getMessage());
        }
    }


    private void register() {
        long id;
        while (true) {
            id = readLong("Введите ID пользователя (число): ");
            if (userRepository.existsById(id)) {
                printer.printMessage("Ошибка: пользователь с таким ID уже существует. Попробуйте другой ID.");
            } else {
                break;
            }
        }

        String username = readNonEmptyString("Введите имя пользователя: ", 30);
        String password = readNonEmptyString("Введите пароль: ", 30);
        User.Role role = readRole();

        try {
            authService.register(new User(id, username, password, role));
            printer.printMessage("Регистрация успешна!");
        } catch (IllegalArgumentException e) {
            printer.printMessage("Ошибка: " + e.getMessage());
        }
    }

    // Работа с товарами
    private void listProducts() {
        printer.printProducts(productService.findAll());
    }

    private void findProductById() {
        long id = readLong("Введите ID товара: ");
        Optional<Product> productOpt = productService.findById(id);
        if (productOpt.isPresent()) {
            printer.printProduct(productOpt.get());
        } else {
            printer.printMessage("Товар не найден.");
        }
    }

    private void findByBrand() {
        String brand = readNonEmptyString("Введите бренд: ", 50);
        printer.printProducts(productService.findByBrand(brand));
    }

    private void findByCategory() {
        String category = readNonEmptyString("Введите категорию: ", 50);
        printer.printProducts(productService.findByCategory(category));
    }

    private void findByPriceRange() {
        double min, max;
        while (true) {
            min = readDouble("Минимальная цена: ");
            max = readDouble("Максимальная цена: ");
            if (min > max) {
                printer.printMessage("Ошибка: минимальная цена не может быть больше максимальной. Попробуйте снова.");
            } else {
                break;
            }
        }
        printer.printProducts(productService.findByPriceRange(min, max));
    }

    private void addOrUpdateProduct() {
        while (true) {
            try {
                long id = readLong("Введите ID товара: ");
                String name = readNonEmptyString("Название: ", 50);
                String brand = readNonEmptyString("Бренд: ", 50);
                String category = readNonEmptyString("Категория: ", 50);
                double price = readDouble("Цена: ");

                Product product = new Product(id, name, brand, category, price);

                if (productService.findById(id).isPresent()) {
                    productService.update(product);
                    printer.printMessage("Товар успешно обновлён.");
                } else {
                    productService.save(product);
                    printer.printMessage("Товар успешно создан.");
                }

                break;

            } catch (IllegalArgumentException e) {
                printer.printMessage("Ошибка: " + e.getMessage());
                printer.printMessage("Попробуйте снова.\n");
            } catch (SecurityException e) {
                printer.printMessage("Ошибка доступа: " + e.getMessage());
                break;
            }
        }
    }


    private void deleteProduct() {
        try {
            long id = readLong("Введите ID товара для удаления: ");
            boolean deleted = productService.deleteById(id);
            printer.printMessage(deleted ? "Товар удалён." : "Товар с указанным ID не найден.");
        } catch (SecurityException e) {
            printer.printMessage("Ошибка доступа: " + e.getMessage());
        }
    }

    private void showAuditLog() {
        printer.printAuditLog(auditService.getAllEvents());
    }

    // Вспомогательные методы
    private long readLong(String prompt) {
        while (true) {
            printer.printMessage(prompt);
            try {
                long val = Long.parseLong(scanner.nextLine());
                if (val < 0) {
                    printer.printMessage("Ошибка: число не может быть отрицательным.");
                } else return val;
            } catch (NumberFormatException e) {
                printer.printMessage("Ошибка: нужно ввести целое число.");
            }
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            printer.printMessage(prompt);
            try {
                double val = Double.parseDouble(scanner.nextLine());
                if (val < 0) {
                    printer.printMessage("Ошибка: число не может быть отрицательным.");
                } else return val;
            } catch (NumberFormatException e) {
                printer.printMessage("Ошибка: нужно ввести число.");
            }
        }
    }

    private String readNonEmptyString(String prompt, int maxLength) {
        while (true) {
            printer.printMessage(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                printer.printMessage("Ошибка: строка не может быть пустой.");
            } else if (input.length() > maxLength) {
                printer.printMessage("Ошибка: слишком длинное значение (максимум " + maxLength + " символов).");
            } else {
                return input;
            }
        }
    }

    private User.Role readRole() {
        while (true) {
            printer.printMessage("Роль (ADMIN / USER): ");
            String roleStr = scanner.nextLine().trim().toUpperCase();
            if (roleStr.equals("ADMIN")) return User.Role.ADMIN;
            if (roleStr.equals("USER")) return User.Role.USER;
            printer.printMessage("Ошибка: допустимые роли ADMIN или USER.");
        }
    }

    public static void main(String[] args) {
        new ConsoleApp().start();
    }
}
