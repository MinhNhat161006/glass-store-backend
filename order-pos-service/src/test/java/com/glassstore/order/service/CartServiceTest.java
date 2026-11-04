package com.glassstore.order.service;

import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import com.glassstore.order.client.CatalogContracts;
import com.glassstore.order.dto.CartDtos;
import com.glassstore.order.entity.Cart;
import com.glassstore.order.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CartServiceTest {

    private CartRepository cartRepository;
    private BasketPricingService pricingService;
    private CartService service;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        pricingService = mock(BasketPricingService.class);
        service = new CartService(cartRepository, pricingService);
        AtomicLong ids = new AtomicLong(1);
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> {
            Cart cart = inv.getArgument(0);
            if (cart.getId() == null) {
                cart.setId(ids.getAndIncrement());
            }
            cart.getItems().forEach(item -> {
                if (item.getId() == null) {
                    item.setId(ids.getAndIncrement());
                }
            });
            return cart;
        });
        when(cartRepository.findByCustomerId(5L)).thenReturn(Optional.empty());
        CatalogContracts.VariantInfo info = new CatalogContracts.VariantInfo();
        info.setSku("SKU-1");
        info.setName("Gọng");
        info.setPrice(new BigDecimal("150000"));
        info.setStatus("ACTIVE");
        info.setProductStatus("ACTIVE");
        info.setAvailableQuantity(10);
        when(pricingService.lookup("SKU-1")).thenReturn(info);
    }

    @Test
    @DisplayName("Add to cart recalculates live catalog price")
    void addItem() {
        CartDtos.CartView view = service.addItem(5L, null, CartDtos.AddItemRequest.builder()
                .sku("SKU-1").quantity(2).build());
        assertThat(view.getItems()).hasSize(1);
        assertThat(view.getTotalAmount()).isEqualByComparingTo("300000");
    }

    @Test
    @DisplayName("Quantity must be positive")
    void invalidQty() {
        Cart cart = Cart.builder().id(1L).customerId(5L).build();
        when(cartRepository.findByCustomerId(5L)).thenReturn(Optional.of(cart));
        assertThatThrownBy(() -> service.updateQuantity(5L, null, 1L, 0))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_QUANTITY);
    }
}
