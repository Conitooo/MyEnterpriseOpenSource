package com.myenterpriseos.myenterpriseopensource.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import com.myenterpriseos.myenterpriseopensource.enums.*;

@Entity
@Table(name = "shipment")
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private SalesOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ShipmentStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;
    private String carrier;
    @Column(name = "tracking_number") private String trackingNumber;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }

    public SalesOrder getOrder() { return order; }
    public void setOrder(SalesOrder value) { this.order = value; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse value) { this.warehouse = value; }

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus value) { this.status = value; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }

    public Instant getShippedAt() { return shippedAt; }
    public void setShippedAt(Instant value) { this.shippedAt = value; }
    public String getCarrier() { return carrier; }
    public void setCarrier(String value) { carrier = value; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String value) { trackingNumber = value; }
}
