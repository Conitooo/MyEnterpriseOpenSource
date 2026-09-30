package com.myenterpriseos.myenterpriseopensource.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import com.myenterpriseos.myenterpriseopensource.enums.*;

@Entity
@Table(name = "stock_reservation")
public class StockReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }

    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem value) { this.orderItem = value; }

    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory value) { this.inventory = value; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer value) { this.quantity = value; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus value) { this.status = value; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }

    public Instant getReleasedAt() { return releasedAt; }
    public void setReleasedAt(Instant value) { this.releasedAt = value; }
}
