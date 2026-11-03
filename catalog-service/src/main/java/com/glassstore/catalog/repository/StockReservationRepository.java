package com.glassstore.catalog.repository;

import com.glassstore.catalog.entity.ReservationStatus;
import com.glassstore.catalog.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    Optional<StockReservation> findByReservationKey(String reservationKey);
    List<StockReservation> findAllByReservationKey(String reservationKey);
    List<StockReservation> findByOrderCodeAndStatus(String orderCode, ReservationStatus status);
    List<StockReservation> findByReservationKeyAndStatus(String reservationKey, ReservationStatus status);
}