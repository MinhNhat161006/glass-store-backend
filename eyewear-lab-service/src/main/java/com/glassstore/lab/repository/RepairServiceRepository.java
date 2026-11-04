package com.glassstore.lab.repository;

import com.glassstore.lab.entity.RepairService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepairServiceRepository extends JpaRepository<RepairService, Long> {
    List<RepairService> findAllByOrderByCreatedAtDesc();

    List<RepairService> findByCustomerPhoneOrderByCreatedAtDesc(String customerPhone);

    List<RepairService> findByProductBarcodeOrderByCreatedAtDesc(String productBarcode);
}
