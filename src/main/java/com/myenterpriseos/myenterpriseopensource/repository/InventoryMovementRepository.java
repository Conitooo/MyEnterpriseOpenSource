package com.myenterpriseos.myenterpriseopensource.repository;

import com.myenterpriseos.myenterpriseopensource.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    java.util.List<InventoryMovement> findByInventoryIdOrderByIdDesc(Long inventoryId);
}
