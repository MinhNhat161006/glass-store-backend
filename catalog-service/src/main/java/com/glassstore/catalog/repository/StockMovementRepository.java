package com.glassstore.catalog.repository;

import com.glassstore.catalog.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findBySkuOrderByCreatedAtDesc(String sku);
    List<StockMovement> findByReferenceOrderByCreatedAtAsc(String reference);
}