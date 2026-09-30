package com.myenterpriseos.myenterpriseopensource.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import com.myenterpriseos.myenterpriseopensource.enums.*;

@Entity
@Table(name = "inventory_movement")
public class InventoryMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50)
    private MovementType movementType;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reason")
    private String reason;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }

    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory value) { this.inventory = value; }

    public MovementType getMovementType() { return movementType; }
    public void setMovementType(MovementType value) { this.movementType = value; }

    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer value) { this.quantityChange = value; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }

    public String getReason() { return reason; }
    public void setReason(String value) { this.reason = value; }
}
