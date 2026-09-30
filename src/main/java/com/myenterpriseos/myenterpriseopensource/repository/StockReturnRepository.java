package com.myenterpriseos.myenterpriseopensource.repository;

import com.myenterpriseos.myenterpriseopensource.entity.StockReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockReturnRepository extends JpaRepository<StockReturn, Long> {
    @Query("select coalesce(sum(r.quantity), 0) from StockReturn r where r.shipmentItem.id = :itemId")
    long returnedQuantity(Long itemId);
}
