package com.myenterpriseos.myenterpriseopensource.repository;

import com.myenterpriseos.myenterpriseopensource.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByCompanyIdAndSku(Long companyId, String sku);
    java.util.List<Product> findByCompanyId(Long companyId);
    org.springframework.data.domain.Page<Product> findByCompanyIdAndProductNameContainingIgnoreCase(
            Long companyId, String name, org.springframework.data.domain.Pageable pageable);
}
