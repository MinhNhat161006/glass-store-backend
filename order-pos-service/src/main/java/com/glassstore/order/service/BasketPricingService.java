package com.glassstore.order.service;

import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import com.glassstore.order.client.CatalogClient;
import com.glassstore.order.client.CatalogContracts;
import com.glassstore.order.domain.SalesChannel;
import com.glassstore.order.dto.OrderDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Authoritative basket pricing. Catalog-service is the only source of unit prices (BR-05).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BasketPricingService {

    static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final CatalogClient catalogClient;
    private final VoucherService voucherService;

    public OrderDtos.PricedBasket price(List<OrderDtos.CheckoutLine> items, String voucherCode,
                                        SalesChannel channel) {
        if (items == null || items.isEmpty()) {
            throw ApiException.of(ErrorCode.CART_EMPTY, "Đơn hàng phải có ít nhất một sản phẩm");
        }

        Map<String, Integer> quantities = new LinkedHashMap<>();
        Map<String, BigDecimal> expectedPrices = new HashMap<>();
        for (OrderDtos.CheckoutLine line : items) {
            String sku = line.getSku() == null ? "" : line.getSku().trim();
            if (sku.isEmpty()) {
                throw ApiException.of(ErrorCode.VALIDATION_ERROR, "SKU không được để trống");
            }
            int qty = line.getQuantity() == null ? 0 : line.getQuantity();
            if (qty <= 0) {
                throw ApiException.of(ErrorCode.INVALID_QUANTITY,
                        "Số lượng phải lớn hơn 0 cho SKU " + sku);
            }
            quantities.merge(sku, qty, Integer::sum);
            if (line.getExpectedUnitPrice() != null) {
                expectedPrices.put(sku, line.getExpectedUnitPrice());
            }
        }

        List<OrderDtos.OrderItemSnapshot> lines = new ArrayList<>();
        List<OrderDtos.PriceChange> changes = new ArrayList<>();
        BigDecimal total = ZERO;

        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            CatalogContracts.VariantInfo info = lookup(entry.getKey());
            requireSellable(info, entry.getKey());

            BigDecimal unitPrice = info.getPrice() == null ? ZERO
                    : info.getPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal expected = expectedPrices.get(entry.getKey());
            if (expected != null && expected.setScale(2, RoundingMode.HALF_UP).compareTo(unitPrice) != 0) {
                changes.add(OrderDtos.PriceChange.builder()
                        .sku(entry.getKey())
                        .expectedUnitPrice(expected)
                        .currentUnitPrice(unitPrice)
                        .build());
            }

            int qty = entry.getValue();
            if (info.getAvailableQuantity() != null && info.getAvailableQuantity() < qty) {
                throw ApiException.of(ErrorCode.INSUFFICIENT_STOCK,
                        String.format("SKU %s chỉ còn %d sản phẩm khả dụng, cần %d",
                                entry.getKey(), info.getAvailableQuantity(), qty),
                        Map.of("sku", entry.getKey(),
                                "requested", qty,
                                "available", info.getAvailableQuantity()));
            }

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty))
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(subtotal);
            lines.add(OrderDtos.OrderItemSnapshot.builder()
                    .sku(entry.getKey())
                    .productName(info.getName())
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build());
        }

        BigDecimal discount = voucherService.calculateDiscount(voucherCode, total, channel);
        BigDecimal finalAmount = total.subtract(discount).max(ZERO).setScale(2, RoundingMode.HALF_UP);

        return OrderDtos.PricedBasket.builder()
                .lines(lines)
                .totalAmount(total)
                .discountAmount(discount)
                .finalAmount(finalAmount)
                .priceChanges(changes)
                .build();
    }

    public CatalogContracts.VariantInfo lookup(String sku) {
        try {
            CatalogContracts.ApiEnvelope<CatalogContracts.VariantInfo> response = catalogClient.getBySku(sku);
            CatalogContracts.VariantInfo info = response == null ? null : response.getData();
            if (info == null) {
                throw ApiException.of(ErrorCode.VARIANT_NOT_FOUND, "Không tìm thấy SKU: " + sku);
            }
            return info;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Catalog lookup failed for SKU {}", sku, ex);
            throw ApiException.of(ErrorCode.UPSTREAM_UNAVAILABLE,
                    "Không thể kiểm tra sản phẩm " + sku + " lúc này, vui lòng thử lại");
        }
    }

    public CatalogContracts.VariantInfo lookupBarcode(String barcode) {
        try {
            CatalogContracts.ApiEnvelope<CatalogContracts.VariantInfo> response =
                    catalogClient.getByBarcode(barcode);
            CatalogContracts.VariantInfo info = response == null ? null : response.getData();
            if (info == null) {
                throw ApiException.of(ErrorCode.VARIANT_NOT_FOUND, "Không tìm thấy mã vạch: " + barcode);
            }
            return info;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Catalog barcode lookup failed for {}", barcode, ex);
            throw ApiException.of(ErrorCode.UPSTREAM_UNAVAILABLE,
                    "Không thể kiểm tra mã vạch lúc này, vui lòng thử lại");
        }
    }

    public List<CatalogContracts.VariantInfo> search(String keyword) {
        try {
            CatalogContracts.ApiEnvelope<List<CatalogContracts.VariantInfo>> response =
                    catalogClient.search(keyword);
            return response == null || response.getData() == null ? List.of() : response.getData();
        } catch (Exception ex) {
            log.error("Catalog search failed for {}", keyword, ex);
            throw ApiException.of(ErrorCode.UPSTREAM_UNAVAILABLE,
                    "Không thể tìm sản phẩm lúc này, vui lòng thử lại");
        }
    }

    public void requireSellable(CatalogContracts.VariantInfo info, String sku) {
        if ("DISCONTINUED".equalsIgnoreCase(info.getProductStatus())) {
            throw ApiException.of(ErrorCode.PRODUCT_DISCONTINUED,
                    "Sản phẩm " + info.getName() + " đã ngừng kinh doanh");
        }
        if (info.getStatus() != null && !"ACTIVE".equalsIgnoreCase(info.getStatus())) {
            throw ApiException.of(ErrorCode.VARIANT_INACTIVE,
                    "SKU " + sku + " hiện không được bán");
        }
    }
}
