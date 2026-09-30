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

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus value) { this.status = value; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }

    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant value) { this.confirmedAt = value; }

    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant value) { this.cancelledAt = value; }
}
