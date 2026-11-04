package com.glassstore.lab.repository;

import com.glassstore.lab.entity.Warranty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, Long> {
    Optional<Warranty> findByWarrantyCode(String warrantyCode);

    List<Warranty> findByCustomerPhoneOrderByCreatedAtDesc(String customerPhone);

    List<Warranty> findByProductSkuOrderByCreatedAtDesc(String productSku);

    List<Warranty> findByOriginalOrderCodeOrderByCreatedAtDesc(String originalOrderCode);

    List<Warranty> findByOrderItemIdOrderByCreatedAtDesc(Long orderItemId);

    Optional<Warranty> findByOrderItemIdAndBenefitType(Long orderItemId, String benefitType);

    List<Warranty> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query(value = """
            INSERT INTO warranties
                (warranty_code, original_order_code, order_id, order_item_id, benefit_type,
                 activated_at, expires_at, product_barcode, customer_id, customer_name,
                 customer_phone, product_name, product_sku, product_details, policy_ref,
                 status, status_label, created_at, updated_at)
            VALUES
                (:warrantyCode, :orderCode, :orderId, :orderItemId, :benefitType,
                 :activatedAt, :expiresAt, :barcode, :customerId, :customerName,
                 :customerPhone, :productName, :productSku, :productDetails, :policyRef,
                 :status, :statusLabel, :createdAt, :updatedAt)
            ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)
            """, nativeQuery = true)
    int insertIfMissing(
            @Param("warrantyCode") String warrantyCode,
            @Param("orderCode") String orderCode,
            @Param("orderId") Long orderId,
            @Param("orderItemId") Long orderItemId,
            @Param("benefitType") String benefitType,
            @Param("activatedAt") java.time.LocalDateTime activatedAt,
            @Param("expiresAt") java.time.LocalDateTime expiresAt,
            @Param("barcode") String barcode,
            @Param("customerId") Long customerId,
            @Param("customerName") String customerName,
            @Param("customerPhone") String customerPhone,
            @Param("productName") String productName,
            @Param("productSku") String productSku,
            @Param("productDetails") String productDetails,
            @Param("policyRef") String policyRef,
            @Param("status") String status,
            @Param("statusLabel") String statusLabel,
            @Param("createdAt") java.time.LocalDateTime createdAt,
            @Param("updatedAt") java.time.LocalDateTime updatedAt);
}
