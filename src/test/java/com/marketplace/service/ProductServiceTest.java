package com.marketplace.service;

import com.marketplace.model.Product;
import com.marketplace.model.User;
import com.marketplace.out.repository.ProductRepository;
import com.marketplace.validation.ProductValidator;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    private ProductRepository productRepository;
    private AuditService auditService;
    private AuthService authService;
    private MetricsService metricsService;
    private ProductValidator validator;

    private ProductService productService;

    private final User testUser = new User(2L, "admin1", "pass", User.Role.ADMIN);
    private final Product sampleProduct =
            new Product(1L, "Laptop", "Dell", "Electronics", 1200.0, Instant.now(), Instant.now());

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        auditService = mock(AuditService.class);
        authService = mock(AuthService.class);
        metricsService = mock(MetricsService.class);
        validator = mock(ProductValidator.class);

        when(authService.isAdmin()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(metricsService.startTimer()).thenReturn(100L);

        productService = new ProductService(productRepository, auditService, authService, metricsService, validator);
    }

    @Test
    void save_shouldSaveNewProduct_whenValidAndAdmin() {
        Product newProduct = new Product("Laptop", "Dell", "Electronics", 1200.0);

        when(productRepository.save(newProduct)).thenReturn(sampleProduct);
        when(productRepository.count()).thenReturn(1L);

        Product result = productService.save(newProduct);

        assertThat(result).isEqualTo(sampleProduct);

        verify(validator).validate(eq(newProduct), eq(testUser.getId()));
        verify(productRepository).save(newProduct);
        verify(auditService).logInfo(eq(testUser.getId()), eq("ADD_PRODUCT"), contains("Laptop"));
        verify(metricsService).increment("product.added");
        verify(metricsService).setGauge("product.count", 1L);
    }

    @Test
    void update_shouldUpdateExistingProduct_whenValidAndAdmin() {
        when(productRepository.existsById(sampleProduct.getId())).thenReturn(true);
        when(productRepository.update(sampleProduct)).thenReturn(true);
        when(productRepository.findById(sampleProduct.getId())).thenReturn(Optional.of(sampleProduct));
        when(productRepository.count()).thenReturn(1L);

        Product result = productService.update(sampleProduct);

        assertThat(result).isEqualTo(sampleProduct);

        verify(validator).validate(eq(sampleProduct), eq(testUser.getId()));
        verify(productRepository).update(sampleProduct);
        verify(auditService).logInfo(eq(testUser.getId()), eq("UPDATE_PRODUCT"), contains("Laptop"));
        verify(metricsService).increment("product.updated");
        verify(metricsService).setGauge("product.count", 1L);
    }

    @Test
    void deleteById_shouldDeleteProduct_whenAdminAndExists() {
        when(productRepository.deleteById(1L)).thenReturn(true);
        when(productRepository.count()).thenReturn(0L);

        boolean result = productService.deleteById(1L);

        assertThat(result).isTrue();

        verify(auditService).logInfo(eq(testUser.getId()), eq("DELETE_PRODUCT"), contains("1"));
        verify(metricsService).increment("product.deleted");
        verify(metricsService).setGauge("product.count", 0L);
    }

    @Test
    void findByBrand_shouldReturnFilteredProducts() {
        when(productRepository.findByBrand("Dell"))
                .thenReturn(Collections.singletonList(sampleProduct));

        List<Product> result = productService.findByBrand("Dell");

        assertThat(result).containsExactly(sampleProduct);

        verify(auditService).logInfo(any(), eq("FILTER_PRODUCTS"), contains("Dell"));
        verify(metricsService).stopTimer(eq("findByBrand"), anyLong());
    }

    @Test
    void findByCategory_shouldReturnFilteredProducts() {
        when(productRepository.findByCategory("Electronics"))
                .thenReturn(Collections.singletonList(sampleProduct));

        List<Product> result = productService.findByCategory("Electronics");

        assertThat(result).containsExactly(sampleProduct);

        verify(auditService).logInfo(any(), eq("FILTER_PRODUCTS"), contains("Electronics"));
        verify(metricsService).stopTimer(eq("findByCategory"), anyLong());
    }

    @Test
    void count_shouldReturnTotalProducts() {
        when(productRepository.count()).thenReturn(5L);

        long total = productService.count();

        assertThat(total).isEqualTo(5L);

        verify(metricsService).setGauge("product.count", 5L);
    }

    @Test
    void save_shouldThrowException_whenNotAdmin() {
        when(authService.isAdmin()).thenReturn(false);

        Throwable thrown = catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                productService.save(new Product("Laptop", "Dell", "Electronics", 1200.0));
            }
        });

        assertThat(thrown).isInstanceOf(SecurityException.class);

        verify(auditService).logError(eq(testUser.getId()), eq("ACCESS_DENIED"), anyString());
    }

    @Test
    void update_shouldThrowException_whenProductNotFound() {
        when(productRepository.existsById(999L)).thenReturn(false);

        final Product p = new Product(
                999L, "Phone", "Samsung", "Electronics", 900.0, Instant.now(), Instant.now()
        );

        Throwable thrown = catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                productService.update(p);
            }
        });

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не найден");

        verify(auditService).logError(eq(testUser.getId()), eq("UPDATE_FAILED"), contains("does not exist"));
    }

    @Test
    void findByPriceRange_shouldThrowException_whenMinGreaterThanMax() {

        Throwable thrown = catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                productService.findByPriceRange(2000, 1000);
            }
        });

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Минимальная цена не может быть больше");

        verify(auditService).logError(eq(testUser.getId()), eq("INVALID_PRICE_RANGE"), anyString());
    }
}
