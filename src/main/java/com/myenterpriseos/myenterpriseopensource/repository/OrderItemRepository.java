package com.myenterpriseos.myenterpriseopensource.repository;

import com.myenterpriseos.myenterpriseopensource.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "product")
    java.util.List<OrderItem> findByOrderIdIn(java.util.Collection<Long> orderIds);
    java.util.List<OrderItem> findByOrderId(Long orderId);
    @org.springframework.data.jpa.repository.Query("select i from OrderItem i where i.order.company.id = :companyId order by i.order.id, i.id")
    java.util.List<OrderItem> findByCompanyId(Long companyId);
}
