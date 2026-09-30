package com.myenterpriseos.myenterpriseopensource.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import com.myenterpriseos.myenterpriseopensource.enums.*;

@Entity
@Table(name = "shipment_item")
public class ShipmentItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }

    public Shipment getShipment() { return shipment; }
    public void setShipment(Shipment value) { this.shipment = value; }

    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem value) { this.orderItem = value; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer value) { this.quantity = value; }
}
