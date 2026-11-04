package com.glassstore.lab.service;

import com.glassstore.common.dto.ApiResponse;
import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import com.glassstore.lab.client.WarrantyCatalogClient;
import com.glassstore.lab.client.WarrantyOrderClient;
import com.glassstore.lab.dto.CatalogVariantInfo;
import com.glassstore.lab.dto.WarrantyActivationRequest;
import com.glassstore.lab.dto.WarrantyLookupItemDTO;
import com.glassstore.lab.dto.WarrantyOrderItemDTO;
import com.glassstore.lab.entity.Warranty;
import com.glassstore.lab.repository.WarrantyRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WarrantyService {

    private final WarrantyRepository warrantyRepository;
    private final WarrantyOrderClient orderClient;
    private final WarrantyCatalogClient catalogClient;

    @Transactional
    public Warranty activate(WarrantyActivationRequest request) {
        WarrantyOrderItemDTO item = getPurchaseItem(request.orderId(), request.orderItemId());
        if (!isEligibleForBenefit(item.productSku(), request.benefitType())) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR,
                    "Sản phẩm không đủ điều kiện cho quyền lợi bảo hành này");
        }
        if (warrantyRepository.findByOrderItemIdAndBenefitType(item.orderItemId(), request.benefitType()).isPresent()) {
            return warrantyRepository.findByOrderItemIdAndBenefitType(item.orderItemId(), request.benefitType())
                    .orElseThrow();
        }

        CatalogVariantInfo variant = getVariantBySku(item.productSku());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime purchaseDate = item.purchaseDate() == null ? now : item.purchaseDate();
        boolean lensCoating = "LENS_COATING".equals(request.benefitType());
        boolean exchange = "EXCHANGE_30_DAYS".equals(request.benefitType());
        LocalDateTime expiresAt = lensCoating || exchange ? purchaseDate.plusDays(30) : null;
        String status = expiresAt != null && expiresAt.isBefore(now) ? "EXPIRED" : "ACTIVE";
        String statusLabel = "EXPIRED".equals(status) ? "Đã hết hạn" : "Đang hiệu lực";
        String policy = exchange ? "EXCHANGE_30_DAYS" : lensCoating ? "LENS_COATING_30_DAYS" : "FRAME_ADJUSTMENT_LIFETIME";

        warrantyRepository.insertIfMissing(
                "WAR-" + UUID.randomUUID(),
                item.orderCode(),
                item.orderId(),
                item.orderItemId(),
                request.benefitType(),
                now,
                expiresAt,
                variant == null ? null : variant.barcode(),
                item.customerId(),
                item.customerName(),
                item.customerPhone(),
                item.productName(),
                item.productSku(),
                exchange ? "Đổi 1–1 trong 30 ngày từ ngày mua, sau khi kiểm tra điều kiện bảo hành"
                        : lensCoating ? "Thay thế một lần khi lớp phủ tròng kính bong/tróc do lỗi thuộc phạm vi bảo hành"
                        : "Hỗ trợ điều chỉnh gọng kính trọn đời",
                policy,
                status,
                statusLabel,
                now,
                now);
        return warrantyRepository.findByOrderItemIdAndBenefitType(item.orderItemId(), request.benefitType())
                .orElseThrow(() -> new IllegalStateException("Không thể đọc lại bản ghi bảo hành vừa tạo"));
    }

    @Transactional(readOnly = true)
    public List<WarrantyLookupItemDTO> lookupByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "Cần nhập số điện thoại khách hàng");
        }
        return toLookupItems(orderClient.findPurchases(phone.trim(), null));
    }

    @Transactional(readOnly = true)
    public List<WarrantyLookupItemDTO> lookupByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "Cần nhập barcode sản phẩm");
        }
        CatalogVariantInfo variant = getVariantByBarcode(barcode.trim());
        if (variant == null || variant.sku() == null) {
            return List.of();
        }
        return toLookupItems(orderClient.findPurchases(null, variant.sku()));
    }

    private WarrantyOrderItemDTO getPurchaseItem(Long orderId, Long orderItemId) {
        try {
            WarrantyOrderItemDTO item = orderClient.getPurchaseItem(orderId, orderItemId);
            if (item == null || !orderId.equals(item.orderId()) || !orderItemId.equals(item.orderItemId())) {
                throw ApiException.of(ErrorCode.VALIDATION_ERROR,
                        "Sản phẩm không thuộc đơn hàng đã chỉ định");
            }
            return item;
        } catch (FeignException.NotFound exception) {
            throw ApiException.of(ErrorCode.ORDER_NOT_FOUND,
                    "Không tìm thấy đơn hàng hoặc sản phẩm trong đơn hàng");
        } catch (FeignException exception) {
            throw ApiException.of(ErrorCode.UPSTREAM_UNAVAILABLE,
                    "Không thể xác thực thông tin đơn hàng với POS");
        }
    }

    private List<WarrantyLookupItemDTO> toLookupItems(List<WarrantyOrderItemDTO> purchases) {
        return purchases.stream().map(item -> {
            List<Warranty> warranties = warrantyRepository.findByOrderItemIdOrderByCreatedAtDesc(item.orderItemId());
            CatalogVariantInfo variant = getVariantBySku(item.productSku());
            String activationStatus = warranties.isEmpty()
                    ? item.activationStatus()
                    : warranties.stream().anyMatch(warranty -> isExpired(warranty)) ? "EXPIRED" : "ACTIVE";
            return new WarrantyLookupItemDTO(
                    item.orderId(),
                    item.orderItemId(),
                    item.orderCode(),
                    item.purchaseDate(),
                    item.customerName(),
                    item.customerPhone(),
                    item.productName(),
                    item.productSku(),
                    variant == null ? null : variant.barcode(),
                    item.benefitType(),
                    activationStatus,
                    item.activationAttempts(),
                    item.activationError(),
                    warranties);
        }).toList();
    }

    private CatalogVariantInfo getVariantByBarcode(String barcode) {
        try {
            ApiResponse<CatalogVariantInfo> response = catalogClient.findByBarcode(barcode);
            return response == null ? null : response.getData();
        } catch (FeignException.NotFound exception) {
            return null;
        } catch (FeignException exception) {
            throw ApiException.of(ErrorCode.UPSTREAM_UNAVAILABLE,
                    "Không thể xác thực barcode với catalog service");
        }
    }

    private CatalogVariantInfo getVariantBySku(String sku) {
        try {
            ApiResponse<CatalogVariantInfo> response = catalogClient.findBySku(sku);
            return response == null ? null : response.getData();
        } catch (FeignException.NotFound exception) {
            return null;
        } catch (FeignException exception) {
            throw ApiException.of(ErrorCode.UPSTREAM_UNAVAILABLE,
                    "Không thể tra cứu sản phẩm trong catalog service");
        }
    }

    private boolean isEligibleForBenefit(String sku, String benefitType) {
        if (sku == null || benefitType == null) {
            return false;
        }
        String normalized = sku.trim().toUpperCase();
        return ("EXCHANGE_30_DAYS".equals(benefitType) && (normalized.startsWith("FRM-") || normalized.startsWith("LNS-") || normalized.startsWith("LZ-")))
                || ("FRAME_ADJUSTMENT".equals(benefitType) && normalized.startsWith("FRM-"))
                || ("LENS_COATING".equals(benefitType)
                && (normalized.startsWith("LNS-") || normalized.startsWith("LZ-")));
    }

    private boolean isExpired(Warranty warranty) {
        return warranty.getExpiresAt() != null && warranty.getExpiresAt().isBefore(LocalDateTime.now());
    }
}
