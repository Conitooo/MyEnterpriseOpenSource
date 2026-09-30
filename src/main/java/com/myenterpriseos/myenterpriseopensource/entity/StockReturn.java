package com.myenterpriseos.myenterpriseopensource.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "stock_return")
public class StockReturn {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_item_id") private ShipmentItem shipmentItem;
    @Column(nullable = false) private int quantity;
    @Column(nullable = false) private String reason;
    @Column(name = "created_at", insertable = false, updatable = false) private Instant createdAt;
    public Long getId() { return id; }
    public ShipmentItem getShipmentItem() { return shipmentItem; }
    public void setShipmentItem(ShipmentItem value) { shipmentItem = value; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int value) { quantity = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public Instant getCreatedAt() { return createdAt; }
}
