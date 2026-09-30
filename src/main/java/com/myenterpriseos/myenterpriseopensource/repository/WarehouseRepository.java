package com.myenterpriseos.myenterpriseopensource.repository;

import com.myenterpriseos.myenterpriseopensource.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select w from Warehouse w where w.id = :id")
    java.util.Optional<Warehouse> lockById(Long id);
    boolean existsByCompanyIdAndCode(Long companyId, String code);
    java.util.List<Warehouse> findByCompanyId(Long companyId);
}
