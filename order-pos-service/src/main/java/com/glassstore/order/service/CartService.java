package com.glassstore.order.service;

import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import com.glassstore.order.client.CatalogContracts;
import com.glassstore.order.dto.CartDtos;
import com.glassstore.order.dto.OrderDtos;
import com.glassstore.order.entity.Cart;
import com.glassstore.order.entity.CartItem;
import com.glassstore.order.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final BasketPricingService pricingService;

    @Transactional
    public CartDtos.CartView getOrCreate(Long customerId, String sessionToken) {
        Cart cart = resolve(customerId, sessionToken, true);
        return toView(cart);
    }

    @Transactional
    public CartDtos.CartView addItem(Long customerId, String sessionToken, CartDtos.AddItemRequest request) {
        Cart cart = resolve(customerId, sessionToken, true);
        CatalogContracts.VariantInfo info = pricingService.lookup(request.getSku().trim());
        pricingService.requireSellable(info, info.getSku());
        if (info.getAvailableQuantity() != null && info.getAvailableQuantity() < request.getQuantity()) {
            throw ApiException.of(ErrorCode.INSUFFICIENT_STOCK,
                    "Không đủ tồn kho khả dụng cho SKU " + info.getSku());
        }

        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getSku().equalsIgnoreCase(info.getSku()))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            cart.getItems().add(CartItem.builder()
                    .cart(cart)
                    .sku(info.getSku())
                    .productName(info.getName())
                    .quantity(request.getQuantity())
                    .quotedUnitPrice(info.getPrice())
                    .updatedAt(LocalDateTime.now())
                    .build());
        } else {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            existing.setQuotedUnitPrice(info.getPrice());
            existing.setProductName(info.getName());
            existing.setUpdatedAt(LocalDateTime.now());
        }
        cart.setUpdatedAt(LocalDateTime.now());
        return toView(cartRepository.save(cart));
    }

    @Transactional
    public CartDtos.CartView updateQuantity(Long customerId, String sessionToken, Long itemId, int quantity) {
        if (quantity <= 0) {
            throw ApiException.of(ErrorCode.INVALID_QUANTITY, "Số lượng phải lớn hơn 0");
        }
        Cart cart = resolve(customerId, sessionToken, false);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND, "Không tìm thấy sản phẩm trong giỏ"));
        CatalogContracts.VariantInfo info = pricingService.lookup(item.getSku());
        pricingService.requireSellable(info, item.getSku());
        item.setQuantity(quantity);
        item.setQuotedUnitPrice(info.getPrice());
        item.setUpdatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());
        return toView(cartRepository.save(cart));
    }

    @Transactional
    public CartDtos.CartView removeItem(Long customerId, String sessionToken, Long itemId) {
        Cart cart = resolve(customerId, sessionToken, false);
        boolean removed = cart.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw ApiException.of(ErrorCode.NOT_FOUND, "Không tìm thấy sản phẩm trong giỏ");
        }
        cart.setUpdatedAt(LocalDateTime.now());
        return toView(cartRepository.save(cart));
    }

    @Transactional
    public void clear(Cart cart) {
        cart.getItems().clear();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    public Cart requireCart(Long customerId, String sessionToken) {
        return resolve(customerId, sessionToken, false);
    }

    public List<OrderDtos.CheckoutLine> toCheckoutLines(Cart cart) {
        if (cart.getItems().isEmpty()) {
            throw ApiException.of(ErrorCode.CART_EMPTY, "Giỏ hàng đang trống");
        }
        return cart.getItems().stream()
                .map(i -> OrderDtos.CheckoutLine.builder()
                        .sku(i.getSku())
                        .quantity(i.getQuantity())
                        .expectedUnitPrice(i.getQuotedUnitPrice())
                        .build())
                .toList();
    }

    private Cart resolve(Long customerId, String sessionToken, boolean create) {
        if (customerId != null) {
            return cartRepository.findByCustomerId(customerId)
                    .orElseGet(() -> {
                        if (!create) {
                            throw ApiException.of(ErrorCode.CART_NOT_FOUND, "Không tìm thấy giỏ hàng");
                        }
                        return cartRepository.save(Cart.builder().customerId(customerId).build());
                    });
        }
        String token = sessionToken == null || sessionToken.isBlank() ? null : sessionToken.trim();
        if (token != null) {
            return cartRepository.findBySessionToken(token)
                    .orElseGet(() -> {
                        if (!create) {
                            throw ApiException.of(ErrorCode.CART_NOT_FOUND, "Không tìm thấy giỏ hàng");
                        }
                        return cartRepository.save(Cart.builder().sessionToken(token).build());
                    });
        }
        if (!create) {
            throw ApiException.of(ErrorCode.CART_NOT_FOUND, "Không tìm thấy giỏ hàng");
        }
        return cartRepository.save(Cart.builder().sessionToken(UUID.randomUUID().toString()).build());
    }

    private CartDtos.CartView toView(Cart cart) {
        List<CartDtos.CartLineView> lines = new ArrayList<>();
        List<OrderDtos.PriceChange> changes = new ArrayList<>();
        BigDecimal total = BasketPricingService.ZERO;
        for (CartItem item : cart.getItems()) {
            CatalogContracts.VariantInfo info = pricingService.lookup(item.getSku());
            BigDecimal current = info.getPrice() == null ? BasketPricingService.ZERO
                    : info.getPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal quoted = item.getQuotedUnitPrice() == null ? current
                    : item.getQuotedUnitPrice().setScale(2, RoundingMode.HALF_UP);
            if (quoted.compareTo(current) != 0) {
                changes.add(OrderDtos.PriceChange.builder()
                        .sku(item.getSku())
                        .expectedUnitPrice(quoted)
                        .currentUnitPrice(current)
                        .build());
            }
            BigDecimal subtotal = current.multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(subtotal);
            lines.add(CartDtos.CartLineView.builder()
                    .itemId(item.getId())
                    .sku(item.getSku())
                    .productName(info.getName())
                    .quantity(item.getQuantity())
                    .quotedUnitPrice(quoted)
                    .currentUnitPrice(current)
                    .subtotal(subtotal)
                    .availableQuantity(info.getAvailableQuantity())
                    .build());
        }
        return CartDtos.CartView.builder()
                .cartId(cart.getId())
                .customerId(cart.getCustomerId())
                .sessionToken(cart.getSessionToken())
                .items(lines)
                .totalAmount(total)
                .priceChanges(changes)
                .build();
    }
}
