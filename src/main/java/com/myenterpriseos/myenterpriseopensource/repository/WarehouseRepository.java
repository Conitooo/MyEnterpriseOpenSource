package com.myenterpriseos.myenterpriseopensource.repository;

import com.myenterpriseos.myenterpriseopensource.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    boolean existsByCompanyIdAndCode(Long companyId, String code);
    java.util.List<Warehouse> findByCompanyId(Long companyId);
}
