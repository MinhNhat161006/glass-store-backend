package com.glassstore.lab.repository;

import com.glassstore.lab.entity.Warranty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, Long> {
    Optional<Warranty> findByWarrantyCode(String warrantyCode);
    List<Warranty> findAllByOrderByCreatedAtDesc();

    @Query("SELECT w FROM Warranty w WHERE " +
           "LOWER(w.warrantyCode) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(w.customerPhone) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(w.originalOrderCode) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(w.customerName) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY w.createdAt DESC")
    List<Warranty> search(@Param("q") String q);
}
