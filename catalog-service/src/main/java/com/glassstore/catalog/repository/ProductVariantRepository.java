package com.glassstore.catalog.repository;

import com.glassstore.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findBySku(String sku);
    Optional<ProductVariant> findByBarcode(String barcode);
    boolean existsBySku(String sku);

    @Modifying
    @Query("""
            update ProductVariant v
            set v.stockQuantity = v.stockQuantity - :quantity,
                v.updatedAt = CURRENT_TIMESTAMP
            where v.sku = :sku and v.stockQuantity >= :quantity
            """)
    int decrementStockIfAvailable(@Param("sku") String sku, @Param("quantity") int quantity);

    @Modifying
    @Query("""
            update ProductVariant v
            set v.stockQuantity = v.stockQuantity + :quantity,
                v.updatedAt = CURRENT_TIMESTAMP
            where v.sku = :sku
            """)
    int incrementStock(@Param("sku") String sku, @Param("quantity") int quantity);
}
