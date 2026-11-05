package com.glassstore.order.repository;

import com.glassstore.order.entity.LabDispatchOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface LabDispatchOutboxRepository extends JpaRepository<LabDispatchOutbox,Long> {
    Optional<LabDispatchOutbox> findByOrderId(Long orderId);
    List<LabDispatchOutbox> findTop20ByStatusOrderByCreatedAtAsc(String status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entry from LabDispatchOutbox entry where entry.id=:id")
    Optional<LabDispatchOutbox> lockById(@Param("id") Long id);
}
