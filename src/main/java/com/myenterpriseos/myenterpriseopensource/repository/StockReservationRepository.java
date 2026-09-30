package com.myenterpriseos.myenterpriseopensource.repository;

import com.myenterpriseos.myenterpriseopensource.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    java.util.List<StockReservation> findByOrderItemId(Long orderItemId);
    java.util.List<StockReservation> findByInventoryIdAndStatus(Long inventoryId, com.myenterpriseos.myenterpriseopensource.enums.ReservationStatus status);
}
