package com.glassstore.order.repository;

import com.glassstore.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByStatusOrderByCreatedAtDesc(String status);
    List<Order> findByChannelOrderByCreatedAtDesc(String channel);
    List<Order> findByStatusAndChannelOrderByCreatedAtDesc(String status, String channel);
}
