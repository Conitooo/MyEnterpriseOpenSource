package com.myenterpriseos.myenterpriseopensource.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import com.myenterpriseos.myenterpriseopensource.enums.*;

@Entity
@Table(name = "warehouse")
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }

    public Company getCompany() { return company; }
    public void setCompany(Company value) { this.company = value; }

    public String getCode() { return code; }
    public void setCode(String value) { this.code = value; }

    public String getName() { return name; }
    public void setName(String value) { this.name = value; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { active = value; }
}
