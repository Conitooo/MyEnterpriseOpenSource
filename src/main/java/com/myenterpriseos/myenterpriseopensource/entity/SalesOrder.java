package com.myenterpriseos.myenterpriseopensource.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import com.myenterpriseos.myenterpriseopensource.enums.*;

@Entity
@Table(name = "sales_order")
public class SalesOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
    private String recipient;
    @Column(name = "delivery_street") private String deliveryStreet;
    @Column(name = "delivery_city") private String deliveryCity;
    @Column(name = "delivery_postal_code") private String deliveryPostalCode;
    @Column(name = "delivery_country") private String deliveryCountry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }

    public Company getCompany() { return company; }
    public void setCompany(Company value) { this.company = value; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer value) { customer = value; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String value) { recipient = value; }
    public String getDeliveryStreet() { return deliveryStreet; }
    public void setDeliveryStreet(String value) { deliveryStreet = value; }
    public String getDeliveryCity() { return deliveryCity; }
    public void setDeliveryCity(String value) { deliveryCity = value; }
    public String getDeliveryPostalCode() { return deliveryPostalCode; }
    public void setDeliveryPostalCode(String value) { deliveryPostalCode = value; }
    public String getDeliveryCountry() { return deliveryCountry; }
    public void setDeliveryCountry(String value) { deliveryCountry = value; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus value) { this.status = value; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }

    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant value) { this.confirmedAt = value; }

    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant value) { this.cancelledAt = value; }
}
