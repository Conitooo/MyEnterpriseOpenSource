package com.myenterpriseos.myenterpriseopensource.repository;

import com.myenterpriseos.myenterpriseopensource.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Page<Customer> findByCompanyIdAndNameContainingIgnoreCase(Long companyId, String name, Pageable pageable);
}
