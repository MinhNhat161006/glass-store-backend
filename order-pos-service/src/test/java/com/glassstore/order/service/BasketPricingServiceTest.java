package com.glassstore.order.service;

import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import com.glassstore.order.client.CatalogClient;
import com.glassstore.order.client.CatalogContracts;
import com.glassstore.order.domain.SalesChannel;
import com.glassstore.order.dto.OrderDtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BasketPricingServiceTest {

    private CatalogClient catalogClient;
    private VoucherService voucherService;
    private BasketPricingService service;

    @BeforeEach
    void setUp() {
        catalogClient = mock(CatalogClient.class);
        voucherService = mock(VoucherService.class);
        service = new BasketPricingService(catalogClient, voucherService);
        when(voucherService.calculateDiscount(any(), any(), any())).thenReturn(BigDecimal.ZERO.setScale(2));
    }

    @Test
    @DisplayName("Basket uses catalog price, not the client-submitted amount")
    void usesCatalogPrice() {
        stubSku("SKU-1", "Item", "100.00", "ACTIVE", "ACTIVE", 5);
        OrderDtos.PricedBasket basket = service.price(List.of(OrderDtos.CheckoutLine.builder()
                .sku("SKU-1").quantity(2).expectedUnitPrice(new BigDecimal("100.00")).build()),
                null, SalesChannel.ONLINE);
        assertThat(basket.getFinalAmount()).isEqualByComparingTo("200.00");
        assertThat(basket.getLines().get(0).getUnitPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Discontinued products cannot enter a new order (BR-04)")
    void discontinued() {
        stubSku("SKU-X", "Old", "10.00", "ACTIVE", "DISCONTINUED", 9);
        assertThatThrownBy(() -> service.price(List.of(OrderDtos.CheckoutLine.builder()
                .sku("SKU-X").quantity(1).build()), null, SalesChannel.ONLINE))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_DISCONTINUED);
    }

    @Test
    @DisplayName("Out-of-stock checkout is rejected (EX-ORD-01)")
    void outOfStock() {
        stubSku("SKU-1", "Item", "10.00", "ACTIVE", "ACTIVE", 0);
        assertThatThrownBy(() -> service.price(List.of(OrderDtos.CheckoutLine.builder()
                .sku("SKU-1").quantity(1).build()), null, SalesChannel.ONLINE))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("Price drift is reported so checkout can ask for confirmation")
    void priceDrift() {
        stubSku("SKU-1", "Item", "12.00", "ACTIVE", "ACTIVE", 3);
        OrderDtos.PricedBasket basket = service.price(List.of(OrderDtos.CheckoutLine.builder()
                .sku("SKU-1").quantity(1).expectedUnitPrice(new BigDecimal("10.00")).build()),
                null, SalesChannel.ONLINE);
        assertThat(basket.getPriceChanges()).hasSize(1);
        assertThat(basket.getPriceChanges().get(0).getCurrentUnitPrice()).isEqualByComparingTo("12.00");
    }

    private void stubSku(String sku, String name, String price, String status, String productStatus, int available) {
        CatalogContracts.VariantInfo info = new CatalogContracts.VariantInfo();
        info.setSku(sku);
        info.setName(name);
        info.setPrice(new BigDecimal(price));
        info.setStatus(status);
        info.setProductStatus(productStatus);
        info.setAvailableQuantity(available);
        CatalogContracts.ApiEnvelope<CatalogContracts.VariantInfo> envelope = new CatalogContracts.ApiEnvelope<>();
        envelope.setData(info);
        when(catalogClient.getBySku(sku)).thenReturn(envelope);
    }
}
