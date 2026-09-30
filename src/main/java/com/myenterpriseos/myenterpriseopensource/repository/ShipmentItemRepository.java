package com.myenterpriseos.myenterpriseopensource.repository;

import com.myenterpriseos.myenterpriseopensource.entity.ShipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, Long> {
    java.util.List<ShipmentItem> findByShipmentId(Long shipmentId);
    java.util.List<ShipmentItem> findByOrderItemId(Long orderItemId);
    @org.springframework.data.jpa.repository.Query("select i.orderItem.id, sum(i.quantity) from ShipmentItem i where i.orderItem.order.id = :orderId group by i.orderItem.id")
    java.util.List<Object[]> shippedTotalsByOrderId(Long orderId);
}
