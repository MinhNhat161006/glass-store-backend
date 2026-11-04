package com.glassstore.order.repository;

import com.glassstore.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Order o where o.id=:id")
    Optional<Order> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findAllByOrderByCreatedAtDesc();
}
