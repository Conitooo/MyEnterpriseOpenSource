package com.myenterpriseos.myenterpriseopensource.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "stock_transfer")
public class StockTransfer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Company company;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Product product;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_warehouse_id") private Warehouse sourceWarehouse;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_warehouse_id") private Warehouse destinationWarehouse;
    @Column(nullable = false) private int quantity;
    @Column(name = "created_at", insertable = false, updatable = false) private Instant createdAt;
    public Long getId() { return id; }
    public Company getCompany() { return company; }
    public void setCompany(Company value) { company = value; }
    public Product getProduct() { return product; }
    public void setProduct(Product value) { product = value; }
    public Warehouse getSourceWarehouse() { return sourceWarehouse; }
    public void setSourceWarehouse(Warehouse value) { sourceWarehouse = value; }
    public Warehouse getDestinationWarehouse() { return destinationWarehouse; }
    public void setDestinationWarehouse(Warehouse value) { destinationWarehouse = value; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int value) { quantity = value; }
    public Instant getCreatedAt() { return createdAt; }
}
