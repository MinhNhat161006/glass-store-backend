package com.glassstore.catalog.repository;

import com.glassstore.catalog.entity.StockTransfer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    @EntityGraph(attributePaths = {"sourceLocation", "destinationLocation", "items"})
    List<StockTransfer> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct t from StockTransfer t left join fetch t.items where t.id = :id")
    Optional<StockTransfer> findByIdForUpdate(@Param("id") Long id);
}
