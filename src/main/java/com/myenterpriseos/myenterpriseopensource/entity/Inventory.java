package com.myenterpriseos.myenterpriseopensource.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import com.myenterpriseos.myenterpriseopensource.enums.*;

@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }

    public Product getProduct() { return product; }
    public void setProduct(Product value) { this.product = value; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse value) { this.warehouse = value; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer value) { this.quantity = value; }
}
