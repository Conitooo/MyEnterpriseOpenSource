package com.myenterpriseos.myenterpriseopensource.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import com.myenterpriseos.myenterpriseopensource.enums.*;

@Entity
@Table(name = "order_item")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private SalesOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }

    public SalesOrder getOrder() { return order; }
    public void setOrder(SalesOrder value) { this.order = value; }

    public Product getProduct() { return product; }
    public void setProduct(Product value) { this.product = value; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer value) { this.quantity = value; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal value) { this.price = value; }

    public String getCurrency() { return currency; }
    public void setCurrency(String value) { this.currency = value; }
}
