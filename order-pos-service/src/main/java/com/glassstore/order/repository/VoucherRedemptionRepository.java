package com.glassstore.order.repository;
import com.glassstore.order.entity.VoucherRedemption;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption,Long> {
 long countByVoucherIdAndCustomerIdAndReversedFalse(Long voucherId,Long customerId);
 Optional<VoucherRedemption> findByOrderId(Long orderId);
}

