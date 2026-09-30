package com.myenterpriseos.myenterpriseopensource.repository;

import com.myenterpriseos.myenterpriseopensource.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    java.util.List<StockReservation> findByOrderItemId(Long orderItemId);
    java.util.List<StockReservation> findByInventoryIdAndStatus(Long inventoryId, com.myenterpriseos.myenterpriseopensource.enums.ReservationStatus status);
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(r.quantity), 0) from StockReservation r where r.inventory.id = :inventoryId and r.status = :status")
    long sumQuantityByInventoryIdAndStatus(Long inventoryId, com.myenterpriseos.myenterpriseopensource.enums.ReservationStatus status);
    @org.springframework.data.jpa.repository.Query(value =
            "select quantity from stock_reservation where inventory_id = :inventoryId and status = 'ACTIVE' for update",
            nativeQuery = true)
    java.util.List<Integer> activeQuantitiesForUpdate(Long inventoryId);
    @org.springframework.data.jpa.repository.Query("select r.inventory.id, sum(r.quantity) from StockReservation r where r.inventory.id in :inventoryIds and r.status = :status group by r.inventory.id")
    java.util.List<Object[]> totalsByInventoryIds(java.util.Collection<Long> inventoryIds,
            com.myenterpriseos.myenterpriseopensource.enums.ReservationStatus status);
    @org.springframework.data.jpa.repository.Query("select r from StockReservation r where r.orderItem.order.id = :orderId")
    java.util.List<StockReservation> findByOrderId(Long orderId);
}
