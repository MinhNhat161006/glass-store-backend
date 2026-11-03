package com.glassstore.catalog.repository;

import com.glassstore.catalog.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findBySku(String sku);
    Optional<ProductVariant> findByBarcode(String barcode);

    /**
     * Loads variants for a stock mutation with a row-level write lock, so concurrent
     * checkouts for the same SKU are serialised instead of overselling (EX-INV-05, BR-09).
     * Callers must sort the SKU list before calling to avoid lock-order deadlocks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.sku IN :skus ORDER BY v.sku")
    List<ProductVariant> findAllBySkuInForUpdate(@Param("skus") List<String> skus);
}