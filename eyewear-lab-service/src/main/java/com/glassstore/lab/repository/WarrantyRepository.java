package com.glassstore.lab.repository;

import com.glassstore.lab.entity.Warranty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, Long> {
    Optional<Warranty> findByWarrantyCode(String warrantyCode);
    List<Warranty> findAllByOrderByCreatedAtDesc();
}
