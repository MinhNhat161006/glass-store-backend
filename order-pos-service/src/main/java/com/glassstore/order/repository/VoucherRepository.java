package com.glassstore.order.repository;
import com.glassstore.order.entity.Voucher;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface VoucherRepository extends JpaRepository<Voucher,Long> {
 Optional<Voucher> findByCode(String code);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select v from Voucher v where v.code=:code") Optional<Voucher> lockByCode(@Param("code") String code);
}

