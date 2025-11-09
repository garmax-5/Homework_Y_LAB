import com.marketplace.model.Product;
import com.marketplace.model.User;
import com.marketplace.out.repository.ProductRepository;
import com.marketplace.service.*;
import com.marketplace.validation.ProductValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    private ProductRepository productRepository;
    private AuditService auditService;
    private AuthService authService;
    private MetricsService metricsService;
    private ProductValidator validator;

    private ProductService productService;

    private final Product sampleProduct = new Product(1, "Laptop", "Dell", "Electronics", 1200.0);
    private final User testUser = new User(2, "admin1", "pass", User.Role.ADMIN);

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

    // Успешные сценарии
    @Test
    void save_shouldSaveNewProduct_whenValidAndAdmin() {
        when(productRepository.findById(sampleProduct.getId())).thenReturn(Optional.empty());
        when(productRepository.save(sampleProduct)).thenReturn(sampleProduct);
        when(productRepository.count()).thenReturn(1L);

        Product result = productService.save(sampleProduct);

        assertThat(result).isEqualTo(sampleProduct);
        verify(validator).validate(eq(sampleProduct), eq(testUser.getId()));
        verify(productRepository).save(sampleProduct);
        verify(auditService).logInfo(eq(testUser.getId()), eq("ADD_PRODUCT"), contains("Laptop"));
        verify(metricsService).increment("product.added");
        verify(metricsService).setGauge(eq("product.count"), eq(1L));
    }

    @Test
    void update_shouldUpdateExistingProduct_whenValidAndAdmin() {
        when(productRepository.findById(sampleProduct.getId())).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(sampleProduct)).thenReturn(sampleProduct);
        when(productRepository.count()).thenReturn(1L);

        Product result = productService.update(sampleProduct);

        assertThat(result).isEqualTo(sampleProduct);
        verify(validator).validate(eq(sampleProduct), eq(testUser.getId()));
        verify(productRepository).save(sampleProduct);
        verify(auditService).logInfo(eq(testUser.getId()), eq("UPDATE_PRODUCT"), contains("Laptop"));
        verify(metricsService).increment("product.updated");
    }

    @Test
    void deleteById_shouldDeleteProduct_whenAdminAndExists() {
        when(authService.isAdmin()).thenReturn(true);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(productRepository.deleteById(1L)).thenReturn(true);
        when(productRepository.count()).thenReturn(0L);

        boolean result = productService.deleteById(1L);

        assertThat(result).isTrue();

        verify(auditService).logInfo(eq(testUser.getId()), eq("DELETE_PRODUCT"), contains("1"));
        verify(metricsService).increment("product.deleted");
        verify(metricsService).setGauge(eq("product.count"), eq(0L));
    }




    @Test
    void findByBrand_shouldReturnFilteredProducts() {
        List<Product> dellProducts = List.of(sampleProduct);
        when(productRepository.findByBrand("Dell")).thenReturn(dellProducts);

        List<Product> result = productService.findByBrand("Dell");

        assertThat(result).hasSize(1).containsExactly(sampleProduct);
        verify(productRepository).findByBrand("Dell");
        verify(auditService).logInfo(any(), eq("FILTER_PRODUCTS"), contains("Dell"));
        verify(metricsService).stopTimer(eq("findByBrand"), anyLong());
    }

    @Test
    void findByCategory_shouldReturnFilteredProducts() {
        List<Product> electronics = List.of(sampleProduct);
        when(productRepository.findByCategory("Electronics")).thenReturn(electronics);

        List<Product> result = productService.findByCategory("Electronics");

        assertThat(result).hasSize(1).containsExactly(sampleProduct);
        verify(productRepository).findByCategory("Electronics");
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

    // Неуспешные сценарии

    @Test
    void save_shouldThrowSecurityException_whenUserIsNotAdmin() {
        when(authService.isAdmin()).thenReturn(false);

        assertThatThrownBy(() -> productService.save(sampleProduct))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Доступ запрещен");

        verify(auditService).logError(eq(testUser.getId()), eq("ACCESS_DENIED"), contains("Admin role required"));
    }

    @Test
    void save_shouldThrowIllegalArgumentException_whenProductAlreadyExists() {
        when(productRepository.findById(sampleProduct.getId())).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> productService.save(sampleProduct))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("уже существует");

        verify(auditService).logError(eq(testUser.getId()), eq("DUPLICATE_PRODUCT_ID"), contains("already exists"));
    }

    @Test
    void save_shouldThrowIllegalArgumentException_whenValidationFails() {
        doThrow(new IllegalArgumentException("Invalid data"))
                .when(validator).validate(eq(sampleProduct), eq(testUser.getId()));

        when(productRepository.findById(sampleProduct.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.save(sampleProduct))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid data");
    }

    @Test
    void update_shouldThrowIllegalArgumentException_whenProductNotFound() {
        when(productRepository.findById(sampleProduct.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(sampleProduct))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не найден");

        verify(auditService).logError(eq(testUser.getId()), eq("UPDATE_FAILED"), contains("does not exist"));
    }

    @Test
    void findByPriceRange_shouldThrowException_whenMinGreaterThanMax() {
        assertThatThrownBy(() -> productService.findByPriceRange(2000.0, 1000.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Минимальная цена не может быть больше");

        verify(auditService).logError(eq(testUser.getId()), eq("INVALID_PRICE_RANGE"), anyString());
    }
}
