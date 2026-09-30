package com.myenterpriseos.myenterpriseopensource.repository;

import com.myenterpriseos.myenterpriseopensource.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    java.util.List<SalesOrder> findByCompanyId(Long companyId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from SalesOrder o where o.id = :id")
    java.util.Optional<SalesOrder> lockById(Long id);
}
