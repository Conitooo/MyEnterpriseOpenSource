package com.myenterpriseos.myenterpriseopensource.repository;

import com.myenterpriseos.myenterpriseopensource.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    java.util.List<Inventory> findByWarehouseId(Long warehouseId);
    java.util.Optional<Inventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from Inventory i where i.id = :id")
    java.util.Optional<Inventory> lockById(Long id);
}
