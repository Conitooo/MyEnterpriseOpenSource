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
        Long customer = service.createCustomer(company, new CustomerRequest("Buyer", null, null)).id();
        DeliveryAddress address = new DeliveryAddress("Buyer", "Street 1", "Madrid", "28001", "Spain");
        InventoryResponse stock = service.addStock(warehouse, new StockRequest(product, 10));
        OrderResponse draft = service.createOrder(company, new OrderRequest(customer, address, List.of(new OrderLineRequest(product, 8))));
        Long item = draft.items().getFirst().id();

        OrderResponse confirmed = service.confirm(draft.id(),
                new ConfirmRequest(List.of(new AllocationRequest(item, stock.id(), 8))));
        assertEquals("CONFIRMED", confirmed.status());
        assertEquals(2, service.inventory(warehouse).getFirst().available());
        assertThrows(ApiException.class, () -> service.adjust(stock.id(), new AdjustmentRequest(-3, "damage")));
        assertThrows(ApiException.class, () -> service.createOrder(company,
                new OrderRequest(customer, address, List.of(new OrderLineRequest(product, 8), new OrderLineRequest(product, 1)))));

        service.ship(draft.id(), new ShipmentRequest(warehouse, null, null, List.of(new ShipmentLineRequest(item, 3))));
        assertEquals("CONFIRMED", service.order(draft.id()).status());
        assertEquals(7, service.inventory(warehouse).getFirst().quantity());
        assertThrows(ApiException.class, () -> service.cancel(draft.id()));

        service.ship(draft.id(), new ShipmentRequest(warehouse, null, null, List.of(new ShipmentLineRequest(item, 5))));
        assertEquals("SHIPPED", service.order(draft.id()).status());
        assertEquals(2, service.inventory(warehouse).getFirst().quantity());
        assertEquals(2, service.inventory(warehouse).getFirst().available());
        assertEquals(3, service.movements(stock.id()).size());
    }

    @Test
    void rejectsCrossCompanyStockAndOverAllocation() {
        Long first = service.createCompany(new CompanyRequest("First")).id();
        Long customer = service.createCustomer(first, new CustomerRequest("Buyer", null, null)).id();
        DeliveryAddress address = new DeliveryAddress("Buyer", "Street 1", "Madrid", "28001", "Spain");
        Long second = service.createCompany(new CompanyRequest("Second")).id();
        Long product = service.createProduct(first,
                new ProductRequest("Widget", "W-1", BigDecimal.ONE, "EUR")).id();
        Long wrongWarehouse = service.createWarehouse(second, new WarehouseRequest("W", "Other")).id();
        assertThrows(ApiException.class, () -> service.addStock(wrongWarehouse, new StockRequest(product, 1)));

        Long warehouse = service.createWarehouse(first, new WarehouseRequest("W", "Main")).id();
        Long stock = service.addStock(warehouse, new StockRequest(product, 2)).id();
        OrderResponse order = service.createOrder(first, new OrderRequest(customer, address, List.of(new OrderLineRequest(product, 3))));
        assertThrows(ApiException.class, () -> service.confirm(order.id(),
                new ConfirmRequest(List.of(new AllocationRequest(order.items().getFirst().id(), stock, 3)))));
        assertEquals("DRAFT", service.order(order.id()).status());
    }

    @Test
    void customerDeliveryAndCatalogEditsAreTenantScoped() {
        Long first = service.createCompany(new CompanyRequest("First")).id();
        Long second = service.createCompany(new CompanyRequest("Second")).id();
        Long customer = service.createCustomer(first, new CustomerRequest("Buyer", "buyer@example.com", "123")).id();
        Long product = service.createProduct(first,
                new ProductRequest("Widget", "W-1", BigDecimal.ONE, "EUR")).id();
        Long warehouse = service.createWarehouse(first, new WarehouseRequest("W", "Main")).id();
        assertThrows(ApiException.class, () -> service.updateCustomer(second, customer,
                new CustomerRequest("Other", null, null)));
        assertThrows(ApiException.class, () -> service.updateProduct(second, product,
                new ProductRequest("Other", "X", BigDecimal.ONE, "EUR")));
        assertEquals("Widget 2", service.updateProduct(first, product,
                new ProductRequest("Widget 2", "W-2", BigDecimal.TEN, "EUR")).productName());
        assertEquals("West", service.updateWarehouse(first, warehouse,
                new WarehouseRequest("WEST", "West")).name());
        DeliveryAddress address = new DeliveryAddress("Buyer", "Street 1", "Madrid", "28001", "Spain");
        OrderResponse order = service.createOrder(first, new OrderRequest(customer, address,
                List.of(new OrderLineRequest(product, 1))));
        assertEquals("Buyer", order.customerName());
        assertEquals("Street 1", service.order(order.id()).deliveryAddress().street());
        service.deactivateCustomer(first, customer);
        assertThrows(ApiException.class, () -> service.createOrder(first,
                new OrderRequest(customer, address, List.of(new OrderLineRequest(product, 1)))));
        service.deactivateProduct(first, product);
        assertThrows(ApiException.class, () -> service.addStock(warehouse, new StockRequest(product, 1)));
    }

    @Test
    void transfersRespectReservationsAndStocktakesUseAbsoluteCounts() {
        Long company = service.createCompany(new CompanyRequest("Transfer Co")).id();
        Long product = service.createProduct(company,
                new ProductRequest("Widget", "W-1", BigDecimal.ONE, "EUR")).id();
        Long source = service.createWarehouse(company, new WarehouseRequest("A", "A")).id();
        Long destination = service.createWarehouse(company, new WarehouseRequest("B", "B")).id();
        InventoryResponse stock = service.addStock(source, new StockRequest(product, 10));
        TransferResponse transfer = service.transfer(company, new TransferRequest(source, destination, product, 4));
        assertEquals(6, transfer.source().quantity());
        assertEquals(4, transfer.destination().quantity());
        assertEquals(2, service.movements(stock.id()).size());
        assertEquals(5, service.stocktake(stock.id(), new StocktakeRequest(5, "Physical count")).quantity());
        assertThrows(ApiException.class, () -> service.transfer(company,
                new TransferRequest(source, destination, product, 6)));
        assertThrows(ApiException.class, () -> service.deactivateWarehouse(company, source));
    }

    @Test
    void returnedShipmentCannotExceedOriginalQuantity() {
        Long company = service.createCompany(new CompanyRequest("Returns Co")).id();
        Long customer = service.createCustomer(company, new CustomerRequest("Buyer", null, null)).id();
        Long product = service.createProduct(company,
                new ProductRequest("Widget", "W-1", BigDecimal.ONE, "EUR")).id();
        Long warehouse = service.createWarehouse(company, new WarehouseRequest("W", "Main")).id();
        Long stock = service.addStock(warehouse, new StockRequest(product, 5)).id();
        DeliveryAddress address = new DeliveryAddress("Buyer", "Street 1", "Madrid", "28001", "Spain");
        OrderResponse draft = service.createOrder(company,
                new OrderRequest(customer, address, List.of(new OrderLineRequest(product, 3))));
        Long line = draft.items().getFirst().id();
        service.confirm(draft.id(), new ConfirmRequest(List.of(new AllocationRequest(line, stock, 3))));
        ShipmentResponse shipment = service.ship(draft.id(), new ShipmentRequest(warehouse,
                "Carrier", "TRACK-1", List.of(new ShipmentLineRequest(line, 3))));
        Long shipmentItem = shipment.items().getFirst().id();
        assertEquals("TRACK-1", shipment.trackingNumber());
        service.returnStock(draft.id(), new ReturnRequest(shipmentItem, 2, "Damaged package"));
        assertEquals(4, service.inventory(warehouse).getFirst().quantity());
        assertEquals(2, service.shipments(draft.id()).getFirst().items().getFirst().returnedQuantity());
        assertThrows(ApiException.class, () -> service.returnStock(draft.id(),
                new ReturnRequest(shipmentItem, 2, "Duplicate")));
    }

    @Test
    void productSearchIsPagedWithinCompany() {
        Long first = service.createCompany(new CompanyRequest("First")).id();
        Long second = service.createCompany(new CompanyRequest("Second")).id();
        for (int i = 0; i < 4; i++) service.createProduct(first,
                new ProductRequest("Widget " + i, "W-" + i, BigDecimal.ONE, "EUR"));
        service.createProduct(second, new ProductRequest("Widget other", "W-0", BigDecimal.ONE, "EUR"));
        PageResponse<ProductResponse> page = service.productPage(first, "Widget", 1, 2);
        assertEquals(4, page.total());
        assertEquals(2, page.items().size());
        assertTrue(page.items().stream().allMatch(p -> p.companyId().equals(first)));
        assertThrows(ApiException.class, () -> service.productPage(first, "", 0, 101));
        assertThrows(ApiException.class, () -> service.orderPage(first, 0, 20, "BAD", ""));
    }
}
