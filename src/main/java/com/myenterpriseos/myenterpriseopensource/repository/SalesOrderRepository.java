package com.myenterpriseos.myenterpriseopensource.repository;

import com.myenterpriseos.myenterpriseopensource.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    java.util.List<SalesOrder> findByCompanyId(Long companyId);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "customer")
    org.springframework.data.domain.Page<SalesOrder> findByCompanyId(Long companyId,
            org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "customer")
    @org.springframework.data.jpa.repository.Query("select o from SalesOrder o left join o.customer c " +
            "where o.company.id = :companyId and (:status is null or o.status = :status) " +
            "and (:customerQuery = '' or lower(c.name) like lower(concat('%', :customerQuery, '%')))")
    org.springframework.data.domain.Page<SalesOrder> search(Long companyId,
            com.myenterpriseos.myenterpriseopensource.enums.OrderStatus status, String customerQuery,
            org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from SalesOrder o where o.id = :id")
    java.util.Optional<SalesOrder> lockById(Long id);
}
