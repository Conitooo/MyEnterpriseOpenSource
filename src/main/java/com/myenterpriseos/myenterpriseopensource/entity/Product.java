package com.myenterpriseos.myenterpriseopensource.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import com.myenterpriseos.myenterpriseopensource.enums.*;

@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }

    public Company getCompany() { return company; }
    public void setCompany(Company value) { this.company = value; }

    public String getProductName() { return productName; }
    public void setProductName(String value) { this.productName = value; }

    public String getSku() { return sku; }
    public void setSku(String value) { this.sku = value; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal value) { this.price = value; }

    public String getCurrency() { return currency; }
    public void setCurrency(String value) { this.currency = value; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
}
