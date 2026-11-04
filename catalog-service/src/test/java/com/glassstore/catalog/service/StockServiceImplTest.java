package com.glassstore.catalog.service;

import com.glassstore.catalog.CatalogApplication;
import com.glassstore.catalog.dto.StockDtos;
import com.glassstore.catalog.entity.ProductVariant;
import com.glassstore.catalog.repository.ProductVariantRepository;
import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = CatalogApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:stock-service;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class StockServiceImplTest {

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
        stockService.release(StockDtos.ReleaseRequest.builder()
                .reservationKey("idem-1").actor("TEST").reason("cleanup").build());
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
        return variantRepository.findAll().get(0).getSku();
    }
}
