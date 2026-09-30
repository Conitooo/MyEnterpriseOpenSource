package com.myenterpriseos.myenterpriseopensource.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "customer")
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    @Column(nullable = false)
    private String name;
    private String email;
    private String phone;
    @Column(nullable = false)
    private boolean active = true;
    public Long getId() { return id; }
    public Company getCompany() { return company; }
    public void setCompany(Company value) { company = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { active = value; }
}
