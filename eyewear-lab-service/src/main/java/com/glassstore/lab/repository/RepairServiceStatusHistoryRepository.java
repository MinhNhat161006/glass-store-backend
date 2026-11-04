package com.glassstore.lab.repository;

import com.glassstore.lab.entity.RepairServiceStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepairServiceStatusHistoryRepository extends JpaRepository<RepairServiceStatusHistory, Long> {
    List<RepairServiceStatusHistory> findByRepairServiceIdOrderByChangedAtAsc(Long repairServiceId);
}
