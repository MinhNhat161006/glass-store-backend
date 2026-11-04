package com.glassstore.catalog.service;

import com.glassstore.catalog.CatalogApplication;
import com.glassstore.catalog.dto.StockDtos;
import com.glassstore.catalog.entity.ProductVariant;
import com.glassstore.catalog.repository.ProductVariantRepository;
import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import jakarta.persistence.EntityManager;
import com.glassstore.catalog.entity.Category;
import com.glassstore.catalog.entity.Brand;
import com.glassstore.catalog.entity.Product;
import com.glassstore.catalog.entity.ProductType;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = CatalogApplication.class)
@ActiveProfiles("test")
@Transactional
class StockServiceImplTest {

    @Autowired
    private EntityManager entityManager;

    private String testSku;

    @BeforeEach
    void createTestStock() {
        String suffix = UUID.randomUUID().toString();
        Category category = Category.builder().name("Test frames").slug("frames-" + suffix).build();
        entityManager.persist(category);
        Brand brand = Brand.builder().name("Test brand " + suffix).build();
        entityManager.persist(brand);
        Product product = Product.builder().name("Test frame").slug("frame-" + suffix)
                .category(category).brand(brand).productType(ProductType.FRAME).build();
        entityManager.persist(product);
        testSku = "TEST-" + suffix;
        variantRepository.saveAndFlush(ProductVariant.builder().product(product).sku(testSku)
                .barcode(testSku).name("Test frame").price(BigDecimal.TEN).costPrice(BigDecimal.ONE)
                .stockQuantity(5).build());
    }

    @Autowired
    private StockService stockService;
    @Autowired
    private ProductVariantRepository variantRepository;

    @Test
    @DisplayName("Reserve is idempotent on reservationKey")
    void reserveReplay() {
        String sku = firstSku();
        StockDtos.ReserveRequest request = StockDtos.ReserveRequest.builder()
                .reservationKey("idem-1")
                .orderCode("ORD-T1")
                .actor("TEST")
                .lines(List.of(StockDtos.ReservationLine.builder().sku(sku).quantity(1).build()))
                .build();
        StockDtos.ReservationResult first = stockService.reserve(request);
        StockDtos.ReservationResult second = stockService.reserve(request);
        assertThat(first.isReplayed()).isFalse();
        assertThat(second.isReplayed()).isTrue();
        entityManager.flush();
        entityManager.clear();
        assertThat(variantRepository.findBySku(sku).orElseThrow().getReservedQuantity()).isEqualTo(1);
        stockService.release(StockDtos.ReleaseRequest.builder()
                .reservationKey("idem-1").actor("TEST").reason("cleanup").build());
        entityManager.flush();
        entityManager.clear();
        assertThat(variantRepository.findBySku(sku).orElseThrow().getReservedQuantity()).isZero();
    }

    @Test
    @DisplayName("Oversell is rejected and leaves no partial hold (EX-INV-05)")
    void oversell() {
        String sku = firstSku();
        ProductVariant variant = variantRepository.findBySku(sku).orElseThrow();
        int available = variant.getAvailableQuantity();
        assertThatThrownBy(() -> stockService.reserve(StockDtos.ReserveRequest.builder()
                .reservationKey("too-many")
                .orderCode("ORD-T2")
                .actor("TEST")
                .lines(List.of(StockDtos.ReservationLine.builder()
                        .sku(sku).quantity(available + 50).build()))
                .build()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
        entityManager.clear();
        ProductVariant unchanged = variantRepository.findBySku(sku).orElseThrow();
        assertThat(unchanged.getStockQuantity()).isEqualTo(5);
        assertThat(unchanged.getReservedQuantity()).isZero();
    }

    @Test
    @DisplayName("Stock adjustment requires a reason (BR-12)")
    void adjustNeedsReason() {
        assertThatThrownBy(() -> stockService.adjust(firstSku(), -1, "TEST", " "))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    private String firstSku() {
        return testSku;
    }
}
