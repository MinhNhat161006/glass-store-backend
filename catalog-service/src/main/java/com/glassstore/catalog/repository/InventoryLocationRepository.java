package com.glassstore.catalog.repository;

import com.glassstore.catalog.entity.InventoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryLocationRepository extends JpaRepository<InventoryLocation, Long> {
    Optional<InventoryLocation> findByCode(String code);
    List<InventoryLocation> findAllByActiveTrueOrderByCodeAsc();
}
