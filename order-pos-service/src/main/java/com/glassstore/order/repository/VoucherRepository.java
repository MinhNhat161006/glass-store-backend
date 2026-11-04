package com.glassstore.order.repository;

import com.glassstore.order.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeIgnoreCase(String code);
    default Optional<Voucher> findByCode(String code) { return findByCodeIgnoreCase(code); }

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Voucher v where upper(v.code)=upper(:code)")
    Optional<Voucher> lockByCode(@org.springframework.data.repository.query.Param("code") String code);
}
