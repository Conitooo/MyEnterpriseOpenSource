package com.myenterpriseos.myenterpriseopensource;

import com.myenterpriseos.myenterpriseopensource.api.ApiException;
import com.myenterpriseos.myenterpriseopensource.dto.ApiDtos.*;
import com.myenterpriseos.myenterpriseopensource.service.BusinessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BusinessServiceTests {
    @Autowired BusinessService service;

    @Test
    void reservesStockAndShipsInTwoParts() {
        Long company = service.createCompany(new CompanyRequest("Acme")).id();
        Long product = service.createProduct(company,
                new ProductRequest("Widget", "W-1", new BigDecimal("12.50"), "EUR")).id();
        Long warehouse = service.createWarehouse(company, new WarehouseRequest("MAD", "Madrid")).id();
        InventoryResponse stock = service.addStock(warehouse, new StockRequest(product, 10));
        OrderResponse draft = service.createOrder(company, new OrderRequest(List.of(new OrderLineRequest(product, 8))));
        Long item = draft.items().getFirst().id();

        OrderResponse confirmed = service.confirm(draft.id(),
                new ConfirmRequest(List.of(new AllocationRequest(item, stock.id(), 8))));
        assertEquals("CONFIRMED", confirmed.status());
        assertEquals(2, service.inventory(warehouse).getFirst().available());
        assertThrows(ApiException.class, () -> service.adjust(stock.id(), new AdjustmentRequest(-3, "damage")));
        assertThrows(ApiException.class, () -> service.createOrder(company,
                new OrderRequest(List.of(new OrderLineRequest(product, 8), new OrderLineRequest(product, 1)))));

        service.ship(draft.id(), new ShipmentRequest(warehouse, List.of(new ShipmentLineRequest(item, 3))));
        assertEquals("CONFIRMED", service.order(draft.id()).status());
        assertEquals(7, service.inventory(warehouse).getFirst().quantity());
        assertThrows(ApiException.class, () -> service.cancel(draft.id()));

        service.ship(draft.id(), new ShipmentRequest(warehouse, List.of(new ShipmentLineRequest(item, 5))));
        assertEquals("SHIPPED", service.order(draft.id()).status());
        assertEquals(2, service.inventory(warehouse).getFirst().quantity());
        assertEquals(2, service.inventory(warehouse).getFirst().available());
        assertEquals(3, service.movements(stock.id()).size());
    }

    @Test
    void rejectsCrossCompanyStockAndOverAllocation() {
        Long first = service.createCompany(new CompanyRequest("First")).id();
        Long second = service.createCompany(new CompanyRequest("Second")).id();
        Long product = service.createProduct(first,
                new ProductRequest("Widget", "W-1", BigDecimal.ONE, "EUR")).id();
        Long wrongWarehouse = service.createWarehouse(second, new WarehouseRequest("W", "Other")).id();
        assertThrows(ApiException.class, () -> service.addStock(wrongWarehouse, new StockRequest(product, 1)));

        Long warehouse = service.createWarehouse(first, new WarehouseRequest("W", "Main")).id();
        Long stock = service.addStock(warehouse, new StockRequest(product, 2)).id();
        OrderResponse order = service.createOrder(first, new OrderRequest(List.of(new OrderLineRequest(product, 3))));
        assertThrows(ApiException.class, () -> service.confirm(order.id(),
                new ConfirmRequest(List.of(new AllocationRequest(order.items().getFirst().id(), stock, 3)))));
        assertEquals("DRAFT", service.order(order.id()).status());
    }
}
